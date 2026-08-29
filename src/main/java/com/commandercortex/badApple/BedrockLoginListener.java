package com.commandercortex.badApple;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

/**
 * Detects Bedrock connections (via hostname or presence of Geyser/Floodgate) and blocks them if configured.
 */
public class BedrockLoginListener implements Listener {

    private final BadApple plugin;

    public BedrockLoginListener(BadApple plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        // If a Geyser/Floodgate plugin is installed, allow Bedrock connections to be handled by that plugin.
        if (isBedrockPluginPresent()) {
            return;
        }

        // No Bedrock bridge plugin found. Do not attempt to block Bedrock connections here —
        // the server will not accept Bedrock protocol connections without a bridge. Log once for visibility.
        plugin.getLogger().info("No Geyser/Floodgate plugin detected. Bedrock clients will not be able to connect unless a bridge plugin is installed.");
    }

    private boolean isBedrockPluginPresent() {
        return Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null || Bukkit.getPluginManager().getPlugin("Floodgate") != null
                || Bukkit.getPluginManager().getPlugin("Geyser") != null;
    }
}
