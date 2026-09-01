package com.commandercortex.badApple.Local.Players.Events;

import com.commandercortex.badApple.Local.Permissions.GroupManager;
import com.commandercortex.badApple.Local.Permissions.PermissionManager;
import com.commandercortex.badApple.Local.Permissions.VanishManager;
import com.commandercortex.badApple.Local.Players.Data.SetupPlayerFile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.util.Vector;
import com.commandercortex.badApple.BadApple;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * Listens for player joins/quits to initialize data, apply permissions, vanish state and update the tab list.
 */
public class onPlayerJoinEvent implements Listener {
    SetupPlayerFile setupPlayerFile = new SetupPlayerFile();
    GroupManager groupManager = new GroupManager();

    @EventHandler(priority = EventPriority.HIGHEST)
    public int onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();

        // Ensure no other plugin shows a join message after us by clearing at highest priority
        playerJoinEvent.setJoinMessage(null);

        setupPlayerFile.newPlayer(player);
        if (groupManager.getGroup(player) == null || groupManager.getGroup(player).trim().isEmpty()) {
            groupManager.setGroup(player, "Copper");
        }
        PermissionManager.applyGroupPermissions(player);
        VanishManager.refreshForJoin(player);
        groupManager.TabManager();

        // If player is in the lobby world, give server selector compass (only when velocity proxy is enabled and this server isn't 'smp')
        try {
            BadApple plugin = BadApple.getPlugin(BadApple.class);
            String serverName = plugin == null ? "" : plugin.getConfig().getString("server.name", "");
            boolean serverIsSmp = serverName != null && serverName.equalsIgnoreCase("smp");
            boolean velocityEnabled = plugin != null && plugin.isVelocityProxy();

            if (velocityEnabled && !serverIsSmp && player.getWorld() != null && "lobby".equalsIgnoreCase(player.getWorld().getName())) {
                boolean hasSelector = false;
                for (ItemStack it : player.getInventory().getContents()) {
                    if (it == null) continue;
                    if (it.hasItemMeta() && it.getItemMeta().hasDisplayName() && "§aServer Selector".equals(it.getItemMeta().getDisplayName())) {
                        hasSelector = true;
                        break;
                    }
                }
                if (!hasSelector) {
                    ItemStack compass = new ItemStack(Material.COMPASS);
                    ItemMeta meta = compass.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§aServer Selector");
                        meta.setLore(Arrays.asList("§7Right-click to choose a server"));
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                        compass.setItemMeta(meta);
                    }
                    // Place compass in middle hotbar slot (index 4) and make it unmovable via click handler
                    player.getInventory().setItem(4, compass);
                }

                    // If player is in lobby world, give cosmetics chest in hotbar slot 8
                    try {
                        if (player.getWorld() != null && "lobby".equalsIgnoreCase(player.getWorld().getName())) {
                            ItemStack cosmeticsItem = new ItemStack(Material.CHEST);
                            ItemMeta cm = cosmeticsItem.getItemMeta();
                            if (cm != null) {
                                cm.setDisplayName("§aCosmetics");
                                cm.setLore(Arrays.asList("§7Open cosmetics menu"));
                                cosmeticsItem.setItemMeta(cm);
                            }
                            ItemStack current = player.getInventory().getItem(8);
                            if (current == null || current.getType() == Material.AIR || !current.hasItemMeta() || !current.getItemMeta().hasDisplayName() || !"§aCosmetics".equals(current.getItemMeta().getDisplayName())) {
                                player.getInventory().setItem(8, cosmeticsItem);
                            }
                        }
                    } catch (Exception ignored) {}

                    // Enable allowFlight so PlayerToggleFlightEvent fires for double-space
                    try {
                        player.setAllowFlight(true);
                    } catch (Exception ignored) {}
            } else {
                    // Not in lobby or not velocity: ensure flight disabled
                    try {
                        player.setAllowFlight(false);
                    } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return 0;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        Player player = playerQuitEvent.getPlayer();
        PermissionManager.clearPermissions(player);
        playerQuitEvent.setQuitMessage(null);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Only handle right-click actions
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // Determine item in hand more reliably
        ItemStack item = event.getItem();
        if (item == null) {
            item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                item = player.getInventory().getItemInOffHand();
            }
        }

        if (item == null) return;

        // Cosmetics hotbar click: allow opening cosmetics when holding the cosmetics chest in lobby
        if (item.getType() == Material.CHEST && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && "§aCosmetics".equals(item.getItemMeta().getDisplayName())) {
            if (player.getWorld() != null && "lobby".equalsIgnoreCase(player.getWorld().getName())) {
                event.setCancelled(true);
                try {
                    new com.commandercortex.badApple.Local.Commands.CosmeticsCommand().openMenu(player);
                } catch (Exception ignored) {}
                return;
            }
        }

        if (item.getType() != Material.COMPASS) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!"§aServer Selector".equals(item.getItemMeta().getDisplayName())) return;

        // Only allow server selector in lobby world and when velocity integration enabled
        BadApple plugin = BadApple.getPlugin(BadApple.class);
        boolean velocityEnabled = plugin != null && plugin.isVelocityProxy();
        if (player.getWorld() == null || !"lobby".equalsIgnoreCase(player.getWorld().getName()) || !velocityEnabled) return;

        event.setCancelled(true);
        // Open the shared Server Selector GUI
        com.commandercortex.badApple.Local.GUI.ServerSelector.openSelector(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Prevent moving the Server Selector item in player's inventory
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                String d = clicked.getItemMeta().getDisplayName();
                if ("§aServer Selector".equals(d) || "§aCosmetics".equals(d)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (event.getView() == null || event.getView().getTitle() == null) return;
        if (!com.commandercortex.badApple.Local.GUI.ServerSelector.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        String name = clicked.getItemMeta().getDisplayName();
        if (name.contains("SMP")) {
            player.closeInventory();
            BadApple plugin = BadApple.getPlugin(BadApple.class);
            boolean velocityEnabled = plugin != null && plugin.isVelocityProxy();
            if (velocityEnabled && plugin != null) {
                // Try plugin messaging first
                if (sendConnect(plugin, player, "smp")) return;
            }
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("server smp"));
            } else {
                player.performCommand("server smp");
            }
        } else if (name.contains("Lobby")) {
            player.closeInventory();
            BadApple plugin = BadApple.getPlugin(BadApple.class);
            boolean velocityEnabled = plugin != null && plugin.isVelocityProxy();
            if (velocityEnabled && plugin != null) {
                if (sendConnect(plugin, player, "lobby")) return;
            }
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("server lobby"));
            } else {
                player.performCommand("server lobby");
            }
        } else if (name.contains("Cosmetics")) {
            player.closeInventory();
            try {
                new com.commandercortex.badApple.Local.Commands.CosmeticsCommand().openMenu(player);
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (p.getWorld() == null || !"lobby".equalsIgnoreCase(p.getWorld().getName())) return;
        // Only handle attempts to start flying (double-space triggers toggle)
        if (!event.isFlying()) return;
        // Cancel actual flight and propel forward
        event.setCancelled(true);
        p.setFlying(false);
        Vector dir = p.getLocation().getDirection().clone();
        dir.setY(0);
        if (dir.lengthSquared() == 0) {
            // If no direction, default forward along player's yaw
            dir = p.getLocation().getDirection().clone();
        }
        dir.normalize();
        // Launch forward ~5 blocks worth of velocity; tuned empirically
        Vector launch = dir.multiply(1.6);
        launch.setY(0.5);
        p.setVelocity(launch);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        try {
            if (p.getWorld() != null && "lobby".equalsIgnoreCase(p.getWorld().getName())) {
                p.setAllowFlight(true);
            } else {
                p.setAllowFlight(false);
            }
        } catch (Exception ignored) {}
    }

    private boolean sendConnect(BadApple plugin, Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            return true;
        } catch (Exception e) {
            // fail silently, caller will fallback to /server command
            return false;
        }
    }
}
