package com.krumpy1337.badapple.Local.Commands;

import com.krumpy1337.badapple.BadApple;
import com.krumpy1337.badapple.Local.Permissions.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides a GUI for players to view, enable, and manage cosmetics (trails, particles, wings).
 */
public class CosmeticsCommand implements CommandExecutor, Listener, TabCompleter {
    private static final String TITLE = "§8Cosmetics";
    private static final String COSMETIC_KEY = "badapple.cosmetic.key";
    private final GroupManager groupManager = new GroupManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("badapple.cosmetics")) {
            // Allow players in the lobby world to open cosmetics even without permission
            if (player.getWorld() == null || !"lobby".equalsIgnoreCase(player.getWorld().getName())) {
                player.sendMessage("§cYou do not have permission to manage cosmetics.");
                return true;
            }
        }

        openMenu(player);
        return true;
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new CosmeticsHolder("MENU"), 54, TITLE);
        fillInventoryWithGlass(inventory);

        List<String> unlocked = groupManager.getPlayerCosmetics(player);
        if (unlocked.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, "§cNo cosmetics unlocked", Collections.singletonList("§7Your rank has no cosmetic unlocks yet.")));
            player.openInventory(inventory);
            return;
        }

        List<String> trails = new ArrayList<>();
        List<String> particles = new ArrayList<>();
        List<String> wings = new ArrayList<>();
        for (String cosmetic : unlocked) {
            if (cosmetic == null || cosmetic.trim().isEmpty()) {
                continue;
            }
            String label = cosmetic.contains(":") ? cosmetic.substring(0, cosmetic.indexOf(':')).trim().toLowerCase() : cosmetic.toLowerCase();
            if ("trail".equals(label)) {
                trails.add(cosmetic);
            } else if ("particle".equals(label)) {
                particles.add(cosmetic);
            } else if ("wing".equals(label)) {
                wings.add(cosmetic);
            }
        }

        inventory.setItem(18, createItem(Material.BLAZE_POWDER, "§6Trails", Collections.singletonList("§7Open trail cosmetics (" + trails.size() + ")")));
        inventory.setItem(21, createItem(Material.FIREWORK_ROCKET, "§eParticles", Collections.singletonList("§7Open particle cosmetics (" + particles.size() + ")")));
        inventory.setItem(24, createItem(Material.PHANTOM_MEMBRANE, "§dWings", Collections.singletonList("§7Open wing cosmetics (" + wings.size() + ")")));
        inventory.setItem(26, createItem(Material.BARRIER, "§cClear Active", Collections.singletonList("§7Disable the current cosmetic.")));
        inventory.setItem(49, createItem(Material.ARROW, "§9Close", Collections.singletonList("§7Close the menu.")));

        if (!groupManager.getEnabledCosmetics(player).isEmpty()) {
            String active = groupManager.getEnabledCosmetics(player).get(0);
            inventory.setItem(40, createItem(Material.LIME_DYE, "§aActive", Collections.singletonList("§7Currently enabled: §f" + active)));
        }

        player.openInventory(inventory);
    }

    private void openCategoryMenu(Player player, String category) {
        List<String> unlocked = groupManager.getPlayerCosmetics(player);
        List<String> matches = new ArrayList<>();
        for (String cosmetic : unlocked) {
            if (cosmetic != null && cosmetic.toLowerCase().startsWith(category.toLowerCase() + ":")) {
                matches.add(cosmetic);
            }
        }

        Inventory inventory = Bukkit.createInventory(new CosmeticsHolder(category.toUpperCase() + "_MENU"), 54, "§8" + capitalize(category) + "s");
        fillInventoryWithGlass(inventory);

        if (matches.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, "§cNo unlocks", Collections.singletonList("§7This category has no cosmetics available.")));
        } else {
            int slot = 0;
            for (String cosmetic : matches) {
                if (slot >= 45) {
                    break;
                }
                boolean enabled = groupManager.isCosmeticEnabled(player, cosmetic);
                String displayName = prettyCosmeticName(cosmetic);
                String title = enabled ? "§a" + displayName : "§c" + displayName;
                ItemStack cosmeticItem = createItem(Material.EMERALD, title, new String[] {
                        "§7Key: §f" + cosmetic,
                        enabled ? "§7Status: §aEnabled" : "§7Status: §cDisabled",
                        "§7Click to activate this cosmetic"
                });
                ItemMeta meta = cosmeticItem.getItemMeta();
                if (meta != null) {
                    meta.setLocalizedName(cosmetic);
                    meta.getPersistentDataContainer().set(new NamespacedKey(BadApple.getPlugin(BadApple.class), COSMETIC_KEY), PersistentDataType.STRING, cosmetic);
                    cosmeticItem.setItemMeta(meta);
                }
                inventory.setItem(slot, cosmeticItem);
                slot++;
            }
        }

        inventory.setItem(49, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to the cosmetics menu.")));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null || !(topInventory.getHolder() instanceof CosmeticsHolder)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        CosmeticsHolder holder = (CosmeticsHolder) topInventory.getHolder();
        String title = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "";
        String displayName = ChatColor.stripColor(title);

        if (displayName.contains("Back")) {
            openMenu(player);
            return;
        }
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        if (displayName.contains("Clear Active")) {
            groupManager.setEnabledCosmetics(player, new ArrayList<>());
            player.sendMessage("§aCleared the active cosmetic.");
            openMenu(player);
            return;
        }
        if (displayName.contains("Trails")) {
            openCategoryMenu(player, "trail");
            return;
        }
        if (displayName.contains("Particles")) {
            openCategoryMenu(player, "particle");
            return;
        }
        if (displayName.contains("Wings")) {
            openCategoryMenu(player, "wing");
            return;
        }

        if (holder == null || !("TRAIL_MENU".equals(holder.mode) || "PARTICLE_MENU".equals(holder.mode) || "WING_MENU".equals(holder.mode))) {
            return;
        }

        String cosmetic = null;
        if (item.hasItemMeta() && item.getItemMeta() != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(new NamespacedKey(BadApple.getPlugin(BadApple.class), COSMETIC_KEY), PersistentDataType.STRING)) {
                cosmetic = meta.getPersistentDataContainer().get(new NamespacedKey(BadApple.getPlugin(BadApple.class), COSMETIC_KEY), PersistentDataType.STRING);
            } else if (meta.getLocalizedName() != null && !meta.getLocalizedName().isEmpty()) {
                cosmetic = meta.getLocalizedName();
            }
        }
        if (cosmetic == null || cosmetic.isEmpty()) {
            cosmetic = resolveCosmeticKey(item, displayName);
        }
        if (cosmetic == null || cosmetic.isEmpty()) {
            return;
        }

        List<String> unlocked = groupManager.getPlayerCosmetics(player);
        String matched = null;
        for (String unlockedCosmetic : unlocked) {
            if (unlockedCosmetic != null && unlockedCosmetic.equalsIgnoreCase(cosmetic)) {
                matched = unlockedCosmetic;
                break;
            }
        }
        if (matched == null) {
            for (String unlockedCosmetic : unlocked) {
                if (unlockedCosmetic != null && unlockedCosmetic.equalsIgnoreCase(cosmetic.replace(" ", ""))) {
                    matched = unlockedCosmetic;
                    break;
                }
            }
        }
        if (matched == null) {
            player.sendMessage("§cThat cosmetic is not available to you.");
            return;
        }

        boolean enabled = groupManager.isCosmeticEnabled(player, matched);
        groupManager.setEnabledCosmetics(player, enabled ? new ArrayList<>() : java.util.Collections.singletonList(matched));
        player.sendMessage(enabled ? "§cDisabled cosmetic '" + matched + "'." : "§aEnabled cosmetic '" + matched + "'.");
        String category = matched.contains(":") ? matched.substring(0, matched.indexOf(':')).trim().toLowerCase() : "";
        if (!category.isEmpty()) {
            openCategoryMenu(player, category);
        }
    }

    private String resolveCosmeticKey(ItemStack item, String fallbackTitle) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null) {
            return parseFallbackCosmeticKey(fallbackTitle);
        }

        if (item.getItemMeta().getLore() != null) {
            for (String loreLine : item.getItemMeta().getLore()) {
                if (loreLine != null && loreLine.contains("Key:")) {
                    String keyPart = loreLine.substring(loreLine.indexOf("Key:") + 4).trim();
                    keyPart = ChatColor.stripColor(keyPart);
                    if (!keyPart.isEmpty()) {
                        return keyPart;
                    }
                }
            }
        }

        return parseFallbackCosmeticKey(fallbackTitle);
    }

    private String parseFallbackCosmeticKey(String fallbackTitle) {
        if (fallbackTitle == null || fallbackTitle.trim().isEmpty()) {
            return null;
        }

        String normalized = ChatColor.stripColor(fallbackTitle)
                .replace("Trail: ", "trail: ")
                .replace("Particle: ", "particle: ")
                .replace("Wing: ", "wing: ")
                .replace("Trails: ", "trail: ")
                .replace("Particles: ", "particle: ")
                .replace("Wings: ", "wing: ");

        if (normalized.contains(":")) {
            String[] split = normalized.split(":", 2);
            if (split.length == 2) {
                return split[0].trim().toLowerCase() + ": " + split[1].trim();
            }
        }
        return null;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private String prettyCosmeticName(String cosmetic) {
        if (cosmetic == null || cosmetic.trim().isEmpty()) {
            return "Unknown";
        }
        String raw = cosmetic.trim();
        if (raw.contains(":")) {
            raw = raw.substring(raw.indexOf(':') + 1).trim();
        }
        String[] parts = raw.split("[_-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return builder.length() == 0 ? raw : builder.toString();
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

    private ItemStack createItem(Material material, String title, String[] lore) {
        return createItem(material, title, java.util.Arrays.asList(lore));
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    private static class CosmeticsHolder implements InventoryHolder {
        private final String mode;

        private CosmeticsHolder(String mode) {
            this.mode = mode;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

