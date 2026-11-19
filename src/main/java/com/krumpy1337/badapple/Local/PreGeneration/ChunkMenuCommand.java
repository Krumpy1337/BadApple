package com.krumpy1337.badapple.Local.PreGeneration;

import com.krumpy1337.badapple.BadApple;
import com.krumpy1337.badapple.Local.GUI.GuiItemFactory;
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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChunkMenuCommand implements CommandExecutor, Listener, TabCompleter {
    private static final String TITLE = "§8Chunk Pre-Generation";
    private final BadApple plugin;
    private final ChunkPreGenerationManager manager;
    private final Map<UUID, Integer> pendingRadius = new HashMap<>();

    public ChunkMenuCommand(BadApple plugin, ChunkPreGenerationManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.isOp() && !player.hasPermission("badapple.chunkpregeneration")) {
            player.sendMessage("§cYou do not have permission to manage chunk pre-generation.");
            return true;
        }
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "status" -> player.sendMessage(statusLine());
                case "pause" -> { manager.pause(); player.sendMessage("§eChunk pre-generation paused."); }
                case "resume" -> { manager.resume(); player.sendMessage("§aChunk pre-generation resumed."); }
                case "cancel" -> { manager.cancel(); player.sendMessage("§cChunk pre-generation cancelled."); }
                default -> player.sendMessage("§cUsage: /chunkpregeneration <status|pause|resume|cancel>");
            }
            return true;
        }
        openMenu(player);
        return true;
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new ChunkMenuHolder(), 54, TITLE);
        GuiItemFactory.fillWithGlass(inventory, Material.GRAY_STAINED_GLASS_PANE);

        inventory.setItem(10, GuiItemFactory.createItem(Material.COMPASS, "§bSet Radius",
                List.of("§7Current radius: §f" + manager.getRadius(), "§eClick, then enter a radius in chat.")));
        inventory.setItem(12, GuiItemFactory.createItem(Material.EMERALD, "§aStart from My Chunk",
                List.of("§7World: §f" + player.getWorld().getName(), "§7Radius: §f" + manager.getRadius(), "§7Uses 1-based chunk radius.")));
        inventory.setItem(14, GuiItemFactory.createItem(manager.isRunning() ? Material.LIME_DYE : Material.GRAY_DYE,
                manager.isRunning() ? "§aPause" : "§eResume", manager.isRunning()
                        ? List.of("§7Pause the active pre-generation task.")
                        : List.of("§7Resume the saved pre-generation task.")));
        inventory.setItem(16, GuiItemFactory.createItem(Material.BARRIER, "§cCancel Task", Collections.singletonList("§7Stop and clear the current task.")));

        boolean keepLoaded = manager.isKeepLoaded();
        inventory.setItem(29, GuiItemFactory.createItem(keepLoaded ? Material.MAGMA_CREAM : Material.BUCKET,
                keepLoaded ? "§6Keep Chunks Loaded" : "§9Unload Generated Chunks",
                List.of("§7Current mode:", keepLoaded ? "§fKeep loaded in server memory" : "§fUnload after processing and use normal storage caching",
                        "§eClick to toggle.")));

        inventory.setItem(31, GuiItemFactory.createItem(Material.PAPER, "§bTask Status",
                statusLore()));

        inventory.setItem(49, GuiItemFactory.createItem(Material.ARROW, "§9Close", Collections.singletonList("§7Close the menu.")));
        player.openInventory(inventory);
    }

    private String statusLine() {
        if (!manager.hasTask()) return "§7No chunk pre-generation task is active.";
        return "§7" + manager.getWorldName() + " §f" + manager.getProgress() + "/" + manager.getTotal()
                + " (§b" + String.format(java.util.Locale.US, "%.2f", manager.getPercentComplete()) + "%§f)"
                + (manager.isRunning() ? " §aRunning" : manager.isPaused() ? " §ePaused" : "");
    }

    private List<String> statusLore() {
        if (!manager.hasTask()) return List.of("§7No active pre-generation task.");
        List<String> lore = new ArrayList<>();
        lore.add("§7World: §f" + manager.getWorldName());
        lore.add("§7Center: §f" + manager.getCenterX() + " / " + manager.getCenterZ());
        lore.add("§7Radius: §f" + manager.getRadius());
        lore.add("§7Progress: §f" + manager.getProgress() + " / " + manager.getTotal());
        lore.add("§7Completion: §f" + String.format(java.util.Locale.US, "%.2f", manager.getPercentComplete()) + "%");
        lore.add(manager.isRunning() ? "§aRunning" : manager.isPaused() ? "§ePaused" : "§7Idle");
        return lore;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!pendingRadius.containsKey(player.getUniqueId())) return;
        event.setCancelled(true);
        pendingRadius.remove(player.getUniqueId());

        try {
            int radius = Integer.parseInt(event.getMessage().trim());
            int max = plugin.getConfig().getInt("pregeneration.max-radius", 2048);
            if (radius < 1) {
                player.sendMessage("§cRadius must be at least 1.");
                return;
            }
            if (radius > max) {
                player.sendMessage("§cMaximum radius is " + max + ".");
                return;
            }
            manager.setRadius(radius);
            player.sendMessage("§aChunk radius set to " + radius + ".");
            Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
        } catch (NumberFormatException exception) {
            player.sendMessage("§cEnter a whole number for the chunk radius.");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof ChunkMenuHolder)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        String name = event.getCurrentItem().getItemMeta().getDisplayName();
        switch (name) {
            case "§bSet Radius" -> {
                pendingRadius.put(player.getUniqueId(), manager.getRadius());
                player.closeInventory();
                player.sendMessage("§eType the chunk radius in chat. Use 1 for a 3x3 chunk area.");
            }
            case "§aStart from My Chunk" -> {
                int radius = manager.getRadius();
                if (radius < 1) radius = plugin.getConfig().getInt("pregeneration.default-radius", 8);
                manager.start(player.getWorld(), player.getLocation().getBlockX() >> 4, player.getLocation().getBlockZ() >> 4, radius);
                player.sendMessage("§aChunk pre-generation started.");
                openMenu(player);
            }
            case "§aPause" -> { manager.pause(); player.sendMessage("§eChunk pre-generation paused."); openMenu(player); }
            case "§eResume" -> { manager.resume(); player.sendMessage("§aChunk pre-generation resumed."); openMenu(player); }
            case "§cCancel Task" -> { manager.cancel(); player.sendMessage("§cChunk pre-generation cancelled."); openMenu(player); }
            case "§6Keep Chunks Loaded", "§9Unload Generated Chunks" -> {
                manager.setKeepLoaded(!manager.isKeepLoaded());
                openMenu(player);
            }
            case "§9Close" -> player.closeInventory();
            default -> { }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("status", "pause", "resume", "cancel");
        return Collections.emptyList();
    }

    private static final class ChunkMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
}
