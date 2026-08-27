package com.commandercortex.badApple.Local.Players.Events;

import com.commandercortex.badApple.Global.ColorUtil;
import com.commandercortex.badApple.Local.Permissions.GroupManager;
import com.commandercortex.badApple.Local.Permissions.VanishManager;
import com.commandercortex.badApple.Local.Players.Data.SetupPlayerFile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class onPlayerJoinEvent implements Listener {
    SetupPlayerFile setupPlayerFile = new SetupPlayerFile();
    GroupManager groupManager = new GroupManager();

    @EventHandler
    public int onPlayerJoin(PlayerJoinEvent playerJoinEvent){
        Player player = playerJoinEvent.getPlayer();

        playerJoinEvent.setJoinMessage(null);

        setupPlayerFile.newPlayer(player);
        VanishManager.refreshForJoin(player);

        groupManager.TabManager();

        return 0;
    }
}
