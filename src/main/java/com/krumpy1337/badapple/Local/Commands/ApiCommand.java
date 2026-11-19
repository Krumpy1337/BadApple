package com.krumpy1337.badapple.Local.Commands;

import com.krumpy1337.badapple.Local.Management.ServerApi;
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
                    player.sendMessage("§cThe API is disabled. Enable api.enabled in config.yml and restart.");
                    return true;
                }
                String key = api.generateKey();
                player.sendMessage("§aAPI key generated. Copy it now; it cannot be viewed again.");
                player.sendMessage("§e" + key);
                player.sendMessage("§7This replaces any existing API key.");
            }
            case "revoke" -> player.sendMessage(api.revokeKey()
                    ? "§aAPI key revoked."
                    : "§eThere is no API key to revoke.");
            case "status" -> {
                player.sendMessage("§7API listener: " + (api.isEnabled() ? "§a" + api.getAddress() : "§cdisabled"));
                player.sendMessage("§7API key: " + (api.hasKey() ? "§aconfigured" : "§cnot configured"));
            }
            default -> player.sendMessage("§7Usage: /api [generate|rotate|revoke|status]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return List.of("generate", "rotate", "revoke", "status").stream()
                .filter(value -> value.startsWith(args[0].toLowerCase()))
                .toList();
    }
}
