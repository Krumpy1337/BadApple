package com.commandercortex.badApple.Local.Commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HelpCommand implements CommandExecutor, TabCompleter {
    private static final List<CommandInfo> COMMANDS = Arrays.asList(
            new CommandInfo("/group", "Manage groups and permissions", "badapple.group"),
            new CommandInfo("/groupmenu", "Open the group manager GUI", "badapple.group"),
            new CommandInfo("/punishmenu", "Open the punish actions menu", "badapple.punish"),
            new CommandInfo("/vanish", "Toggle vanish mode", "badapple.vanish"),
            new CommandInfo("/broadcast", "Send a server-wide announcement", "badapple.broadcast"),
            new CommandInfo("/fly", "Toggle flight", "badapple.fly"),
            new CommandInfo("/gamemode", "Change a player gamemode", "badapple.gamemode"),
            new CommandInfo("/nickname", "Set or clear a nickname", "badapple.nickname")
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage("§cInvalid page number. Use /badapplehelp [page]");
                return true;
            }
        }

        List<CommandInfo> accessible = new ArrayList<>();
        boolean isOp = sender.isOp();
        for (CommandInfo info : COMMANDS) {
            if (isOp || sender.hasPermission(info.permissionNode)) {
                accessible.add(info);
            }
        }

        if (accessible.isEmpty()) {
            sender.sendMessage("§cYou do not have access to any BadApple commands.");
            return true;
        }

        int pageSize = 5;
        int totalPages = (int) Math.ceil(accessible.size() / (double) pageSize);
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, accessible.size());

        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§b§lBadApple Help §7(Page " + page + "/" + totalPages + ")");
        sender.sendMessage("§8§m----------------------------------------");

        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (int i = start; i < end; i++) {
                CommandInfo info = accessible.get(i);
                TextComponent component = new TextComponent("§7- §f" + info.command + " §8- §7" + info.description);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, info.command + " "));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to type: " + info.command)));
                player.spigot().sendMessage(component);
            }
        } else {
            for (int i = start; i < end; i++) {
                CommandInfo info = accessible.get(i);
                sender.sendMessage("§7- §f" + info.command + " §8- §7" + info.description);
            }
        }

        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§7Use §f/badapplehelp " + (page < totalPages ? (page + 1) : 1) + " §7for the next page.");
        sender.sendMessage("§8§m----------------------------------------");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> pages = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                pages.add(String.valueOf(i));
            }
            return pages;
        }
        return Collections.emptyList();
    }

    private static final class CommandInfo {
        private final String command;
        private final String description;
        private final String permissionNode;

        private CommandInfo(String command, String description, String permissionNode) {
            this.command = command;
            this.description = description;
            this.permissionNode = permissionNode;
        }
    }
}
