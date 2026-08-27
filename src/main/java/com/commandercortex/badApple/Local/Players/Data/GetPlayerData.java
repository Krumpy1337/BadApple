package com.commandercortex.badApple.Local.Players.Data;

import org.bukkit.entity.Player;

public class GetPlayerData {
    public static String getPlayerData(Player player){
        return "plugins/BadApple/players/" + player.getUniqueId() + ".yml";
    }
}
