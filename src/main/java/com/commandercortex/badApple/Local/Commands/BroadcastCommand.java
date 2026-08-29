package com.commandercortex.badApple.Local.Commands;

import com.commandercortex.badApple.Global.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Broadcasts a formatted server announcement and shows a title/particles to all online players.
 */
public class BroadcastCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("badapple.broadcast")) {
            sender.sendMessage("§cYou do not have permission to broadcast.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /broadcast <message>");
            return true;
        }

        String message = String.join(" ", args);
        String formatted = ColorUtil.FormatMessage("&7[&b&lBROADCAST&r&7] &r" + message);

        Bukkit.broadcastMessage(formatted);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§b§lSERVER ANNOUNCEMENT", "§7" + message, 10, 40, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.3F);
            spawnBurstParticles(player);
        }

        return true;
    }

    private void spawnBurstParticles(Player player) {
        Location center = player.getLocation().clone().add(0, 1.4, 0);
        for (int i = 0; i < 30; i++) {
            double angle = (Math.PI * 2 / 30) * i;
            double radius = 1.2 + ((i % 5) * 0.15);
            double x = center.getX() + Math.cos(angle) * radius;
            double y = center.getY() + 0.35 * Math.sin(i * 0.75);
            double z = center.getZ() + Math.sin(angle) * radius;
            player.getWorld().spawnParticle(Particle.END_ROD, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
