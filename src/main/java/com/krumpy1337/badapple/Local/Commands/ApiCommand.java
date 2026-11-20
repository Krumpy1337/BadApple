package com.krumpy1337.badapple.Local.Commands;

import com.krumpy1337.badapple.Local.Management.ServerApi;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class ApiCommand implements CommandExecutor, TabCompleter {
    private final ServerApi api;

    public ApiCommand(ServerApi api) {
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only an in-game operator can manage API keys. Keys are never printed to the console.");
            return true;
        }
        if (!player.isOp() && !player.hasPermission("badapple.api")) {
            player.sendMessage("§cYou do not have permission to manage API keys.");
            return true;
        }

        String action = args.length == 0 ? "generate" : args[0].toLowerCase();
        switch (action) {
            case "generate", "rotate" -> {
                if (!api.isEnabled()) {
                    player.sendMessage("§cThe API listener is unavailable. Check api.enabled, bind-address, port, and the server log.");
                    return true;
                }
                String key = api.generateKey();
                player.sendMessage("§aAPI key generated. Copy it now; it cannot be viewed again.");
                sendCopyable(player, "API key", key);
                sendApiUrl(player);
                player.sendMessage("§7This replaces any existing API key.");
            }
            case "revoke" -> player.sendMessage(api.revokeKey()
                    ? "§aAPI key revoked."
                    : "§eThere is no API key to revoke.");
            case "status" -> {
                player.sendMessage("§7API listener: " + (api.isEnabled() ? "§a" + api.getAddress() : "§cdisabled"));
                player.sendMessage("§7API key: " + (api.hasKey() ? "§aconfigured" : "§cnot configured"));
                sendApiUrl(player);
            }
            default -> player.sendMessage("§7Usage: /api [generate|rotate|revoke|status]");
        }
        return true;
    }

    private void sendApiUrl(Player player) {
        String publicUrl = api.getPublicUrl();
        if (publicUrl.isBlank()) {
            player.sendMessage("§eAPI URL not configured. Set api.public-url to your HTTPS reverse-proxy URL in config.yml.");
            return;
        }
        sendCopyable(player, "API URL", publicUrl);
    }

    private void sendCopyable(Player player, String label, String value) {
        TextComponent copyable = new TextComponent("§7" + label + ": §e" + value + " §a[Click to copy]");
        copyable.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value));
        copyable.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new Text("Click to copy the " + label + ".")));
        player.spigot().sendMessage(copyable);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return List.of("generate", "rotate", "revoke", "status").stream()
                .filter(value -> value.startsWith(args[0].toLowerCase()))
                .toList();
    }
}
