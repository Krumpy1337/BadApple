package com.krumpy1337.badapple.Global;

import com.krumpy1337.badapple.Local.Commands.GroupMenuCommand;
import com.krumpy1337.badapple.Local.Permissions.GroupManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Intercepts common private-message commands (msg/tell/m/pm) and routes them through BadApple's chat formatter
 * so prefixes/suffixes are preserved and Essentials-style messages still appear consistent.
 */
public class EssentialsCommandBridge implements Listener {
    private final GroupManager groupManager = new GroupManager();

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        Player sender = event.getPlayer();

        if (GroupMenuCommand.hasPendingInput(sender)) {
            return; // let other logic handle pending input
        }

        String lower = msg.toLowerCase();
        String[] aliases = new String[]{"/msg ", "/tell ", "/m ", "/pm ", "/whisper ", "/w "};
        String used = null;
        for (String a : aliases) {
            if (lower.startsWith(a)) {
                used = a.trim();
                break;
            }
        }
        if (used == null) return;

        // parse: /msg <target> <message...>
        String[] parts = msg.split(" ", 3);
        if (parts.length < 3) {
            sender.sendMessage("§cUsage: /msg <player> <message>");
            event.setCancelled(true);
            return;
        }

        String targetName = parts[1];
        String message = parts[2];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer is not online.");
            event.setCancelled(true);
            return;
        }

        // Respect mute
        if (sender.hasMetadata("muted")) {
            sender.sendMessage("§cYou are muted and cannot message others.");
            event.setCancelled(true);
            return;
        }

        // Build formatted components using BadApple's prefix/suffix logic
        String groupName = groupManager.getGroup(sender);
        String rankName = (groupName == null || groupName.isEmpty()) ? "Player" : groupName;
        String roleName = "";
        switch (rankName.toLowerCase()) {
            case "admin": roleName = "Administrator"; break;
            case "mod": roleName = "Moderator"; break;
            case "developer":
            case "dev": roleName = "Developer"; break;
            default: roleName = rankName;
        }

        String prefixText = groupManager.groupPrefix(groupName);
        String suffixText = groupManager.getUserSuffix(sender);
        String formattedSuffix = suffixText == null || suffixText.trim().isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', suffixText.trim());
        String displayName = sender.getName() + (formattedSuffix.isEmpty() ? "" : " " + formattedSuffix);

        TextComponent rankComponent = new TextComponent(prefixText.isEmpty() ? "" : ColorUtil.FormatMessage(prefixText));
        rankComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                "Rank: " + rankName + "\nRole: " + roleName
        )));

        TextComponent nameComponent = new TextComponent(displayName);
        nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                "Rank: " + rankName + "\nRole: " + roleName + "\nClick to message @" + sender.getName()
        )));
        nameComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + sender.getName() + " "));

        TextComponent arrow = new TextComponent(ChatColor.translateAlternateColorCodes('&', " §a>> "));
        TextComponent messageComponent = new TextComponent(message);

        // Send to recipient and sender (private)
        target.spigot().sendMessage(rankComponent, nameComponent, arrow, messageComponent);
        sender.spigot().sendMessage(new TextComponent("§7To §f" + target.getName() + "§7: "), messageComponent);

        event.setCancelled(true); // prevent Essentials or default handlers from also sending
    }
}
