package com.commandercortex.badApple.Local.Commands;

import com.commandercortex.badApple.BadApple;
import com.commandercortex.badApple.Local.Permissions.GroupConfig;
import com.commandercortex.badApple.Local.Permissions.GroupManager;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupMenuCommand implements CommandExecutor, Listener, TabCompleter {
    private static final String MAIN_TITLE = "§8Group Manager";
    private static final String GROUP_ACTION_TITLE = "§8Group Actions";
    private static final String PERMISSION_TITLE = "§8Permissions";
    private static final String PLAYER_TITLE = "§8Group Players";
    private static final Material FILLER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final List<String> KNOWN_PERMISSIONS = Arrays.asList(
            "badapple.group",
            "badapple.vanish",
            "badapple.punish",
            "essentials.kick",
            "essentials.ban",
            "essentials.mute",
            "essentials.tp",
            "essentials.fly",
            "essentials.god",
            "minecraft.command.op"
    );

    private final GroupManager groupManager = new GroupManager();
    private final Map<UUID, String> selectedGroup = new HashMap<>();
    private static final Map<UUID, PendingInput> pendingInput = new HashMap<>();

    public static boolean hasPendingInput(Player player) {
        return player != null && pendingInput.containsKey(player.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("badapple.group")) {
            player.sendMessage("§cYou do not have permission to manage groups.");
            return true;
        }

        openMainMenu(player);
        return true;
    }

    private void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder("", "MAIN"), 54, MAIN_TITLE);
        fillInventoryWithGlass(inventory);
        inventory.setItem(0, createItem(Material.GREEN_WOOL, "§aCreate Group", Collections.singletonList("§7Type a name in chat.")));
        inventory.setItem(1, createItem(Material.ANVIL, "§eRename Selected", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(2, createItem(Material.NAME_TAG, "§6Set Prefix", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(3, createItem(Material.BOOK, "§dPermissions", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(4, createItem(Material.PLAYER_HEAD, "§bAssign User", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(5, createItem(Material.SKELETON_SKULL, "§cRemove User", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(6, createItem(Material.RED_WOOL, "§4Delete Group", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(8, createItem(Material.CLOCK, "§9Refresh", Collections.singletonList("§7Refreshes the menu.")));

        String selected = selectedGroup.get(player.getUniqueId());
        if (selected != null) {
            inventory.setItem(7, createItem(Material.NAME_TAG, "§aSelected: " + selected, Arrays.asList(
                    "§7Prefix: §r" + GroupConfig.getGroupPrefix(selected),
                    "§7Parent: §r" + (groupManager.getParentGroup(selected).isEmpty() ? "none" : groupManager.getParentGroup(selected))
            )));
        }

        int slot = 9;
        List<String> groups = groupManager.getGroups();
        if (groups.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, "§cNo groups yet", Collections.singletonList("§7Use the create button.")));
        } else {
            for (String group : groups) {
                if (slot >= 45) {
                    break;
                }
                List<String> lore = new ArrayList<>();
                lore.add("§7Prefix: §r" + GroupConfig.getGroupPrefix(group));
                lore.add("§7Parent: §r" + (groupManager.getParentGroup(group).isEmpty() ? "none" : groupManager.getParentGroup(group)));
                lore.add("§7Permissions: §r" + groupManager.getPermissions(group).size());
                lore.add("§7Click to select");
                inventory.setItem(slot, createItem(Material.NAME_TAG, "§a" + group, lore));
                slot++;
            }
        }

        player.openInventory(inventory);
    }

    private void openGroupActionMenu(Player player, String group) {
        selectedGroup.put(player.getUniqueId(), group);
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(group, "GROUP_ACTION"), 54, GROUP_ACTION_TITLE);
        fillInventoryWithGlass(inventory);
        inventory.setItem(0, createItem(Material.ANVIL, "§eRename Group", Collections.singletonList("§7Type a new group name in chat.")));
        inventory.setItem(1, createItem(Material.NAME_TAG, "§6Set Prefix", Collections.singletonList("§7Type a new prefix in chat.")));
        inventory.setItem(2, createItem(Material.BOOK, "§dToggle Permissions", Collections.singletonList("§7Open permission menu.")));
        inventory.setItem(3, createItem(Material.PLAYER_HEAD, "§bAssign User", Collections.singletonList("§7Select a player to assign.")));
        inventory.setItem(4, createItem(Material.SKELETON_SKULL, "§cRemove User", Collections.singletonList("§7Select a player to remove.")));
        inventory.setItem(5, createItem(Material.RED_WOOL, "§4Delete Group", Collections.singletonList("§7Delete this group.")));
        inventory.setItem(8, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to group list.")));
        inventory.setItem(22, createItem(Material.NAME_TAG, "§aSelected Group", Arrays.asList("§7" + group, "§7Prefix: §r" + GroupConfig.getGroupPrefix(group))));
        player.openInventory(inventory);
    }

    private void openPermissionMenu(Player player, String group) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(group, "PERMISSIONS"), 54, PERMISSION_TITLE);
        fillInventoryWithGlass(inventory);
        int slot = 0;
        for (String permission : KNOWN_PERMISSIONS) {
            if (slot >= 45) {
                break;
            }
            boolean enabled = groupManager.getPermissions(group).contains(permission);
            Material material = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
            String title = enabled ? "§a" + permission : "§c" + permission;
            inventory.setItem(slot, createItem(material, title, Collections.singletonList("§7Click to toggle.")));
            slot++;
        }
        inventory.setItem(49, createItem(Material.BOOK, "§eCustom Node", Collections.singletonList("§7Type a custom permission in chat.")));
        inventory.setItem(53, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to group actions.")));
        player.openInventory(inventory);
    }

    private void openPlayerSelectionMenu(Player player, String group, boolean removeMode) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(group, removeMode ? "REMOVE_PLAYER" : "ADD_PLAYER"), 54, PLAYER_TITLE);
        fillInventoryWithGlass(inventory);
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot, createItem(Material.PLAYER_HEAD, "§a" + online.getName(), Arrays.asList(
                    "§7Click to " + (removeMode ? "remove" : "assign") + " this player",
                    "§7Current group: §r" + (groupManager.getGroup(online).isEmpty() ? "none" : groupManager.getGroup(online))
            )));
            slot++;
        }
        inventory.setItem(49, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to group actions.")));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PendingInput input = pendingInput.remove(uuid);
        if (input == null) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(BadApple.getPlugin(BadApple.class), () -> player.sendMessage("§cCancelled."));
            return;
        }

        String groupName = input.groupName;
        Bukkit.getScheduler().runTask(BadApple.getPlugin(BadApple.class), () -> {
            switch (input.type) {
                case CREATE_GROUP:
                    if (groupManager.groupExists(message)) {
                        player.sendMessage("§cGroup '" + message + "' already exists.");
                        return;
                    }
                    groupManager.createGroup(message);
                    selectedGroup.put(uuid, message);
                    player.sendMessage("§aCreated group '" + message + "'.");
                    openGroupActionMenu(player, message);
                    break;
                case RENAME_GROUP:
                    if (groupName == null || groupName.isEmpty()) {
                        player.sendMessage("§cNo group selected.");
                        return;
                    }
                    if (groupManager.groupExists(message)) {
                        player.sendMessage("§cGroup '" + message + "' already exists.");
                        return;
                    }
                    groupManager.renameGroup(groupName, message);
                    selectedGroup.put(uuid, message);
                    player.sendMessage("§aRenamed group to '" + message + "'.");
                    openGroupActionMenu(player, message);
                    break;
                case SET_PREFIX:
                    if (groupName == null || groupName.isEmpty()) {
                        player.sendMessage("§cNo group selected.");
                        return;
                    }
                    groupManager.setGroupPrefix(groupName, message);
                    groupManager.TabManager();
                    player.sendMessage("§aUpdated prefix for '" + groupName + "'.");
                    openGroupActionMenu(player, groupName);
                    break;
                case CUSTOM_PERMISSION:
                    if (groupName == null || groupName.isEmpty()) {
                        player.sendMessage("§cNo group selected.");
                        return;
                    }
                    groupManager.addPermission(groupName, message);
                    player.sendMessage("§aAdded permission '" + message + "' to group '" + groupName + "'.");
                    openPermissionMenu(player, groupName);
                    break;
                default:
                    break;
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof GroupMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        GroupMenuHolder holder = (GroupMenuHolder) event.getInventory().getHolder();
        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "";

        if (event.getView().getTitle().equals(MAIN_TITLE)) {
            if (displayName.contains("Create Group")) {
                pendingInput.put(player.getUniqueId(), new PendingInput(null, PendingAction.CREATE_GROUP));
                player.closeInventory();
                player.sendMessage("§aType the new group name in chat. Type §c'cancel'§a to cancel.");
                return;
            }
            if (displayName.contains("Refresh")) {
                openMainMenu(player);
                return;
            }
            if (displayName.startsWith("§a") && !displayName.startsWith("§aSelected:")) {
                String groupName = displayName.replace("§a", "");
                selectedGroup.put(player.getUniqueId(), groupName);
                openGroupActionMenu(player, groupName);
                return;
            }
            if (displayName.startsWith("§aSelected:")) {
                String selected = selectedGroup.get(player.getUniqueId());
                if (selected != null) {
                    openGroupActionMenu(player, selected);
                }
            }
            return;
        }

        if (event.getView().getTitle().equals(GROUP_ACTION_TITLE)) {
            String groupName = holder.groupName;
            if (displayName.contains("Back")) {
                openMainMenu(player);
                return;
            }
            if (displayName.contains("Rename Group")) {
                pendingInput.put(player.getUniqueId(), new PendingInput(groupName, PendingAction.RENAME_GROUP));
                player.closeInventory();
                player.sendMessage("§aType the new group name in chat. Type §c'cancel'§a to cancel.");
                return;
            }
            if (displayName.contains("Set Prefix")) {
                pendingInput.put(player.getUniqueId(), new PendingInput(groupName, PendingAction.SET_PREFIX));
                player.closeInventory();
                player.sendMessage("§aType the new prefix in chat. Example: §7[Admin] §r. Type §c'cancel'§a to cancel.");
                return;
            }
            if (displayName.contains("Toggle Permissions")) {
                openPermissionMenu(player, groupName);
                return;
            }
            if (displayName.contains("Assign User")) {
                openPlayerSelectionMenu(player, groupName, false);
                return;
            }
            if (displayName.contains("Remove User")) {
                openPlayerSelectionMenu(player, groupName, true);
                return;
            }
            if (displayName.contains("Delete Group")) {
                groupManager.deleteGroup(groupName);
                selectedGroup.remove(player.getUniqueId());
                player.sendMessage("§aDeleted group '" + groupName + "'.");
                openMainMenu(player);
                return;
            }
            return;
        }

        if (event.getView().getTitle().equals(PERMISSION_TITLE)) {
            String groupName = holder.groupName;
            if (displayName.contains("Back")) {
                openGroupActionMenu(player, groupName);
                return;
            }
            if (displayName.contains("Custom Node")) {
                pendingInput.put(player.getUniqueId(), new PendingInput(groupName, PendingAction.CUSTOM_PERMISSION));
                player.closeInventory();
                player.sendMessage("§aType a custom permission node in chat. Type §c'cancel'§a to cancel.");
                return;
            }
            String permission = displayName.replace("§a", "").replace("§c", "");
            if (groupManager.getPermissions(groupName).contains(permission)) {
                groupManager.removePermission(groupName, permission);
            } else {
                groupManager.addPermission(groupName, permission);
            }
            groupManager.TabManager();
            openPermissionMenu(player, groupName);
            return;
        }

        if (event.getView().getTitle().equals(PLAYER_TITLE)) {
            String groupName = holder.groupName;
            if (displayName.contains("Back")) {
                openGroupActionMenu(player, groupName);
                return;
            }
            Player target = Bukkit.getPlayerExact(displayName.replace("§a", ""));
            if (target != null) {
                if ("REMOVE_PLAYER".equals(holder.mode)) {
                    groupManager.setGroup(target, "");
                    player.sendMessage("§aRemoved '" + target.getName() + "' from their group.");
                } else {
                    groupManager.setGroup(target, groupName);
                    player.sendMessage("§aAssigned '" + target.getName() + "' to group '" + groupName + "'.");
                }
                groupManager.TabManager();
                openGroupActionMenu(player, groupName);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    private void fillInventoryWithGlass(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane());
            }
        }
    }

    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(FILLER_MATERIAL);
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
        if (meta != null) {
            meta.setDisplayName(title);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static class PendingInput {
        private final String groupName;
        private final PendingAction type;

        private PendingInput(String groupName, PendingAction type) {
            this.groupName = groupName;
            this.type = type;
        }
    }

    private enum PendingAction {
        CREATE_GROUP,
        RENAME_GROUP,
        SET_PREFIX,
        CUSTOM_PERMISSION
    }

    private static class GroupMenuHolder implements InventoryHolder {
        private final String groupName;
        private final String mode;

        private GroupMenuHolder(String groupName, String mode) {
            this.groupName = groupName;
            this.mode = mode;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
