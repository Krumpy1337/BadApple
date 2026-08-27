package com.commandercortex.badApple.Local.Commands;

import com.commandercortex.badApple.Local.Permissions.GroupManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BadAppleCommand implements CommandExecutor, TabCompleter {
    private final GroupManager groupManager = new GroupManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean hasPermission = sender.isOp() || sender.hasPermission("badapple.group");
        if (!hasPermission) {
            sender.sendMessage("§cYou do not have permission to manage groups.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /group <create|delete|setprefix|setinherit|addperm|removeperm|adduser|removeuser|list>");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "create":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /group create <group>");
                    return true;
                }
                if (groupManager.groupExists(args[1])) {
                    sender.sendMessage("§cGroup '" + args[1] + "' already exists.");
                    return true;
                }
                groupManager.createGroup(args[1]);
                sender.sendMessage("§aCreated group '" + args[1] + "'.");
                return true;
            case "delete":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /group delete <group>");
                    return true;
                }
                if (!groupManager.groupExists(args[1])) {
                    sender.sendMessage("§cGroup '" + args[1] + "' does not exist.");
                    return true;
                }
                groupManager.deleteGroup(args[1]);
                sender.sendMessage("§aDeleted group '" + args[1] + "'.");
                return true;
            case "adduser":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /group adduser <player> <group>");
                    return true;
                }
                if (!groupManager.groupExists(args[2])) {
                    sender.sendMessage("§cGroup '" + args[2] + "' does not exist.");
                    return true;
                }
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer '" + args[1] + "' is not online.");
                    return true;
                }
                groupManager.setGroup(target, args[2]);
                groupManager.TabManager();
                sender.sendMessage("§aAssigned '" + target.getName() + "' to group '" + args[2] + "'.");
                return true;
            case "removeuser":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /group removeuser <player>");
                    return true;
                }
                org.bukkit.entity.Player playerToRemove = org.bukkit.Bukkit.getPlayerExact(args[1]);
                if (playerToRemove == null) {
                    sender.sendMessage("§cPlayer '" + args[1] + "' is not online.");
                    return true;
                }
                groupManager.setGroup(playerToRemove, "");
                groupManager.TabManager();
                sender.sendMessage("§aRemoved '" + playerToRemove.getName() + "' from their group.");
                return true;
            case "setprefix":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /group setprefix <group> <prefix>");
                    return true;
                }
                String prefix = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (!groupManager.groupExists(args[1])) {
                    sender.sendMessage("§cGroup '" + args[1] + "' does not exist.");
                    return true;
                }
                groupManager.setGroupPrefix(args[1], prefix);
                groupManager.TabManager();
                sender.sendMessage("§aUpdated prefix for '" + args[1] + "'.");
                return true;
            case "setinherit":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /group setinherit <group> <parent-group>");
                    return true;
                }
                if (!groupManager.groupExists(args[1])) {
                    sender.sendMessage("§cGroup '" + args[1] + "' does not exist.");
                    return true;
                }
                if (!args[2].equalsIgnoreCase("none") && !groupManager.groupExists(args[2])) {
                    sender.sendMessage("§cParent group '" + args[2] + "' does not exist.");
                    return true;
                }
                groupManager.setParentGroup(args[1], args[2].equalsIgnoreCase("none") ? "" : args[2]);
                groupManager.TabManager();
                sender.sendMessage("§aUpdated inheritance for '" + args[1] + "'.");
                return true;
            case "addperm":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /group addperm <group> <permission>");
                    return true;
                }
                String permission = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (!groupManager.groupExists(args[1])) {
                    sender.sendMessage("§cGroup '" + args[1] + "' does not exist.");
                    return true;
                }
                groupManager.addPermission(args[1], permission);
                groupManager.TabManager();
                sender.sendMessage("§aAdded permission '" + permission + "' to group '" + args[1] + "'.");
                return true;
            case "removeperm":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /group removeperm <group> <permission>");
                    return true;
                }
                String removePermission = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (!groupManager.groupExists(args[1])) {
                    sender.sendMessage("§cGroup '" + args[1] + "' does not exist.");
                    return true;
                }
                groupManager.removePermission(args[1], removePermission);
                groupManager.TabManager();
                sender.sendMessage("§aRemoved permission '" + removePermission + "' from group '" + args[1] + "'.");
                return true;
            case "list":
                if (groupManager.getGroups().isEmpty()) {
                    sender.sendMessage("§cNo groups have been created yet.");
                    return true;
                }
                sender.sendMessage("§aGroups:");
                for (String group : groupManager.getGroups()) {
                    String inherited = groupManager.getParentGroup(group);
                    sender.sendMessage("§7- " + group + " | parent: " + (inherited.isEmpty() ? "none" : inherited) + " | permissions: " + String.join(", ", groupManager.getPermissions(group)));
                }
                return true;
            default:
                sender.sendMessage("§cUnknown command. Use /group <create|delete|setprefix|setinherit|addperm|removeperm|adduser|removeuser|list>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        List<String> actions = Arrays.asList("create", "delete", "setprefix", "setinherit", "addperm", "removeperm", "adduser", "removeuser", "list");

        if (args.length == 1) {
            for (String action : actions) {
                if (action.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    suggestions.add(action);
                }
            }
            return suggestions;
        }

        if (args.length > 1 && ("delete".equalsIgnoreCase(args[0]) || "create".equalsIgnoreCase(args[0]) || "setprefix".equalsIgnoreCase(args[0]) || "setinherit".equalsIgnoreCase(args[0]) || "addperm".equalsIgnoreCase(args[0]) || "removeperm".equalsIgnoreCase(args[0]) || "adduser".equalsIgnoreCase(args[0]))) {
            for (String group : groupManager.getGroups()) {
                if (group.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    suggestions.add(group);
                }
            }
        }

        return suggestions;
    }
}
