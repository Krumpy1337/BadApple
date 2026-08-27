package com.commandercortex.badApple.Local.Commands;

import com.commandercortex.badApple.BadApple;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PunishMenuCommand implements CommandExecutor, Listener, TabCompleter {
    private static final String MAIN_TITLE = "§8Punish Menu";
    private static final String ACTION_TITLE = "§8Punish Actions";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("badapple.punish")) {
            player.sendMessage("§cYou do not have permission to punish players.");
            return true;
        }

        openPlayerMenu(player);
        return true;
    }

    public void openPlayerMenu(Player player) {
        openPlayerMenu(player, null);
    }

    public void openPlayerMenu(Player player, String selectedTarget) {
        Inventory inventory = Bukkit.createInventory(new PunishMenuHolder(selectedTarget), 54, MAIN_TITLE);
        fillInventoryWithGlass(inventory);
        int slot = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 54) {
                break;
            }

            List<String> lore = new ArrayList<>();
            lore.add("§7Click to punish this player");
            inventory.setItem(slot, createPlayerHead(target.getName(), "§a" + target.getName(), lore));
            slot++;
        }

        if (slot == 0) {
            inventory.setItem(22, createItem(Material.BARRIER, "§cNo players online", Arrays.asList("§7There are no players connected.")));
        }

        player.openInventory(inventory);
    }

    private void openActionMenu(Player player, String targetName) {
        Inventory inventory = Bukkit.createInventory(new PunishMenuHolder(targetName), 27, ACTION_TITLE);
        fillInventoryWithGlass(inventory);

        inventory.setItem(10, createItem(Material.REDSTONE, "§cKick", Arrays.asList("§7Kick the player from the server.")));
        inventory.setItem(12, createItem(Material.IRON_AXE, "§4Ban", Arrays.asList("§7Permanently ban the player.")));
        inventory.setItem(14, createItem(Material.BOOK, "§eMute", Arrays.asList("§7Mute the player from chat.")));
        inventory.setItem(16, createItem(Material.PAPER, "§aUnmute", Arrays.asList("§7Unmute the player.")));
        inventory.setItem(18, createItem(Material.NAME_TAG, "§bGroup Settings", Arrays.asList("§7Open this player's group tools.")));
        inventory.setItem(20, createItem(Material.PLAYER_HEAD, "§aPlayers Menu", Arrays.asList("§7Open the player management menu.")));
        inventory.setItem(22, createItem(Material.ARROW, "§9Back", Arrays.asList("§7Return to player list.")));

        inventory.setItem(4, createPlayerHead(targetName, "§bTarget", Arrays.asList("§7" + targetName)));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof PunishMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        PunishMenuHolder holder = (PunishMenuHolder) event.getInventory().getHolder();
        String targetName = holder.getTargetName();

        String title = event.getView().getTitle();
        String displayName = current.getItemMeta() != null ? current.getItemMeta().getDisplayName() : "";

        if (title.equals(MAIN_TITLE)) {
            if (displayName.startsWith("§a")) {
                String selected = displayName.replace("§a", "");
                Player target = Bukkit.getPlayerExact(selected);
                if (target != null) {
                    openActionMenu(player, selected);
                }
            }
            return;
        }

        if (title.equals(ACTION_TITLE)) {
            if (displayName.contains("Back")) {
                openPlayerMenu(player);
                return;
            }

            if (displayName.contains("Group Settings")) {
                new GroupMenuCommand().openPlayerActionMenu(player, targetName);
                return;
            }

            if (displayName.contains("Players Menu")) {
                new GroupMenuCommand().openPlayerManagementMenu(player);
                return;
            }

            if (targetName == null || targetName.isEmpty()) {
                return;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                player.sendMessage("§cPlayer is no longer online.");
                openPlayerMenu(player);
                return;
            }

            if (displayName.contains("Kick")) {
                target.kickPlayer("§cYou were kicked by §e" + player.getName());
                player.sendMessage("§aKicked " + target.getName() + ".");
                return;
            }

            if (displayName.contains("Ban")) {
                Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), "§cBanned by " + player.getName(), null, player.getName());
                target.kickPlayer("§cYou were banned by §e" + player.getName());
                player.sendMessage("§aBanned " + target.getName() + ".");
                return;
            }

            if (displayName.contains("Mute")) {
                target.setMetadata("muted", new FixedMetadataValue(BadApple.getPlugin(BadApple.class), true));
                target.sendMessage("§cYou have been muted by §e" + player.getName() + "§c.");
                player.sendMessage("§aMuted " + target.getName() + ".");
                return;
            }

            if (displayName.contains("Unmute")) {
                target.removeMetadata("muted", BadApple.getPlugin(BadApple.class));
                target.sendMessage("§aYou have been unmuted by §e" + player.getName() + "§a.");
                player.sendMessage("§aUnmuted " + target.getName() + ".");
                return;
            }
        }
    }

    private void fillInventoryWithGlass(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane());
            }
        }
    }

    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String title, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(title);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHead(String ownerName, String title, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
            skullMeta.setDisplayName(title);
            skullMeta.setLore(lore);
            skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(skullMeta);
            return item;
        }
        return createItem(Material.PLAYER_HEAD, title, lore);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    private static class PunishMenuHolder implements InventoryHolder {
        private final String targetName;

        public PunishMenuHolder(String targetName) {
            this.targetName = targetName;
        }

        public String getTargetName() {
            return targetName;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
