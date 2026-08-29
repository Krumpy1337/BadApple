package com.commandercortex.badApple.Local.Players.Events;

import com.commandercortex.badApple.Local.Permissions.GroupManager;
import com.commandercortex.badApple.Local.Permissions.PermissionManager;
import com.commandercortex.badApple.Local.Permissions.VanishManager;
import com.commandercortex.badApple.Local.Players.Data.SetupPlayerFile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player joins/quits to initialize data, apply permissions, vanish state and update the tab list.
 */
public class onPlayerJoinEvent implements Listener {
    SetupPlayerFile setupPlayerFile = new SetupPlayerFile();
    GroupManager groupManager = new GroupManager();

    @EventHandler
    public int onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();

        playerJoinEvent.setJoinMessage(null);

        setupPlayerFile.newPlayer(player);
        if (groupManager.getGroup(player) == null || groupManager.getGroup(player).trim().isEmpty()) {
            groupManager.setGroup(player, "Copper");
        }
        PermissionManager.applyGroupPermissions(player);
        VanishManager.refreshForJoin(player);
        groupManager.TabManager();

        return 0;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        Player player = playerQuitEvent.getPlayer();
        PermissionManager.clearPermissions(player);
        playerQuitEvent.setQuitMessage(null);
    }
}
