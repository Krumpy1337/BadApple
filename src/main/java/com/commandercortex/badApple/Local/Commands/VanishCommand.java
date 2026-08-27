package com.commandercortex.badApple.Local.Commands;

import com.commandercortex.badApple.BadApple;
import com.commandercortex.badApple.Local.Permissions.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) {
                continue;
            }
            if (vanished) {
                other.hidePlayer(BadApple.getPlugin(BadApple.class), player);
            } else {
                other.showPlayer(BadApple.getPlugin(BadApple.class), player);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
