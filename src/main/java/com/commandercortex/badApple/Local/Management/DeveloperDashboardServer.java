package com.commandercortex.badApple.Local.Management;

import com.commandercortex.badApple.BadApple;
import com.commandercortex.badApple.Local.Permissions.GroupConfig;
import com.commandercortex.badApple.Local.Permissions.GroupManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Local HTTP server providing a developer dashboard for inspecting and managing server state.
 */
public class DeveloperDashboardServer {
    private static final int PORT = 8081;
    private HttpServer server;
    private final GroupManager groupManager = new GroupManager();

    public void start() {
        if (server != null) {
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            server.createContext("/", this::handleRoot);
            server.createContext("/api", this::handleApi);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start local developer dashboard.", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    public String getUrl() {
        return "http://127.0.0.1:" + PORT + "/";
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }

        sendText(exchange, 200, buildDashboardHtml(), "text/html; charset=utf-8");
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equalsIgnoreCase(method) && "/api/server".equals(path)) {
                sendJson(exchange, getServerInfo());
                return;
            }
            if ("GET".equalsIgnoreCase(method) && "/api/players".equals(path)) {
                sendJson(exchange, getPlayersData());
                return;
            }
            if ("GET".equalsIgnoreCase(method) && "/api/groups".equals(path)) {
                sendJson(exchange, getGroupsData());
                return;
            }
            if ("GET".equalsIgnoreCase(method) && "/api/tracks".equals(path)) {
                sendJson(exchange, getTracksData());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/console".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String command = String.valueOf(payload.getOrDefault("command", "")).trim();
                if (command.isEmpty()) {
                    sendJson(exchange, mapOf("success", false, "message", "Command cannot be empty."));
                    return;
                }
                runSync(() -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Command executed."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/groups/create".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String name = String.valueOf(payload.getOrDefault("name", "")).trim();
                if (name.isEmpty()) {
                    sendJson(exchange, mapOf("success", false, "message", "Group name required."));
                    return;
                }
                runSync(() -> {
                    if (!groupManager.groupExists(name)) {
                        groupManager.createGroup(name);
                    }
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Group created."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/groups/delete".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String name = String.valueOf(payload.getOrDefault("name", "")).trim();
                if (name.isEmpty()) {
                    sendJson(exchange, mapOf("success", false, "message", "Group name required."));
                    return;
                }
                runSync(() -> {
                    groupManager.deleteGroup(name);
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Group deleted."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/groups/prefix".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String name = String.valueOf(payload.getOrDefault("name", "")).trim();
                String prefix = String.valueOf(payload.getOrDefault("prefix", "")).trim();
                runSync(() -> {
                    groupManager.setGroupPrefix(name, prefix);
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Prefix updated."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/groups/permission".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String name = String.valueOf(payload.getOrDefault("name", "")).trim();
                String permission = String.valueOf(payload.getOrDefault("permission", "")).trim();
                boolean enabled = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("enabled", true)));
                runSync(() -> {
                    if (enabled) {
                        groupManager.addPermission(name, permission);
                    } else {
                        groupManager.removePermission(name, permission);
                    }
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", enabled ? "Permission added." : "Permission removed."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/tracks/create".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String name = String.valueOf(payload.getOrDefault("name", "")).trim();
                List<?> groups = (List<?>) payload.getOrDefault("groups", new ArrayList<>());
                List<String> ordered = new ArrayList<>();
                for (Object group : groups) {
                    String value = String.valueOf(group).trim();
                    if (!value.isEmpty()) {
                        ordered.add(value);
                    }
                }
                runSync(() -> {
                    groupManager.createTrack(name, ordered);
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Track created."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/tracks/addgroup".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String track = String.valueOf(payload.getOrDefault("track", "")).trim();
                String group = String.valueOf(payload.getOrDefault("group", "")).trim();
                runSync(() -> {
                    groupManager.addGroupToTrack(track, group);
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Added group to track."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/tracks/removegroup".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String track = String.valueOf(payload.getOrDefault("track", "")).trim();
                String group = String.valueOf(payload.getOrDefault("group", "")).trim();
                runSync(() -> {
                    groupManager.removeGroupFromTrack(track, group);
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Removed group from track."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/player/promote".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String playerName = String.valueOf(payload.getOrDefault("player", "")).trim();
                String track = String.valueOf(payload.getOrDefault("track", "")).trim();
                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null) {
                    sendJson(exchange, mapOf("success", false, "message", "Player not online."));
                    return;
                }
                runSync(() -> {
                    groupManager.promotePlayer(player, track);
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Player promoted."));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/api/player/demote".equals(path)) {
                Map<String, Object> payload = readJsonPayload(exchange);
                String playerName = String.valueOf(payload.getOrDefault("player", "")).trim();
                String track = String.valueOf(payload.getOrDefault("track", "")).trim();
                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null) {
                    sendJson(exchange, mapOf("success", false, "message", "Player not online."));
                    return;
                }
                runSync(() -> {
                    groupManager.demotePlayer(player, track);
                    return true;
                });
                sendJson(exchange, mapOf("success", true, "message", "Player demoted."));
                return;
            }

            sendText(exchange, 404, "Not found");
        } catch (Exception ex) {
            sendJson(exchange, mapOf("success", false, "message", ex.getMessage()));
        }
    }

    private Map<String, Object> getServerInfo() {
        return runSync(() -> {
            Map<String, Object> info = new LinkedHashMap<>();
            long uptimeMs = System.currentTimeMillis() - BadApple.getPlugin(BadApple.class).getServerStartedAt();
            info.put("name", "BadApple");
            info.put("serverVersion", Bukkit.getVersion());
            info.put("playerCount", Bukkit.getOnlinePlayers().size());
            info.put("maxPlayers", Bukkit.getMaxPlayers());
            info.put("tps", 20.0);
            info.put("uptime", formatUptime(uptimeMs));
            info.put("memory", mapOf(
                    "used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                    "max", Runtime.getRuntime().maxMemory()
            ));
            return info;
        });
    }

    private List<Map<String, Object>> getPlayersData() {
        return runSync(() -> {
            List<Map<String, Object>> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", player.getName());
                row.put("uuid", player.getUniqueId().toString());
                row.put("world", player.getWorld().getName());
                row.put("health", Math.round(player.getHealth() * 10.0) / 10.0);
                row.put("food", player.getFoodLevel());
                row.put("group", groupManager.getGroup(player).isEmpty() ? "None" : groupManager.getGroup(player));
                players.add(row);
            }
            return players;
        });
    }

    private List<Map<String, Object>> getGroupsData() {
        return runSync(() -> {
            List<Map<String, Object>> groups = new ArrayList<>();
            for (String group : GroupConfig.getGroups()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", group);
                row.put("prefix", GroupConfig.getGroupPrefix(group));
                row.put("parent", groupManager.getParentGroup(group));
                row.put("weight", groupManager.getGroupWeight(group));
                row.put("permissions", GroupConfig.getPermissions(group));
                row.put("track", groupManager.getTrackForGroup(group));
                groups.add(row);
            }
            return groups;
        });
    }

    private List<Map<String, Object>> getTracksData() {
        return runSync(() -> {
            List<Map<String, Object>> tracks = new ArrayList<>();
            for (String track : GroupConfig.getTracks()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", track);
                row.put("groups", GroupConfig.getTrackGroups(track));
                tracks.add(row);
            }
            return tracks;
        });
    }

    private Map<String, Object> readJsonPayload(HttpExchange exchange) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return parseJsonMap(body.toString());
    }

    private Map<String, Object> parseJsonMap(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.isEmpty() || !trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return result;
        }

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }

        List<String> tokens = splitTopLevel(content, ',');
        for (String token : tokens) {
            String[] pair = token.split(":", 2);
            if (pair.length != 2) {
                continue;
            }
            String key = stripQuotes(pair[0].trim());
            String valueText = pair[1].trim();
            result.put(key, parseJsonValue(valueText));
        }
        return result;
    }

    private Object parseJsonValue(String valueText) {
        if (valueText.startsWith("\"") && valueText.endsWith("\"")) {
            return stripQuotes(valueText.trim());
        }
        if (valueText.startsWith("[") && valueText.endsWith("]")) {
            List<Object> list = new ArrayList<>();
            String inside = valueText.substring(1, valueText.length() - 1).trim();
            if (!inside.isEmpty()) {
                for (String item : splitTopLevel(inside, ',')) {
                    list.add(parseJsonValue(item.trim()));
                }
            }
            return list;
        }
        if ("true".equalsIgnoreCase(valueText)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valueText)) {
            return false;
        }
        if ("null".equalsIgnoreCase(valueText)) {
            return null;
        }
        if (valueText.contains(".")) {
            try {
                return Double.parseDouble(valueText);
            } catch (NumberFormatException ignored) {
                return valueText;
            }
        }
        try {
            return Long.parseLong(valueText);
        } catch (NumberFormatException ignored) {
            return valueText;
        }
    }

    private List<String> splitTopLevel(String input, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                current.append(ch);
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inQuotes = !inQuotes;
                current.append(ch);
                continue;
            }
            if (ch == delimiter && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"").replace("\\n", "\n");
        }
        return trimmed;
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000L;
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + secs + "s";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m " + secs + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    private void sendJson(HttpExchange exchange, Object payload) throws IOException {
        byte[] response = toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private void sendText(HttpExchange exchange, int statusCode, String payload) throws IOException {
        sendText(exchange, statusCode, payload, "text/plain; charset=utf-8");
    }

    private void sendText(HttpExchange exchange, int statusCode, String payload, String contentType) throws IOException {
        byte[] response = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private static Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }

    private <T> T runSync(Callable<T> callable) {
        try {
            Future<T> future = Bukkit.getScheduler().callSyncMethod(BadApple.getPlugin(BadApple.class), callable::call);
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("Dashboard task failed.", e);
        }
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof String) {
            String escaped = ((String) value).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            return '"' + escaped + '"';
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJson(String.valueOf(entry.getKey()))).append(':').append(toJson(entry.getValue()));
            }
            return builder.append('}').toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJson(item));
            }
            return builder.append(']').toString();
        }
        if (value.getClass().isArray()) {
            StringBuilder builder = new StringBuilder("[");
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(toJson(array[i]));
            }
            return builder.append(']').toString();
        }
        return toJson(String.valueOf(value));
    }

    private static String buildDashboardHtml() {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>BadApple Developer Console</title>
            <style>
                :root {
                    --bg: #10151d;
                    --panel: #1b2430;
                    --panel-alt: #202d3d;
                    --text: #edf2ff;
                    --muted: #9aa8b8;
                    --accent: #5ad1ff;
                    --accent-2: #75f0a9;
                    --danger: #ff7a7a;
                    --warning: #ffdd7a;
                    --border: #2f4153;
                }
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    background: linear-gradient(180deg, #0d1117, #10151d 25%, #0b1220 100%);
                    color: var(--text);
                    font-family: Arial, sans-serif;
                }
                .container { max-width: 1300px; margin: 0 auto; padding: 24px; }
                .topbar {
                    display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 18px;
                    padding: 16px 20px; background: rgba(27,36,48,0.95); border: 1px solid var(--border); border-radius: 14px;
                }
                .brand { font-size: 1.7rem; font-weight: 700; letter-spacing: 0.08em; }
                .chip { background: rgba(90,209,255,0.14); color: var(--accent); border: 1px solid rgba(90,209,255,0.3); border-radius: 999px; padding: 6px 12px; font-size: 0.8rem; }
                .grid {
                    display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px;
                    margin-bottom: 18px;
                }
                .card {
                    background: rgba(27,36,48,0.92); border: 1px solid var(--border); border-radius: 14px; box-shadow: 0 10px 18px rgba(0,0,0,0.18);
                    padding: 16px;
                }
                .card h3 { margin: 0 0 12px; font-size: 1rem; color: var(--muted); letter-spacing: 0.06em; text-transform: uppercase; }
                .stat { font-size: 1.9rem; font-weight: 700; }
                .muted { color: var(--muted); }
                .panel-grid {
                    display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 16px; margin-bottom: 18px;
                }
                table {
                    width: 100%; border-collapse: collapse; overflow: hidden; border-radius: 8px;
                }
                th, td {
                    text-align: left; padding: 10px 12px; border-bottom: 1px solid var(--border);
                    vertical-align: top;
                }
                th { background: rgba(255,255,255,0.02); color: var(--muted); font-size: 0.8rem; text-transform: uppercase; }
                tr:hover { background: rgba(255,255,255,0.02); }
                input, select, button {
                    width: 100%; margin-top: 8px; padding: 10px 12px; border-radius: 10px; border: 1px solid var(--border); background: var(--panel-alt); color: var(--text);
                }
                button {
                    cursor: pointer; background: linear-gradient(135deg, #1d7bd3, #4ad0ff); border: none; font-weight: 700; color: white;
                }
                button.danger { background: linear-gradient(135deg, #d93a4a, #ff7a7a); }
                button.success { background: linear-gradient(135deg, #0d9d70, #77eea2); }
                .inline { display: flex; gap: 8px; }
                .inline > * { flex: 1; }
                textarea {
                    width: 100%; min-height: 120px; resize: vertical; margin-top: 8px; padding: 12px; border-radius: 10px; border: 1px solid var(--border); background: var(--panel-alt); color: var(--text);
                }
                .pill { display: inline-block; background: rgba(117,240,169,0.12); color: var(--accent-2); border: 1px solid rgba(117,240,169,0.2); border-radius: 999px; padding: 4px 8px; margin: 2px 4px 2px 0; }
                .bad { background: rgba(255,122,122,0.12); color: var(--danger); border-color: rgba(255,122,122,0.2); }
                .stack { display: flex; flex-direction: column; gap: 16px; }
                @media (max-width: 900px) {
                    .panel-grid { grid-template-columns: 1fr; }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="topbar">
                    <div>
                        <div class="brand">BADAPPLE</div>
                        <div class="muted">Developer Management Console</div>
                    </div>
                    <div class="chip" id="status-chip">Server Online</div>
                </div>

                <div class="grid" id="stats-grid"></div>

                <div class="panel-grid">
                    <div class="card stack">
                        <div>
                            <h3>Console</h3>
                            <textarea id="console-command" placeholder="dispatchCommand ..."></textarea>
                            <button class="success" onclick="runConsoleCommand()">Execute Console Command</button>
                        </div>
                        <div>
                            <h3>Group Manager</h3>
                            <div class="inline">
                                <input id="group-name" placeholder="Group name" />
                                <button onclick="createGroup()">Create</button>
                            </div>
                            <div class="inline">
                                <input id="group-prefix" placeholder="Prefix" />
                                <button onclick="setGroupPrefix()">Set Prefix</button>
                            </div>
                            <div class="inline">
                                <input id="group-perm" placeholder="Permission node" />
                                <button onclick="toggleGroupPermission()">Add/Toggle</button>
                            </div>
                        </div>
                        <div>
                            <h3>Track Manager</h3>
                            <div class="inline">
                                <input id="track-name" placeholder="Track name" />
                                <button onclick="createTrack()">Create Track</button>
                            </div>
                            <div class="inline">
                                <input id="track-group" placeholder="Group name" />
                                <button onclick="addGroupToTrack()">Add To Track</button>
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h3>Player Actions</h3>
                        <select id="player-select"></select>
                        <select id="track-select" style="margin-top: 10px;"></select>
                        <button class="success" onclick="promotePlayer()" style="margin-top: 12px;">Promote</button>
                        <button class="danger" onclick="demotePlayer()" style="margin-top: 8px;">Demote</button>
                    </div>
                </div>

                <div class="card" style="margin-bottom: 18px;">
                    <h3>Groups</h3>
                    <div id="groups-table"></div>
                </div>

                <div class="card" style="margin-bottom: 18px;">
                    <h3>Tracks</h3>
                    <div id="tracks-table"></div>
                </div>

                <div class="card">
                    <h3>Players</h3>
                    <div id="players-table"></div>
                </div>
            </div>

            <script>
                const state = { server: null, groups: [], tracks: [], players: [] };

                function escapeHtml(value) {
                    return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\"/g, '&quot;');
                }

                function renderStats(server) {
                    const cards = [
                        ['Players', `${server.playerCount}/${server.maxPlayers}`],
                        ['TPS', `${server.tps}`],
                        ['Uptime', `${server.uptime}`],
                        ['Memory', `${Math.round((server.memory.used / 1048576))} MB / ${Math.round((server.memory.max / 1048576))} MB`]
                    ];
                    document.getElementById('stats-grid').innerHTML = cards.map(([label, value]) => `
                        <div class="card">
                            <h3>${label}</h3>
                            <div class="stat">${escapeHtml(value)}</div>
                        </div>
                    `).join('');
                }

                function renderGroups() {
                    if (!state.groups.length) {
                        document.getElementById('groups-table').innerHTML = '<div class="muted">No groups found.</div>';
                        return;
                    }
                    document.getElementById('groups-table').innerHTML = `
                        <table>
                            <thead><tr><th>Name</th><th>Prefix</th><th>Parent</th><th>Weight</th><th>Track</th><th>Permissions</th></tr></thead>
                            <tbody>
                                ${state.groups.map(group => `
                                    <tr>
                                        <td>${escapeHtml(group.name)}</td>
                                        <td>${escapeHtml(group.prefix || 'None')}</td>
                                        <td>${escapeHtml(group.parent || 'None')}</td>
                                        <td>${escapeHtml(group.weight || '0')}</td>
                                        <td>${escapeHtml(group.track || 'None')}</td>
                                        <td>${group.permissions && group.permissions.length ? group.permissions.map(p => `<span class="pill">${escapeHtml(p)}</span>`).join('') : '<span class="pill bad">None</span>'}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `;
                }

                function renderTracks() {
                    if (!state.tracks.length) {
                        document.getElementById('tracks-table').innerHTML = '<div class="muted">No tracks found.</div>';
                        return;
                    }
                    document.getElementById('tracks-table').innerHTML = `
                        <table>
                            <thead><tr><th>Track</th><th>Order</th></tr></thead>
                            <tbody>
                                ${state.tracks.map(track => `
                                    <tr>
                                        <td>${escapeHtml(track.name)}</td>
                                        <td>${track.groups.length ? track.groups.map(group => `<span class="pill">${escapeHtml(group)}</span>`).join(' ') : '<span class="pill bad">Empty</span>'}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `;
                }

                function renderPlayers() {
                    const playerSelect = document.getElementById('player-select');
                    playerSelect.innerHTML = state.players.length ? state.players.map(player => `<option value="${escapeHtml(player.name)}">${escapeHtml(player.name)} (${escapeHtml(player.group)})</option>`).join('') : '<option value="">No players online</option>';

                    const tracks = state.tracks.map(track => track.name);
                    const trackSelect = document.getElementById('track-select');
                    trackSelect.innerHTML = tracks.length ? tracks.map(track => `<option value="${escapeHtml(track)}">${escapeHtml(track)}</option>`).join('') : '<option value="">No tracks</option>';

                    if (!state.players.length) {
                        document.getElementById('players-table').innerHTML = '<div class="muted">No players online.</div>';
                        return;
                    }
                    document.getElementById('players-table').innerHTML = `
                        <table>
                            <thead><tr><th>Name</th><th>Group</th><th>World</th><th>Health</th><th>Food</th></tr></thead>
                            <tbody>
                                ${state.players.map(player => `
                                    <tr>
                                        <td>${escapeHtml(player.name)}</td>
                                        <td>${escapeHtml(player.group)}</td>
                                        <td>${escapeHtml(player.world)}</td>
                                        <td>${escapeHtml(player.health)}</td>
                                        <td>${escapeHtml(player.food)}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `;
                }

                async function refreshData() {
                    state.server = await fetch('/api/server').then(r => r.json());
                    state.groups = await fetch('/api/groups').then(r => r.json());
                    state.tracks = await fetch('/api/tracks').then(r => r.json());
                    state.players = await fetch('/api/players').then(r => r.json());
                    renderStats(state.server);
                    renderGroups();
                    renderTracks();
                    renderPlayers();
                }

                async function runConsoleCommand() {
                    const command = document.getElementById('console-command').value.trim();
                    if (!command) return;
                    const response = await fetch('/api/console', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ command })
                    }).then(r => r.json());
                    alert(response.message || 'Command executed.');
                    refreshData();
                }

                async function createGroup() {
                    const name = document.getElementById('group-name').value.trim();
                    if (!name) return;
                    const response = await fetch('/api/groups/create', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ name })
                    }).then(r => r.json());
                    alert(response.message || 'Group created.');
                    refreshData();
                }

                async function setGroupPrefix() {
                    const name = document.getElementById('group-name').value.trim();
                    const prefix = document.getElementById('group-prefix').value.trim();
                    if (!name || !prefix) return;
                    const response = await fetch('/api/groups/prefix', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ name, prefix })
                    }).then(r => r.json());
                    alert(response.message || 'Group prefix updated.');
                    refreshData();
                }

                async function toggleGroupPermission() {
                    const name = document.getElementById('group-name').value.trim();
                    const permission = document.getElementById('group-perm').value.trim();
                    if (!name || !permission) return;
                    const existing = state.groups.find(g => g.name === name);
                    const enabled = !(existing && existing.permissions && existing.permissions.includes(permission));
                    const response = await fetch('/api/groups/permission', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ name, permission, enabled })
                    }).then(r => r.json());
                    alert(response.message || 'Permission updated.');
                    refreshData();
                }

                async function createTrack() {
                    const name = document.getElementById('track-name').value.trim();
                    if (!name) return;
                    const response = await fetch('/api/tracks/create', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ name, groups: [] })
                    }).then(r => r.json());
                    alert(response.message || 'Track created.');
                    refreshData();
                }

                async function addGroupToTrack() {
                    const track = document.getElementById('track-select').value;
                    const group = document.getElementById('track-group').value.trim();
                    if (!track || !group) return;
                    const response = await fetch('/api/tracks/addgroup', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ track, group })
                    }).then(r => r.json());
                    alert(response.message || 'Group added.');
                    refreshData();
                }

                async function promotePlayer() {
                    const player = document.getElementById('player-select').value;
                    const track = document.getElementById('track-select').value;
                    if (!player || !track) return;
                    const response = await fetch('/api/player/promote', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ player, track })
                    }).then(r => r.json());
                    alert(response.message || 'Player promoted.');
                    refreshData();
                }

                async function demotePlayer() {
                    const player = document.getElementById('player-select').value;
                    const track = document.getElementById('track-select').value;
                    if (!player || !track) return;
                    const response = await fetch('/api/player/demote', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ player, track })
                    }).then(r => r.json());
                    alert(response.message || 'Player demoted.');
                    refreshData();
                }

                refreshData();
                setInterval(refreshData, 6000);
            </script>
        </body>
        </html>
        """;
    }
}
