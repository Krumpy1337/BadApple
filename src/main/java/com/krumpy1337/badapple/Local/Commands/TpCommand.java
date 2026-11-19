package com.krumpy1337.badapple.Local.Commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Teleport commands and GUI: /tp, /tpa, /tpaccept, /tpdeny with pending request management.
 */
public class TpCommand implements CommandExecutor, Listener, TabCompleter {
    private static final String TITLE = "§8Teleport Players";
    private static final Map<UUID, UUID> pendingRequests = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        String name = command.getName().toLowerCase();

        if ("tp".equals(name)) {
            if (!player.isOp() && !player.hasPermission("badapple.tp")) {
                player.sendMessage("§cStaff only: you do not have permission to use teleport.");
                return true;
            }
            if (args.length > 0) {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found or not online.");
                    return true;
                }
                player.teleport(target.getLocation());
                player.sendMessage("§aTeleported to §f" + target.getName() + "§a.");
                return true;
            }
            openMenu(player);
            return true;
        }

        if ("tpa".equals(name)) {
            if (args.length != 1) {
                player.sendMessage("§cUsage: /tpa <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found or not online.");
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage("§cYou cannot send a teleport request to yourself.");
                return true;
            }
            if (!target.isOp() && !target.hasPermission("badapple.tp")) {
                player.sendMessage("§cThat player is not available for teleport requests.");
                return true;
            }
            pendingRequests.put(target.getUniqueId(), player.getUniqueId());
            target.sendMessage("§a" + player.getName() + " wants to teleport to you. Type §f/tpaccept§a or §f/tpdeny§a.");
            player.sendMessage("§aTeleport request sent to §f" + target.getName() + "§a.");
            return true;
        }

        if ("tpaccept".equals(name)) {
            if (!player.isOp() && !player.hasPermission("badapple.tp")) {
                player.sendMessage("§cStaff only: you do not have permission to accept teleport requests.");
                return true;
            }
            if (!pendingRequests.containsKey(player.getUniqueId())) {
                player.sendMessage("§cYou have no pending teleport requests.");
                return true;
            }
            UUID requesterId = pendingRequests.remove(player.getUniqueId());
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester == null) {
                player.sendMessage("§cThe requesting player is no longer online.");
                return true;
            }
            requester.teleport(player.getLocation());
            requester.sendMessage("§aYour teleport request was accepted by §f" + player.getName() + "§a.");
            player.sendMessage("§aAccepted §f" + requester.getName() + "§a's teleport request.");
            return true;
        }

        if ("tpdeny".equals(name)) {
            if (!player.isOp() && !player.hasPermission("badapple.tp")) {
                player.sendMessage("§cStaff only: you do not have permission to deny teleport requests.");
                return true;
            }
            if (!pendingRequests.containsKey(player.getUniqueId())) {
                player.sendMessage("§cYou have no pending teleport requests.");
                return true;
            }
            UUID requesterId = pendingRequests.remove(player.getUniqueId());
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null) {
                requester.sendMessage("§cYour teleport request was denied by §f" + player.getName() + "§c.");
            }
            player.sendMessage("§cDenied the teleport request.");
            return true;
        }

        return false;
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new TpHolder(), 54, TITLE);
        fillInventoryWithGlass(inventory);

        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) {
                break;
            }

            List<String> lore = new ArrayList<>();
            lore.add("§7World: §f" + target.getWorld().getName());
            lore.add("§7Click to teleport to this player");
            inventory.setItem(slot, createPlayerHead(target.getName(), "§a" + target.getName(), lore));
            slot++;
        }

        if (slot == 0) {
            inventory.setItem(22, createItem(Material.BARRIER, "§cNo players online", Collections.singletonList("§7There are no players currently connected.")));
        }

        inventory.setItem(49, createItem(Material.ARROW, "§9Close", Collections.singletonList("§7Close this menu.")));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof TpHolder)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        String display = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "";
        if (display.contains("Close")) {
            player.closeInventory();
            return;
        }

        if (item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        String targetName = ChatColorStrip(display);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage("§cThat player is no longer online.");
            return;
        }

        player.teleport(target.getLocation());
        player.sendMessage("§aTeleported to §f" + target.getName() + "§a.");
        player.closeInventory();
    }

    private String ChatColorStrip(String value) {
        if (value == null) {
            return "";
        }
        return org.bukkit.ChatColor.stripColor(value).replace("§", "");
    }

    private ItemStack createPlayerHead(String ownerName, String title, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);
            skullMeta.setOwningPlayer(owner);
            skullMeta.setDisplayName(title);
            skullMeta.setLore(lore);
            skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(skullMeta);
            return item;
        }
        return createItem(Material.PLAYER_HEAD, title, lore);
    }

    private ItemStack createItem(Material material, String title, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillInventoryWithGlass(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<>()));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    matches.add(player.getName());
                }
            }
            return matches;
        }
        return new ArrayList<>();
    }

    private static class TpHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
