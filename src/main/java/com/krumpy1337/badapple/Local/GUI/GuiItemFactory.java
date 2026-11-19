package com.krumpy1337.badapple.Local.GUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public final class GuiItemFactory {
    private GuiItemFactory() {}

    public static void fillWithGlass(Inventory inventory, Material material) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, createGlassPane(material));
        }
    }

    public static ItemStack createGlassPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createItem(Material material, String title, List<String> lore) {
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

    public static ItemStack createPlayerHead(String ownerName, String title, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
            skullMeta.setDisplayName(title);
            skullMeta.setLore(lore);
            skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(skullMeta);
            return item;
        }
        return createItem(Material.PLAYER_HEAD, title, lore);
    }
}
