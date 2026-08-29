package com.commandercortex.badApple.Local.Commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sets player gamemode for self or others with permission checks and tab completion.
 */
public class GamemodeCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("badapple.gamemode")) {
            sender.sendMessage("§cYou do not have permission to change gamemodes.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /gamemode <survival|creative|adventure|spectator> [player]");
            return true;
        }

        GameMode mode = parseGameMode(args[0]);
        if (mode == null) {
            sender.sendMessage("§cInvalid gamemode. Use survival, creative, adventure, or spectator.");
            return true;
        }

        Player target = null;
        if (args.length >= 2) {
            if (!sender.isOp() && !sender.hasPermission("badapple.gamemode.other")) {
                sender.sendMessage("§cYou do not have permission to change another player's gamemode.");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[1] + "' is not online.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player name.");
                return true;
            }
            target = (Player) sender;
        }

        target.setGameMode(mode);
        if (target.equals(sender)) {
            sender.sendMessage("§aYour gamemode was set to " + mode.name() + ".");
        } else {
            sender.sendMessage("§aSet " + target.getName() + " to gamemode " + mode.name() + ".");
            target.sendMessage("§aYour gamemode was set to " + mode.name() + ".");
        }
        return true;
    }

    private GameMode parseGameMode(String input) {
        switch (input.toLowerCase()) {
            case "0":
            case "survival":
                return GameMode.SURVIVAL;
            case "1":
            case "creative":
                return GameMode.CREATIVE;
            case "2":
            case "adventure":
                return GameMode.ADVENTURE;
            case "3":
            case "spectator":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("survival", "creative", "adventure", "spectator");
        }
        if (args.length == 2) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }
        return new ArrayList<>();
    }
}
