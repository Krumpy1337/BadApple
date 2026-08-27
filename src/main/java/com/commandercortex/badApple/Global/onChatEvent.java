package com.commandercortex.badApple.Global;

import com.commandercortex.badApple.Local.Commands.GroupMenuCommand;
import com.commandercortex.badApple.Local.Permissions.GroupManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class onChatEvent implements Listener {
    GroupManager groupManager = new GroupManager();

    @EventHandler
    public void chat(AsyncPlayerChatEvent chatEvent) {
        Player player = chatEvent.getPlayer();
        if (GroupMenuCommand.hasPendingInput(player)) {
            chatEvent.setCancelled(true);
            return;
        }

        if (player.hasMetadata("muted")) {
            chatEvent.setCancelled(true);
            player.sendMessage("§cYou are muted and cannot speak.");
            return;
        }

        chatEvent.setCancelled(true);

        String groupName = groupManager.getGroup(player);
        String rankName = (groupName == null || groupName.isEmpty()) ? "Player" : groupName;
        String roleName = resolveRoleName(rankName);

        String prefixText = groupManager.groupPrefix(groupName);
        String suffixText = groupManager.getUserSuffix(player);
        String formattedSuffix = suffixText == null || suffixText.trim().isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', suffixText.trim());
        String displayName = player.getName() + (formattedSuffix.isEmpty() ? "" : " " + formattedSuffix);

        TextComponent rankComponent = new TextComponent(prefixText.isEmpty() ? "" : ColorUtil.FormatMessage(prefixText));
        rankComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                "Rank: " + rankName + "\nRole: " + roleName
        )));

        TextComponent nameComponent = new TextComponent(displayName);
        nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                "Rank: " + rankName + "\nRole: " + roleName + "\nClick to message @" + player.getName()
        )));
        nameComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + player.getName() + " "));

        TextComponent messageArrow = new TextComponent(ChatColor.translateAlternateColorCodes('&', " §a>> "));
        TextComponent messageComponent = new TextComponent(chatEvent.getMessage());

        Bukkit.spigot().broadcast(rankComponent, nameComponent, messageArrow, messageComponent);
    }

    private String resolveRoleName(String rankName) {
        if (rankName == null || rankName.isEmpty()) {
            return "Player";
        }

        switch (rankName.toLowerCase()) {
            case "admin":
                return "Administrator";
            case "mod":
                return "Moderator";
            case "developer":
                return "Developer";
            case "dev":
                return "Developer";
            default:
                return rankName;
        }
    }
}
