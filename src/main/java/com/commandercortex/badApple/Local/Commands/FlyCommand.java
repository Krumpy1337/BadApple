package com.commandercortex.badApple.Local.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlyCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("badapple.fly")) {
            sender.sendMessage("§cYou do not have permission to toggle fly.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUsage: /fly <player> [on|off]");
                return true;
            }

            Player player = (Player) sender;
            boolean enabled = !player.getAllowFlight();
            setFly(player, enabled);
            sender.sendMessage(enabled ? "§aFly enabled for you." : "§aFly disabled for you.");
            return true;
        }

        boolean canManageOthers = sender.isOp() || sender.hasPermission("badapple.fly.other");
        if (args.length >= 2 && canManageOthers) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' is not online.");
                return true;
            }

            boolean enabled = parseEnabled(args[1]);
            setFly(target, enabled);
            sender.sendMessage("§aSet fly for " + target.getName() + " to " + (enabled ? "enabled" : "disabled") + ".");
            return true;
        }

        if (args.length == 1) {
            if ("on".equalsIgnoreCase(args[0]) || "off".equalsIgnoreCase(args[0])) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cConsole must specify a player name.");
                    return true;
                }

                Player player = (Player) sender;
                boolean enabled = parseEnabled(args[0]);
                setFly(player, enabled);
                sender.sendMessage("§aSet fly to " + (enabled ? "enabled" : "disabled") + " for you.");
                return true;
            }

            if (!canManageOthers) {
                sender.sendMessage("§cYou do not have permission to manage another player's fly status.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' is not online.");
                return true;
            }

            boolean enabled = !target.getAllowFlight();
            setFly(target, enabled);
            sender.sendMessage("§aToggled fly for " + target.getName() + ".");
            return true;
        }

        sender.sendMessage("§cUsage: /fly [player] [on|off]");
        return true;
    }

    private void setFly(Player player, boolean enabled) {
        player.setAllowFlight(enabled);
        player.setFlying(enabled);
    }

    private boolean parseEnabled(String value) {
        if ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "enable".equalsIgnoreCase(value)) {
            return true;
        }
        if ("off".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "disable".equalsIgnoreCase(value)) {
            return false;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            values.add("on");
            values.add("off");
            for (Player player : Bukkit.getOnlinePlayers()) {
                values.add(player.getName());
            }
            return values;
        }
        if (args.length == 2) {
            return Arrays.asList("on", "off");
        }
        return new ArrayList<>();
    }
}
