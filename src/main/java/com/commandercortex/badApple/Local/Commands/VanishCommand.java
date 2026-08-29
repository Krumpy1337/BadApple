package com.commandercortex.badApple.Local.Commands;

import com.commandercortex.badApple.BadApple;
import com.commandercortex.badApple.Local.Permissions.VanishManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Toggles vanish (spectator) mode for the executing player and updates visibility for others.
 */
public class VanishCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("badapple.vanish")) {
            player.sendMessage("§cYou do not have permission to vanish.");
            return true;
        }

        boolean vanished = !VanishManager.isVanished(player);
        VanishManager.setVanished(player, vanished);
        if (vanished) {
            player.sendMessage("§aVanish enabled: spectator mode active.");
        } else {
            player.sendMessage("§aVanish disabled: standard mode restored.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
