package com.krumpy1337.badapple.Local.GUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ServerSelector {
    public static final String TITLE = "Server Selector";

    public static void openSelector(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);

        ItemStack smp = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta sm = smp.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§aSMP");
            sm.setLore(Arrays.asList("§7Connect to the SMP server"));
            smp.setItemMeta(sm);
        }

        ItemStack lobby = new ItemStack(Material.BEACON);
        ItemMeta lm = lobby.getItemMeta();
        if (lm != null) {
            lm.setDisplayName("§aLobby");
            lm.setLore(Arrays.asList("§7Return to lobby"));
            lobby.setItemMeta(lm);
        }

        inv.setItem(3, smp);
        inv.setItem(5, lobby);
        player.openInventory(inv);
    }
}
