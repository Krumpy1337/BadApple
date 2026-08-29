package com.commandercortex.badApple.Local.Commands;

import com.commandercortex.badApple.BadApple;
import com.commandercortex.badApple.Local.Management.DeveloperDashboardServer;
import com.commandercortex.badApple.Local.Permissions.GroupManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.awt.Desktop;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Opens the developer dashboard server and attempts to open it in the local desktop browser for developers.
 */
public class ManageCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        GroupManager groupManager = new GroupManager();
        boolean isDeveloper = player.isOp() || player.hasPermission("badapple.manage") || "Developer".equalsIgnoreCase(groupManager.getGroup(player));
        if (!isDeveloper) {
            player.sendMessage("§cThis dashboard is restricted to developers.");
            return true;
        }

        BadApple plugin = BadApple.getPlugin(BadApple.class);
        DeveloperDashboardServer dashboard = plugin.getDeveloperDashboardServer();
        if (dashboard == null) {
            player.sendMessage("§cThe developer dashboard is not available.");
            return true;
        }

        dashboard.start();
        String url = dashboard.getUrl();
        player.sendMessage("§aOpening the developer dashboard at §f" + url); 

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                player.sendMessage("§7Open this manually: §f" + url);
            }
        } catch (Exception exception) {
            player.sendMessage("§7Open this manually: §f" + url);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
