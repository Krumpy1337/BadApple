package com.commandercortex.badApple.Local.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NicknameCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length < 2) {
            sender.sendMessage("§cConsole must specify a player name and nickname.");
            return true;
        }

        if (!sender.isOp() && !sender.hasPermission("badapple.nickname")) {
            sender.sendMessage("§cYou do not have permission to change nicknames.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /nickname <nickname> | /nickname <player> <nickname> | /nickname clear [player]");
            return true;
        }

        if ("clear".equalsIgnoreCase(args[0])) {
            Player target = null;
            if (args.length >= 2) {
                if (!sender.isOp() && !sender.hasPermission("badapple.nickname.other")) {
                    sender.sendMessage("§cYou do not have permission to clear another player's nickname.");
                    return true;
                }
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer '" + args[1] + "' is not online.");
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cConsole must specify a player name.");
                return true;
            }

            target.setDisplayName(target.getName());
            target.setPlayerListName(target.getName());
            sender.sendMessage("§aCleared nickname for " + target.getName() + ".");
            return true;
        }

        Player target = null;
        String nickname;
        if (args.length >= 2) {
            if (!sender.isOp() && !sender.hasPermission("badapple.nickname.other")) {
                sender.sendMessage("§cYou do not have permission to set another player's nickname.");
                return true;
            }

            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' is not online.");
                return true;
            }
            nickname = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        } else if (sender instanceof Player) {
            target = (Player) sender;
            nickname = String.join(" ", args);
        } else {
            sender.sendMessage("§cConsole must specify a player name and nickname.");
            return true;
        }

        if (nickname.length() > 32) {
            sender.sendMessage("§cNickname is too long. Max 32 characters.");
            return true;
        }

        target.setDisplayName(nickname);
        target.setPlayerListName(nickname);
        sender.sendMessage("§aSet nickname for " + target.getName() + " to §r" + nickname + "§a.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("clear");
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
            return suggestions;
        }
        return new ArrayList<>();
    }
}
