package com.commandercortex.badApple.Local.Players.Data;

import com.commandercortex.badApple.Global.ColorUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class SetupPlayerFile {

    public int newPlayer(Player player){
        File _player = new File(GetPlayerData.getPlayerData(player));
        if(!_player.exists()){
            try {
                _player.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        YamlConfiguration playerdata = YamlConfiguration.loadConfiguration(_player);

        playerdata.addDefault("Name:", player.getName());
        playerdata.addDefault("Nickname:", player.getDisplayName());
        playerdata.addDefault("Group:", "");
        playerdata.addDefault("Coins:", 0);

        playerdata.options().copyDefaults(true);

        try {
            playerdata.save(_player);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ColorUtil.ComponentMessage("Welcome to the Server! Your player data is up-to-date.", player);
        return 0;
    }
}
