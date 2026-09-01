package com.commandercortex.badApple.Local.Management;

import com.commandercortex.badApple.BadApple;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages launching an external process (e.g., Geyser standalone JAR) from the plugin data folder.
 * Place the desired standalone JAR at: <server>/plugins/BadApple/embedded/geyser.jar
 */
public class EmbeddedBridge {
    private final BadApple plugin;
    private Process process;
    private Thread logThread;

    public EmbeddedBridge(BadApple plugin) {
        this.plugin = plugin;
    }

    public void startIfConfigured() {
        boolean enabled = plugin.getConfig().getBoolean("bedrock.embedded.enabled", false);
        if (!enabled) {
            plugin.getLogger().info("Embedded bridge disabled in config.");
            return;
        }

        // If a Geyser/Floodgate plugin is already installed in the server's plugins folder prefer that
        try {
            org.bukkit.plugin.PluginManager pm = org.bukkit.Bukkit.getPluginManager();
            for (org.bukkit.plugin.Plugin p : pm.getPlugins()) {
                if (p == null || p.getName() == null) continue;
                String name = p.getName().toLowerCase(java.util.Locale.ROOT);
                if (name.contains("geyser") || name.contains("floodgate")) {
                    plugin.getLogger().info("Detected existing plugin '" + p.getName() + "' in plugins folder. Skipping embedded bridge startup.");
                    return;
                }
            }
        } catch (NoClassDefFoundError | Exception ex) {
            // If Bukkit isn't available for some reason, fall through and attempt to start the embedded bridge.
            plugin.getLogger().warning("Could not query plugin manager for existing Geyser/Floodgate plugins: " + ex.getMessage());
        }

        String jarPath = plugin.getConfig().getString("bedrock.embedded.jar", "embedded/geyser.jar");
        File jarFile = new File(plugin.getDataFolder(), jarPath);
        if (!jarFile.exists()) {
            plugin.getLogger().warning("Embedded bridge JAR not found: " + jarFile.getAbsolutePath());
            plugin.getLogger().warning("Place the Geyser/Floodgate standalone JAR at the path above to enable embedded Bedrock bridging.");
            return;
        }

        try {
            startProcess(jarFile);
            plugin.getLogger().info("Embedded bridge started using: " + jarFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start embedded bridge.", e);
        }
    }

    private void startProcess(File jar) throws IOException {
        List<String> cmd = new ArrayList<>();
        String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        cmd.add(javaExe);
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());

        String extra = plugin.getConfig().getString("bedrock.embedded.args", "").trim();
        if (!extra.isEmpty()) {
            // naive split on spaces; fine for simple args
            for (String part : extra.split(" ")) {
                if (!part.isEmpty()) cmd.add(part);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(plugin.getDataFolder(), "embedded"));
        pb.redirectErrorStream(true);

        process = pb.start();

        // spawn a thread to stream process output to plugin logger
        logThread = new Thread(() -> {
            try (InputStream is = process.getInputStream(); InputStreamReader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr)) {
                String line;
                while ((line = br.readLine()) != null) {
                    plugin.getLogger().info("[EmbeddedBridge] " + line);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error reading embedded bridge output", e);
            }
        }, "EmbeddedBridge-LogThread");
        logThread.setDaemon(true);
        logThread.start();
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            plugin.getLogger().info("Stopping embedded bridge process...");
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (logThread != null && logThread.isAlive()) {
            logThread.interrupt();
        }
    }
}
