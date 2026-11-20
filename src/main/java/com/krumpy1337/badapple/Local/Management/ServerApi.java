package com.krumpy1337.badapple.Local.Management;

import com.krumpy1337.badapple.BadApple;
import com.krumpy1337.badapple.Local.Permissions.GroupConfig;
import com.krumpy1337.badapple.Local.Permissions.GroupManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/** Authenticated, plugin-scoped HTTP API. Place it behind a TLS reverse proxy for remote access. */
public final class ServerApi {
    private static final int MAX_BODY_BYTES = 16 * 1024;
    private final BadApple plugin;
    private final GroupManager groups = new GroupManager();
    private final File keyFile;
    private final SecureRandom random = new SecureRandom();
    private HttpServer server;
    private ExecutorService executor;
    private volatile byte[] keyHash;

    public ServerApi(BadApple plugin) {
        this.plugin = plugin;
        this.keyFile = new File(plugin.getDataFolder(), "api-key.yml");
        loadKey();
    }

    public boolean isEnabled() {
        return server != null;
    }

    public String generateKey() {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String apiKey = "ba_" + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        byte[] replacementHash = hash(apiKey);
        saveKey(replacementHash);
        keyHash = replacementHash;
        return apiKey;
    }

    public void installProvisionedKey(String key) {
        if (keyHash != null || key == null || !key.matches("ba_[A-Za-z0-9_-]{43}")) return;
        byte[] provisionedHash = hash(key);
        saveKey(provisionedHash);
        keyHash = provisionedHash;
    }

    public boolean revokeKey() {
        if (keyHash == null) return false;
        try {
            Files.deleteIfExists(keyFile.toPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove the stored API key.", exception);
        }
        keyHash = null;
        return true;
    }

    public boolean hasKey() {
        return keyHash != null;
    }

    public String getAddress() {
        return plugin.getConfig().getString("api.bind-address", "127.0.0.1") + ":"
                + plugin.getConfig().getInt("api.port", 8787);
    }

    public String getPublicUrl() {
        String publicUrl = plugin.getConfig().getString("api.public-url", "");
        if (publicUrl == null) return "";
        return publicUrl.trim().replaceAll("/+$", "");
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("api.enabled", true) || server != null) return;
        String bindAddress = plugin.getConfig().getString("api.bind-address", "127.0.0.1");
        int port = plugin.getConfig().getInt("api.port", 8787);
        if (port < 1 || port > 65535) throw new IllegalArgumentException("api.port must be between 1 and 65535.");

        try {
            server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            server.createContext("/v1/", this::handleRequest);
            executor = Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "BadApple-API");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.start();
            plugin.getLogger().info("Server API listening on " + getAddress());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not start the BadApple API on " + getAddress()
                            + ". Check that the address and port are available; the plugin will continue without the API.",
                    exception);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            if (!authenticate(exchange)) {
                sendJson(exchange, 401, mapOf("error", "unauthorized"));
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("GET".equals(method) && "/v1/server".equals(path)) {
                sendJson(exchange, 200, runSync(this::serverInfo));
            } else if ("GET".equals(method) && "/v1/players".equals(path)) {
                sendJson(exchange, 200, runSync(this::playerInfo));
            } else if ("GET".equals(method) && "/v1/groups".equals(path)) {
                sendJson(exchange, 200, runSync(this::groupInfo));
            } else if ("GET".equals(method) && "/v1/tracks".equals(path)) {
                sendJson(exchange, 200, runSync(this::trackInfo));
            } else if ("POST".equals(method)) {
                Map<String, Object> input = parseJsonMap(readBody(exchange));
                Object result = runSync(() -> mutate(path, input));
                sendJson(exchange, 200, mapOf("success", true, "result", result));
            } else {
                sendJson(exchange, 404, mapOf("error", "not_found"));
            }
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, mapOf("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "BadApple API request failed.", exception);
            if (exchange.getResponseCode() == -1) sendJson(exchange, 500, mapOf("error", "request_failed"));
        } finally {
            exchange.close();
        }
    }

    private boolean authenticate(HttpExchange exchange) {
        if (keyHash == null) return false;
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return false;
        byte[] providedHash = hash(header.substring(7).trim());
        return MessageDigest.isEqual(keyHash, providedHash);
    }

    private Map<String, Object> serverInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", plugin.getServer().getName());
        info.put("version", Bukkit.getVersion());
        info.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        info.put("maxPlayers", Bukkit.getMaxPlayers());
        info.put("uptimeMillis", System.currentTimeMillis() - plugin.getServerStartedAt());
        info.put("memory", mapOf(
                "usedBytes", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                "maxBytes", Runtime.getRuntime().maxMemory()));
        return info;
    }

    private List<Map<String, Object>> playerInfo() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            result.add(mapOf(
                    "name", player.getName(),
                    "uuid", player.getUniqueId().toString(),
                    "world", player.getWorld().getName(),
                    "group", groups.getGroup(player)));
        }
        return result;
    }

    private List<Map<String, Object>> groupInfo() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : groups.getGroups()) {
            result.add(mapOf(
                    "name", name,
                    "prefix", GroupConfig.getGroupPrefix(name),
                    "parent", groups.getParentGroup(name),
                    "weight", groups.getGroupWeight(name),
                    "permissions", GroupConfig.getPermissions(name),
                    "track", groups.getTrackForGroup(name)));
        }
        return result;
    }

    private List<Map<String, Object>> trackInfo() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : groups.getTracks()) {
            result.add(mapOf("name", name, "groups", groups.getTrackGroups(name)));
        }
        return result;
    }

    private Object mutate(String path, Map<String, Object> input) {
        return switch (path) {
            case "/v1/groups/create" -> {
                String name = required(input, "name");
                groups.createGroup(name);
                audit("created group " + name);
                yield mapOf("group", name);
            }
            case "/v1/groups/delete" -> {
                String name = required(input, "name");
                groups.deleteGroup(name);
                audit("deleted group " + name);
                yield mapOf("group", name);
            }
            case "/v1/groups/prefix" -> {
                String name = required(input, "name");
                String prefix = required(input, "prefix");
                groups.setGroupPrefix(name, prefix);
                audit("updated prefix for group " + name);
                yield mapOf("group", name);
            }
            case "/v1/players/group" -> {
                String playerName = required(input, "player");
                String groupName = required(input, "group");
                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null) throw new IllegalArgumentException("player_not_online");
                if (!groups.groupExists(groupName)) throw new IllegalArgumentException("group_not_found");
                groups.setGroup(player, groupName);
                audit("assigned group " + groupName + " to " + playerName);
                yield mapOf("player", playerName, "group", groupName);
            }
            case "/v1/players/promote", "/v1/players/demote" -> {
                String playerName = required(input, "player");
                String track = required(input, "track");
                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null) throw new IllegalArgumentException("player_not_online");
                if ("/v1/players/promote".equals(path)) groups.promotePlayer(player, track);
                else groups.demotePlayer(player, track);
                audit((path.endsWith("promote") ? "promoted " : "demoted ") + playerName + " on " + track);
                yield mapOf("player", playerName, "track", track);
            }
            default -> throw new IllegalArgumentException("not_found");
        };
    }

    private String required(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (!(value instanceof String text) || text.isBlank() || text.length() > 128) {
            throw new IllegalArgumentException("invalid_" + key);
        }
        return text.trim();
    }

    private void audit(String action) {
        plugin.getLogger().info("API key performed action: " + action);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (var input = exchange.getRequestBody(); var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BODY_BYTES) throw new IllegalArgumentException("request_too_large");
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] response = toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.getResponseBody().close();
    }

    private <T> T runSync(Callable<T> callable) throws Exception {
        Future<T> future = Bukkit.getScheduler().callSyncMethod(plugin, callable);
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            future.cancel(false);
            throw new IllegalStateException("server_busy");
        }
    }

    private void loadKey() {
        if (keyFile.isFile()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(keyFile);
            String saved = yaml.getString("sha256");
            if (saved != null) {
                try {
                    keyHash = java.util.HexFormat.of().parseHex(saved);
                    if (keyHash.length != 32) {
                        plugin.getLogger().warning("Stored API key hash has an invalid length; ignoring it.");
                        keyHash = null;
                    }
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Stored API key hash is invalid; ignoring it.");
                }
            }
        }
        String provisioned = plugin.getConfig().getString("api.provisioning-key", "");
        if (provisioned != null && !provisioned.isBlank()) {
            if (keyHash == null) {
                if (!provisioned.trim().matches("ba_[A-Za-z0-9_-]{43}")) {
                    throw new IllegalStateException("api.provisioning-key must be a generated BadApple API key.");
                }
                installProvisionedKey(provisioned.trim());
            }
            plugin.getConfig().set("api.provisioning-key", "");
            plugin.saveConfig();
        }
    }

    private void saveKey(byte[] hash) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("sha256", java.util.HexFormat.of().formatHex(hash));
        File temporaryFile = new File(keyFile.getParentFile(), keyFile.getName() + ".tmp");
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IOException("Could not create plugin data folder.");
            }
            yaml.save(temporaryFile);
            try {
                Files.move(temporaryFile.toPath(), keyFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile.toPath(), keyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporaryFile.toPath());
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new IllegalStateException("Could not save API key hash.", exception);
        }
    }

    private byte[] hash(String key) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) throw new IllegalArgumentException("invalid_json");
        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) return result;
        for (String token : splitTopLevel(content, ',')) {
            String[] pair = token.split(":", 2);
            if (pair.length != 2) throw new IllegalArgumentException("invalid_json");
            result.put(unquote(pair[0].trim()), parseValue(pair[1].trim()));
        }
        return result;
    }

    private Object parseValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) return unquote(value);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        if ("null".equalsIgnoreCase(value)) return null;
        return value;
    }

    private List<String> splitTopLevel(String input, char delimiter) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (escaped) {
                current.append(character);
                escaped = false;
            } else if (character == '\\') {
                current.append(character);
                escaped = true;
            } else if (character == '"') {
                current.append(character);
                quoted = !quoted;
            } else if (character == delimiter && !quoted) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        tokens.add(current.toString());
        return tokens;
    }

    private String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '"' || trimmed.charAt(trimmed.length() - 1) != '"') {
            throw new IllegalArgumentException("invalid_json_string");
        }
        return trimmed.substring(1, trimmed.length() - 1)
                .replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
    }

    private String toJson(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        if (value instanceof String text) {
            return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r") + "\"";
        }
        if (value instanceof Map<?, ?> map) {
            List<String> fields = new ArrayList<>();
            map.forEach((key, item) -> fields.add(toJson(String.valueOf(key)) + ":" + toJson(item)));
            return "{" + String.join(",", fields) + "}";
        }
        if (value instanceof Iterable<?> values) {
            List<String> items = new ArrayList<>();
            values.forEach(item -> items.add(toJson(item)));
            return "[" + String.join(",", items) + "]";
        }
        return toJson(String.valueOf(value));
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return result;
    }
}
