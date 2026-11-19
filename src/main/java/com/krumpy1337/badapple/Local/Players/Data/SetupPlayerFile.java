package com.krumpy1337.badapple.Local.Players.Data;

import com.krumpy1337.badapple.Global.ColorUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/**
 * Initializes player data files with default values when a player joins for the first time.
 */
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
        playerdata.addDefault("Group:", "Copper");
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
