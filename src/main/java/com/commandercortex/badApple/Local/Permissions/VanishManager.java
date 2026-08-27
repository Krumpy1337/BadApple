package com.commandercortex.badApple.Local.Permissions;

import com.commandercortex.badApple.BadApple;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public final class VanishManager {
    public static final String VANISHED_KEY = "badapple_vanished";

    private VanishManager() {
    }

    public static boolean isVanished(Player player) {
        return player != null && player.hasMetadata(VANISHED_KEY);
    }

    public static void setVanished(Player player, boolean vanished) {
        if (player == null) {
            return;
        }

        BadApple plugin = BadApple.getPlugin(BadApple.class);
        if (vanished) {
            if (!isVanished(player)) {
                player.setMetadata(VANISHED_KEY, new FixedMetadataValue(plugin, true));
            }
            player.setGameMode(GameMode.SPECTATOR);
            player.setFlying(true);
            player.setAllowFlight(true);
            applyVisibility(player, true);
            player.sendMessage("§aYou are now vanished.");
            return;
        }

        player.removeMetadata(VANISHED_KEY, plugin);
        player.setGameMode(Bukkit.getDefaultGameMode());
        player.setFlying(false);
        player.setAllowFlight(false);
        applyVisibility(player, false);
        player.sendMessage("§aYou are no longer vanished.");
    }

    public static void applyVisibility(Player player, boolean vanishState) {
        if (player == null) {
            return;
        }

        BadApple plugin = BadApple.getPlugin(BadApple.class);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) {
                continue;
            }
            if (vanishState) {
                other.hidePlayer(plugin, player);
            } else {
                other.showPlayer(plugin, player);
            }
        }
    }

    public static void refreshForPlayer(Player player) {
        applyVisibility(player, isVanished(player));
    }

    public static void refreshForJoin(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) {
                continue;
            }
            if (isVanished(other)) {
                player.hidePlayer(BadApple.getPlugin(BadApple.class), other);
            } else {
                player.showPlayer(BadApple.getPlugin(BadApple.class), other);
            }
        }
    }
}
