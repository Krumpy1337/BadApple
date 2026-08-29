package com.commandercortex.badApple.Global;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utility methods for formatting and sending colored/component messages to players.
 */
public class ColorUtil {
    public static String plugin_prefix = "&7[&b&lBadApple&r&7] &r-> &r";

    public static String FormatMessage(String message){
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    public static void PlayerMessage(String message, Player player){
        player.sendMessage(FormatMessage(plugin_prefix + message));
    }
    public static void ComponentMessage(String message, Player player){
       player.spigot().sendMessage(BaseTextComponent.LinkBaseText(FormatMessage(plugin_prefix + message), "https://github.com/CommanderCortex/", FormatMessage(plugin_prefix + "Click me to find my Github!")));
    }
    public static void CustomComponentMessage(String message, String clickEvent, String hoverEvent, Player player){
        player.spigot().sendMessage(BaseTextComponent.LinkBaseText(FormatMessage(plugin_prefix + message), clickEvent, FormatMessage(plugin_prefix + hoverEvent)));
    }
}
