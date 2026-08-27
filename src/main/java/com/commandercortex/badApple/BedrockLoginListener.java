package com.commandercortex.badApple;

import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

public class BedrockLoginListener implements Listener {

    private final BadApple plugin;

    public BedrockLoginListener(BadApple plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (plugin.getConfig().getBoolean("bedrock.enabled", true)) {
            return;
        }

        if (!isBedrockConnection(event)) {
            return;
        }

        event.disallow(
            Result.KICK_OTHER,
            plugin.getConfig().getString("bedrock.kick-message", "Bedrock players are currently disabled on this server.")
        );
    }

    private boolean isBedrockConnection(PlayerLoginEvent event) {
        String host = event.getHostname();
        if (host != null && host.toLowerCase(Locale.ROOT).contains("bedrock")) {
            return true;
        }

        return Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null || Bukkit.getPluginManager().getPlugin("Floodgate") != null;
    }
}
