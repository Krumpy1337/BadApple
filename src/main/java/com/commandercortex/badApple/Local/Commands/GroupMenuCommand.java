package com.commandercortex.badApple.Local.Commands;

import com.commandercortex.badApple.BadApple;
import com.commandercortex.badApple.Local.Permissions.GroupConfig;
import com.commandercortex.badApple.Local.Permissions.GroupManager;
import com.commandercortex.badApple.Local.Permissions.PermissionManager;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI-driven group manager used by staff to create/modify groups, tracks, permissions and player assignments.
 */
public class GroupMenuCommand implements CommandExecutor, Listener, TabCompleter {
    private static final String MAIN_TITLE = "§8Group Manager";
    private static final String GROUP_ACTION_TITLE = "§8Group Actions";
    private static final String TRACK_TITLE = "§8Track Manager";
    private static final String TRACK_ACTION_TITLE = "§8Track Actions";
    private static final String PERMISSION_TITLE = "§8Permissions";
    private static final String PLAYER_TITLE = "§8Group Players";
    private static final Material FILLER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final List<String> FALLBACK_PERMISSIONS = Arrays.asList(
            "badapple.group",
                "minecraft.command.op"
        );

        private List<String> gatherAvailablePermissions() {
            List<String> perms = new ArrayList<>();
            for (org.bukkit.permissions.Permission p : Bukkit.getPluginManager().getPermissions()) {
                String name = p == null ? null : p.getName();
                if (name != null && !name.trim().isEmpty() && !perms.contains(name)) {
                    perms.add(name);
                }
            }
            for (String s : FALLBACK_PERMISSIONS) {
                if (!perms.contains(s)) perms.add(s);
            }
            Collections.sort(perms);
            return perms;
        }

    private final GroupManager groupManager = new GroupManager();
    private final Map<UUID, String> selectedGroup = new HashMap<>();
    private final Map<UUID, String> selectedTrack = new HashMap<>();
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
        inventory.setItem(3, createItem(Material.NETHER_STAR, "§dSet Weight", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(4, createItem(Material.BOOK, "§dPermissions", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(5, createItem(Material.PLAYER_HEAD, "§bAssign User", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(6, createItem(Material.SKELETON_SKULL, "§cRemove User", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(7, createItem(Material.RED_WOOL, "§4Delete Group", Collections.singletonList("§7Click a group first.")));
        inventory.setItem(8, createItem(Material.MAP, "§aGroups", Collections.singletonList("§7Open the full group directory.")));
        inventory.setItem(9, createItem(Material.PLAYER_HEAD, "§bPlayers", Collections.singletonList("§7Manage player group settings.")));
        inventory.setItem(10, createItem(Material.MAP, "§dTrack Manager", Collections.singletonList("§7Manage rank tracks.")));
        inventory.setItem(11, createItem(Material.FIREWORK_ROCKET, "§dCosmetics", Collections.singletonList("§7Open the cosmetic toggles menu.")));
        inventory.setItem(17, createItem(Material.CLOCK, "§9Refresh", Collections.singletonList("§7Refreshes the menu.")));

        String selected = selectedGroup.get(player.getUniqueId());
        if (selected != null) {
            inventory.setItem(13, createItem(Material.NAME_TAG, "§aSelected: " + selected, Arrays.asList(
                    "§7Prefix: §r" + GroupConfig.getGroupPrefix(selected),
                    "§7Weight: §r" + groupManager.getGroupWeight(selected),
                    "§7Parent: §r" + (groupManager.getParentGroup(selected).isEmpty() ? "none" : groupManager.getParentGroup(selected)),
                    "§7Track: §r" + (groupManager.getTrackForGroup(selected).isEmpty() ? "none" : groupManager.getTrackForGroup(selected))
            )));
        }

        player.openInventory(inventory);
    }

    private void openGroupDirectoryMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder("", "GROUP_DIRECTORY"), 54, "§8Group Directory");
        fillInventoryWithGlass(inventory);
        int slot = 0;
        List<String> groups = groupManager.getGroups();
        if (groups.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, "§cNo groups yet", Collections.singletonList("§7Use create group from the main menu.")));
        } else {
            for (String group : groups) {
                if (slot >= 45) {
                    break;
                }
                List<String> lore = new ArrayList<>();
                lore.add("§7Prefix: §r" + GroupConfig.getGroupPrefix(group));
                lore.add("§7Weight: §r" + groupManager.getGroupWeight(group));
                lore.add("§7Parent: §r" + (groupManager.getParentGroup(group).isEmpty() ? "none" : groupManager.getParentGroup(group)));
                lore.add("§7Track: §r" + (groupManager.getTrackForGroup(group).isEmpty() ? "none" : groupManager.getTrackForGroup(group)));
                lore.add("§7Permissions: §r" + groupManager.getPermissions(group).size());
                lore.add("§7Click to select");
                inventory.setItem(slot, createItem(Material.NAME_TAG, "§a" + group, lore));
                slot++;
            }
        }
        inventory.setItem(49, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to group manager.")));
        player.openInventory(inventory);
    }

    private void openGroupActionMenu(Player player, String group) {
        selectedGroup.put(player.getUniqueId(), group);
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(group, "GROUP_ACTION"), 54, GROUP_ACTION_TITLE);
        fillInventoryWithGlass(inventory);
        inventory.setItem(0, createItem(Material.ANVIL, "§eRename Group", Collections.singletonList("§7Type a new group name in chat.")));
        inventory.setItem(1, createItem(Material.NAME_TAG, "§6Set Prefix", Collections.singletonList("§7Type a new prefix in chat.")));
        inventory.setItem(2, createItem(Material.NETHER_STAR, "§dSet Weight", Collections.singletonList("§7Type a rank weight in chat.")));
        inventory.setItem(3, createItem(Material.BOOK, "§dToggle Permissions", Collections.singletonList("§7Open permission menu.")));
        inventory.setItem(4, createItem(Material.PLAYER_HEAD, "§bAssign User", Collections.singletonList("§7Select a player to assign.")));
        inventory.setItem(5, createItem(Material.SKELETON_SKULL, "§cRemove User", Collections.singletonList("§7Select a player to remove.")));
        inventory.setItem(6, createItem(Material.RED_WOOL, "§4Delete Group", Collections.singletonList("§7Delete this group.")));
        inventory.setItem(8, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to group list.")));
        inventory.setItem(22, createItem(Material.NAME_TAG, "§aSelected Group", Arrays.asList("§7" + group, "§7Prefix: §r" + GroupConfig.getGroupPrefix(group), "§7Weight: §r" + groupManager.getGroupWeight(group))));
        player.openInventory(inventory);
    }

    private void openTrackMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder("", "TRACK_MENU"), 54, TRACK_TITLE);
        fillInventoryWithGlass(inventory);
        inventory.setItem(0, createItem(Material.GREEN_WOOL, "§aCreate Track", Collections.singletonList("§7Type a track name in chat.")));
        inventory.setItem(1, createItem(Material.BOOK, "§eAdd Group To Track", Collections.singletonList("§7Select a track first.")));
        inventory.setItem(2, createItem(Material.RED_WOOL, "§cRemove Group From Track", Collections.singletonList("§7Select a track first.")));
        inventory.setItem(3, createItem(Material.GOLDEN_AXE, "§6Promote Player", Collections.singletonList("§7Select a track first.")));
        inventory.setItem(4, createItem(Material.IRON_AXE, "§7Demote Player", Collections.singletonList("§7Select a track first.")));
        inventory.setItem(8, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to group manager.")));

        int slot = 9;
        for (String track : groupManager.getTracks()) {
            if (slot >= 45) {
                break;
            }
            List<String> lore = new ArrayList<>();
            lore.add("§7Groups: §r" + String.join(" -> ", groupManager.getTrackGroups(track)));
            lore.add("§7Click to manage");
            inventory.setItem(slot, createItem(Material.MAP, "§a" + track, lore));
            slot++;
        }
        player.openInventory(inventory);
    }

    private void openTrackActionMenu(Player player, String track) {
        selectedTrack.put(player.getUniqueId(), track);
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(track, "TRACK_ACTION"), 54, TRACK_ACTION_TITLE);
        fillInventoryWithGlass(inventory);
        inventory.setItem(0, createItem(Material.GREEN_WOOL, "§aAdd Group To Track", Collections.singletonList("§7Choose a group to add.")));
        inventory.setItem(1, createItem(Material.RED_WOOL, "§cRemove Group From Track", Collections.singletonList("§7Choose a group to remove.")));
        inventory.setItem(2, createItem(Material.GOLDEN_AXE, "§6Promote Player", Collections.singletonList("§7Move a player up this track.")));
        inventory.setItem(3, createItem(Material.IRON_AXE, "§7Demote Player", Collections.singletonList("§7Move a player down this track.")));
        inventory.setItem(8, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to track list.")));
        inventory.setItem(22, createItem(Material.NAME_TAG, "§aTrack", Arrays.asList("§7" + track, "§7Order: §r" + String.join(" -> ", groupManager.getTrackGroups(track)))));
        player.openInventory(inventory);
    }

    private void openPermissionMenu(Player player, String group) {
            openPermissionMenu(player, group, 0);
        }

        private void openPermissionMenu(Player player, String group, int page) {
            List<String> allPerms = gatherAvailablePermissions();
            int perPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) allPerms.size() / perPage));
            int normalizedPage = Math.max(0, Math.min(page, totalPages - 1));

            Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(group, "PERMISSIONS", normalizedPage), 54, PERMISSION_TITLE);
            fillInventoryWithGlass(inventory);

            int slot = 0;
            int start = normalizedPage * perPage;
            int end = Math.min(allPerms.size(), start + perPage);
            for (int i = start; i < end; i++) {
                String permission = allPerms.get(i);
                boolean enabled = groupManager.getPermissions(group).contains(permission);
                Material material = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
                String title = enabled ? "§a" + permission : "§c" + permission;
                inventory.setItem(slot, createItem(material, title, Collections.singletonList("§7Click to toggle.")));
                slot++;
            }

            // Pagination controls
            if (normalizedPage > 0) {
                inventory.setItem(45, createItem(Material.ARROW, "§ePrevious Page", Collections.singletonList("§7Go to previous page.")));
            }
            inventory.setItem(49, createItem(Material.BOOK, "§eCustom Node", Collections.singletonList("§7Type a custom permission in chat.")));
            if (normalizedPage < totalPages - 1) {
                inventory.setItem(51, createItem(Material.ARROW, "§eNext Page", Collections.singletonList("§7Go to next page.")));
            }
            inventory.setItem(53, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to group actions.")));
            player.openInventory(inventory);
        }

    public void openPlayerManagementMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder("", "PLAYER_LIST"), 54, "§8Players");
        fillInventoryWithGlass(inventory);
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) {
                break;
            }
            List<String> lore = new ArrayList<>();
            String targetGroup = groupManager.getGroup(target);
            lore.add("§7Group: §r" + (targetGroup.isEmpty() ? "none" : targetGroup));
            lore.add("§7Suffix: §r" + (groupManager.getUserSuffix(target).isEmpty() ? "none" : groupManager.getUserSuffix(target)));
            lore.add("§7Custom perms: §r" + groupManager.getUserPermissions(target).size());
            lore.add("§7Click to manage");
            inventory.setItem(slot, createPlayerHead(target.getName(), "§a" + target.getName(), lore));
            slot++;
        }
        inventory.setItem(49, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to group manager.")));
        player.openInventory(inventory);
    }

    public void openPlayerActionMenu(Player player, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer is no longer online.");
            openPlayerManagementMenu(player);
            return;
        }

        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(targetName, "PLAYER_ACTION"), 54, "§8Player Controls");
        fillInventoryWithGlass(inventory);
        inventory.setItem(0, createItem(Material.NAME_TAG, "§6Set Group", Collections.singletonList("§7Assign a new group.")));
        inventory.setItem(1, createItem(Material.BOOK, "§eSet Suffix", Collections.singletonList("§7Set a custom suffix in chat.")));
        inventory.setItem(2, createItem(Material.BOOK, "§cClear Suffix", Collections.singletonList("§7Remove the player suffix.")));
        inventory.setItem(3, createItem(Material.BOOK, "§dToggle Permissions", Collections.singletonList("§7Open player permission toggles.")));
        inventory.setItem(4, createItem(Material.PAPER, "§dView Group Settings", Collections.singletonList("§7Open the selected group menu.")));
        inventory.setItem(5, createItem(Material.REDSTONE, "§4Open Punish Menu", Collections.singletonList("§7Jump to punishment actions.")));
        inventory.setItem(8, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to player list.")));
        inventory.setItem(22, createItem(Material.PLAYER_HEAD, "§bTarget", Arrays.asList(
                "§7" + target.getName(),
                "§7Group: §r" + (groupManager.getGroup(target).isEmpty() ? "none" : groupManager.getGroup(target)),
                "§7Suffix: §r" + (groupManager.getUserSuffix(target).isEmpty() ? "none" : groupManager.getUserSuffix(target))
        )));
        player.openInventory(inventory);
    }

    private void openUserGroupSelectionMenu(Player player, String targetName) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(targetName, "USER_SET_GROUP"), 54, "§8Assign Group");
        fillInventoryWithGlass(inventory);
        int slot = 0;
        for (String group : groupManager.getGroups()) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot, createItem(Material.NAME_TAG, "§a" + group, Arrays.asList(
                    "§7Click to assign this group to " + targetName,
                    "§7Prefix: §r" + GroupConfig.getGroupPrefix(group)
            )));
            slot++;
        }
        inventory.setItem(49, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to player actions.")));
        player.openInventory(inventory);
    }

    private void openUserPermissionSelectionMenu(Player player, String targetName) {
            openUserPermissionSelectionMenu(player, targetName, 0);
        }

        private void openUserPermissionSelectionMenu(Player player, String targetName, int page) {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                player.sendMessage("§cPlayer is no longer online.");
                return;
            }

            List<String> allPerms = gatherAvailablePermissions();
            int perPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) allPerms.size() / perPage));
            int normalizedPage = Math.max(0, Math.min(page, totalPages - 1));

            Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(targetName, "USER_TOGGLE_PERMISSION", normalizedPage), 54, "§8Player Permissions");
            fillInventoryWithGlass(inventory);
            int slot = 0;
            int start = normalizedPage * perPage;
            int end = Math.min(allPerms.size(), start + perPage);
            for (int i = start; i < end; i++) {
                String permission = allPerms.get(i);
                if (slot >= 45) {
                    break;
                }
                boolean enabled = groupManager.getUserPermissions(target).contains(permission) || groupManager.getPermissions(groupManager.getGroup(target)).contains(permission);
                Material material = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
                String title = enabled ? "§a" + permission : "§c" + permission;
                inventory.setItem(slot, createItem(material, title, Collections.singletonList("§7Click to toggle this permission for the player.")));
                slot++;
            }

            if (normalizedPage > 0) {
                inventory.setItem(45, createItem(Material.ARROW, "§ePrevious Page", Collections.singletonList("§7Go to previous page.")));
            }
            inventory.setItem(49, createItem(Material.BOOK, "§eCustom Node", Collections.singletonList("§7Type a custom permission in chat.")));
            if (normalizedPage < totalPages - 1) {
                inventory.setItem(51, createItem(Material.ARROW, "§eNext Page", Collections.singletonList("§7Go to next page.")));
            }
            inventory.setItem(53, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to player actions.")));
            player.openInventory(inventory);
        }

    private void openTrackGroupSelectionMenu(Player player, String track, boolean removeMode) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(track, removeMode ? "REMOVE_TRACK_GROUP" : "ADD_TRACK_GROUP"), 54, PLAYER_TITLE);
        fillInventoryWithGlass(inventory);
        int slot = 0;
        List<String> groups = groupManager.getGroups();
        List<String> trackGroups = groupManager.getTrackGroups(track);
        for (String group : groups) {
            if (slot >= 45) {
                break;
            }
            if (removeMode && !trackGroups.contains(group)) {
                continue;
            }
            if (!removeMode && trackGroups.contains(group)) {
                continue;
            }
            inventory.setItem(slot, createItem(Material.NAME_TAG, "§a" + group, Arrays.asList(
                    "§7Click to " + (removeMode ? "remove from" : "add to") + " track '" + track + "'",
                    "§7Current parent: §r" + (groupManager.getParentGroup(group).isEmpty() ? "none" : groupManager.getParentGroup(group))
            )));
            slot++;
        }
        inventory.setItem(49, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to track actions.")));
        player.openInventory(inventory);
    }

    private void openTrackPlayerSelectionMenu(Player player, String track, boolean promoteMode) {
        Inventory inventory = Bukkit.createInventory(new GroupMenuHolder(track, promoteMode ? "PROMOTE_PLAYER" : "DEMOTE_PLAYER"), 54, PLAYER_TITLE);
        fillInventoryWithGlass(inventory);
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) {
                break;
            }
            String currentGroup = groupManager.getGroup(online);
            inventory.setItem(slot, createItem(Material.PLAYER_HEAD, "§a" + online.getName(), Arrays.asList(
                    "§7Click to " + (promoteMode ? "promote" : "demote") + " this player",
                    "§7Current group: §r" + (currentGroup.isEmpty() ? "none" : currentGroup)
            )));
            slot++;
        }
        inventory.setItem(49, createItem(Material.ARROW, "§9Back", Collections.singletonList("§7Return to track actions.")));
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
                case SET_WEIGHT:
                    if (groupName == null || groupName.isEmpty()) {
                        player.sendMessage("§cNo group selected.");
                        return;
                    }
                    try {
                        int weight = Integer.parseInt(message);
                        groupManager.setGroupWeight(groupName, weight);
                        groupManager.TabManager();
                        player.sendMessage("§aUpdated weight for '" + groupName + "' to " + weight + ".");
                        openGroupActionMenu(player, groupName);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cWeight must be a number.");
                    }
                    break;
                case CREATE_TRACK:
                    if (message.isEmpty()) {
                        player.sendMessage("§cTrack name cannot be empty.");
                        return;
                    }
                    if (groupManager.getTracks().contains(message)) {
                        player.sendMessage("§cTrack '" + message + "' already exists.");
                        return;
                    }
                    groupManager.createTrack(message, new ArrayList<>());
                    selectedTrack.put(uuid, message);
                    player.sendMessage("§aCreated track '" + message + "'.");
                    openTrackActionMenu(player, message);
                    break;
                case ADD_TO_TRACK:
                    if (groupName == null || groupName.isEmpty()) {
                        player.sendMessage("§cNo track selected.");
                        return;
                    }
                    if (!groupManager.groupExists(message)) {
                        player.sendMessage("§cGroup '" + message + "' does not exist.");
                        return;
                    }
                    groupManager.addGroupToTrack(groupName, message);
                    player.sendMessage("§aAdded group '" + message + "' to track '" + groupName + "'.");
                    openTrackActionMenu(player, groupName);
                    break;
                case REMOVE_FROM_TRACK:
                    if (groupName == null || groupName.isEmpty()) {
                        player.sendMessage("§cNo track selected.");
                        return;
                    }
                    if (!groupManager.groupExists(message)) {
                        player.sendMessage("§cGroup '" + message + "' does not exist.");
                        return;
                    }
                    groupManager.removeGroupFromTrack(groupName, message);
                    player.sendMessage("§aRemoved group '" + message + "' from track '" + groupName + "'.");
                    openTrackActionMenu(player, groupName);
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
                case SET_USER_SUFFIX:
                    Player targetForSuffix = Bukkit.getPlayerExact(groupName);
                    if (targetForSuffix == null) {
                        player.sendMessage("§cPlayer is no longer online.");
                        return;
                    }
                    if (message.equalsIgnoreCase("clear") || message.equalsIgnoreCase("remove") || message.isEmpty()) {
                        groupManager.setUserSuffix(targetForSuffix, "");
                        groupManager.TabManager();
                        player.sendMessage("§aCleared suffix for '" + targetForSuffix.getName() + "'.");
                    } else {
                        groupManager.setUserSuffix(targetForSuffix, message);
                        groupManager.TabManager();
                        player.sendMessage("§aUpdated suffix for '" + targetForSuffix.getName() + "' to '" + message + "'.");
                    }
                    openPlayerActionMenu(player, targetForSuffix.getName());
                    break;
                case ADD_USER_PERMISSION:
                    Player targetForPermission = Bukkit.getPlayerExact(groupName);
                    if (targetForPermission == null) {
                        player.sendMessage("§cPlayer is no longer online.");
                        return;
                    }
                    groupManager.addUserPermission(targetForPermission, message);
                    PermissionManager.applyGroupPermissions(targetForPermission);
                    player.sendMessage("§aAdded custom permission '" + message + "' to '" + targetForPermission.getName() + "'.");
                    openPlayerActionMenu(player, targetForPermission.getName());
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
            if (displayName.contains("Track Manager")) {
                openTrackMenu(player);
                return;
            }
            if (displayName.contains("Groups")) {
                openGroupDirectoryMenu(player);
                return;
            }
            if (displayName.contains("Players")) {
                openPlayerManagementMenu(player);
                return;
            }
            if (displayName.contains("Cosmetics")) {
                new CosmeticsCommand().openMenu(player);
                return;
            }
            if (displayName.contains("Refresh")) {
                openMainMenu(player);
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

        if (event.getView().getTitle().equals("§8Group Directory")) {
            if (displayName.contains("Back")) {
                openMainMenu(player);
                return;
            }
            if (displayName.startsWith("§a")) {
                String groupName = displayName.replace("§a", "");
                selectedGroup.put(player.getUniqueId(), groupName);
                openGroupActionMenu(player, groupName);
                return;
            }
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
            if (displayName.contains("Set Weight")) {
                pendingInput.put(player.getUniqueId(), new PendingInput(groupName, PendingAction.SET_WEIGHT));
                player.closeInventory();
                player.sendMessage("§aType the group weight in chat. Lower numbers appear higher in the tablist. Type §c'cancel'§a to cancel.");
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

        if (event.getView().getTitle().equals(TRACK_TITLE)) {
            if (displayName.contains("Back")) {
                openMainMenu(player);
                return;
            }
            if (displayName.contains("Create Track")) {
                pendingInput.put(player.getUniqueId(), new PendingInput(null, PendingAction.CREATE_TRACK));
                player.closeInventory();
                player.sendMessage("§aType the new track name in chat. Type §c'cancel'§a to cancel.");
                return;
            }
            if (displayName.startsWith("§a") && !displayName.startsWith("§aTrack")) {
                String track = displayName.replace("§a", "");
                selectedTrack.put(player.getUniqueId(), track);
                openTrackActionMenu(player, track);
                return;
            }
            return;
        }

        if (event.getView().getTitle().equals(TRACK_ACTION_TITLE)) {
            String trackName = holder.groupName;
            if (displayName.contains("Back")) {
                openTrackMenu(player);
                return;
            }
            if (displayName.contains("Add Group To Track")) {
                openTrackGroupSelectionMenu(player, trackName, false);
                return;
            }
            if (displayName.contains("Remove Group From Track")) {
                openTrackGroupSelectionMenu(player, trackName, true);
                return;
            }
            if (displayName.contains("Promote Player")) {
                openTrackPlayerSelectionMenu(player, trackName, true);
                return;
            }
            if (displayName.contains("Demote Player")) {
                openTrackPlayerSelectionMenu(player, trackName, false);
                return;
            }
            return;
        }

        if (event.getView().getTitle().equals(PERMISSION_TITLE)) {
            String groupName = holder.groupName;
                    int currentPage = (holder instanceof GroupMenuHolder) ? ((GroupMenuHolder) holder).page : 0;
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
                    if (displayName.contains("Previous Page")) {
                        openPermissionMenu(player, groupName, currentPage - 1);
                        return;
                    }
                    if (displayName.contains("Next Page")) {
                        openPermissionMenu(player, groupName, currentPage + 1);
                        return;
                    }

                    String permission = displayName.replace("§a", "").replace("§c", "");
                    if (groupManager.getPermissions(groupName).contains(permission)) {
                        groupManager.removePermission(groupName, permission);
                    } else {
                        groupManager.addPermission(groupName, permission);
                    }

                    // Persisted; refresh tab and permissions for affected online players immediately
                    groupManager.TabManager();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (groupName.equalsIgnoreCase(groupManager.getGroup(p))) {
                            PermissionManager.applyGroupPermissions(p);
                        }
                    }
                    openPermissionMenu(player, groupName, currentPage);
                    return;
                }

        if (event.getView().getTitle().equals(PLAYER_TITLE)) {
            String groupName = holder.groupName;
            if (displayName.contains("Back")) {
                if ("ADD_TRACK_GROUP".equals(holder.mode) || "REMOVE_TRACK_GROUP".equals(holder.mode) || "PROMOTE_PLAYER".equals(holder.mode) || "DEMOTE_PLAYER".equals(holder.mode)) {
                    String track = selectedTrack.get(player.getUniqueId());
                    if (track != null) {
                        openTrackActionMenu(player, track);
                        return;
                    }
                }
                if ("USER_SET_GROUP".equals(holder.mode)) {
                    Player target = Bukkit.getPlayerExact(groupName);
                    if (target != null) {
                        openPlayerActionMenu(player, target.getName());
                        return;
                    }
                }
                if ("USER_TOGGLE_PERMISSION".equals(holder.mode)) {
                    Player target = Bukkit.getPlayerExact(groupName);
                    if (target != null) {
                        openPlayerActionMenu(player, target.getName());
                        return;
                    }
                }
                openGroupActionMenu(player, groupName);
                return;
            }

            if ("ADD_TRACK_GROUP".equals(holder.mode) || "REMOVE_TRACK_GROUP".equals(holder.mode)) {
                String clickedGroup = displayName.replace("§a", "");
                if (groupManager.groupExists(clickedGroup)) {
                    if ("ADD_TRACK_GROUP".equals(holder.mode)) {
                        groupManager.addGroupToTrack(groupName, clickedGroup);
                        player.sendMessage("§aAdded group '" + clickedGroup + "' to track '" + groupName + "'.");
                    } else {
                        groupManager.removeGroupFromTrack(groupName, clickedGroup);
                        player.sendMessage("§aRemoved group '" + clickedGroup + "' from track '" + groupName + "'.");
                    }
                    groupManager.TabManager();
                    openTrackActionMenu(player, groupName);
                }
                return;
            }

            if ("USER_SET_GROUP".equals(holder.mode)) {
                String clickedGroup = displayName.replace("§a", "");
                if (groupManager.groupExists(clickedGroup)) {
                    Player target = Bukkit.getPlayerExact(groupName);
                    if (target != null) {
                        groupManager.setGroup(target, clickedGroup);
                        PermissionManager.applyGroupPermissions(target);
                        player.sendMessage("§aAssigned '" + target.getName() + "' to group '" + clickedGroup + "'.");
                        groupManager.TabManager();
                        openPlayerActionMenu(player, target.getName());
                    }
                }
                return;
            }

            if ("USER_TOGGLE_PERMISSION".equals(holder.mode)) {
                            int currentPage = (holder instanceof GroupMenuHolder) ? ((GroupMenuHolder) holder).page : 0;
                            if (displayName.contains("Previous Page")) {
                                openUserPermissionSelectionMenu(player, groupName, currentPage - 1);
                                return;
                            }
                            if (displayName.contains("Next Page")) {
                                openUserPermissionSelectionMenu(player, groupName, currentPage + 1);
                                return;
                            }

                            String permission = displayName.replace("§a", "").replace("§c", "");
                            Player target = Bukkit.getPlayerExact(groupName);
                            if (target != null) {
                                if (groupManager.getUserPermissions(target).contains(permission)) {
                                    groupManager.removeUserPermission(target, permission);
                                    player.sendMessage("§aRemoved custom permission '" + permission + "' from '" + target.getName() + "'.");
                                } else {
                                    groupManager.addUserPermission(target, permission);
                                    player.sendMessage("§aAdded custom permission '" + permission + "' to '" + target.getName() + "'.");
                                }
                                PermissionManager.applyGroupPermissions(target);
                                openUserPermissionSelectionMenu(player, target.getName(), currentPage);
                            }
                            return;
                        }

            Player target = Bukkit.getPlayerExact(displayName.replace("§a", ""));
            if (target != null) {
                if ("REMOVE_PLAYER".equals(holder.mode)) {
                    groupManager.setGroup(target, "");
                    player.sendMessage("§aRemoved '" + target.getName() + "' from their group.");
                } else if ("PROMOTE_PLAYER".equals(holder.mode)) {
                    groupManager.promotePlayer(target, groupName);
                    player.sendMessage("§aPromoted '" + target.getName() + "' along track '" + groupName + "'.");
                } else if ("DEMOTE_PLAYER".equals(holder.mode)) {
                    groupManager.demotePlayer(target, groupName);
                    player.sendMessage("§aDemoted '" + target.getName() + "' along track '" + groupName + "'.");
                } else {
                    groupManager.setGroup(target, groupName);
                    player.sendMessage("§aAssigned '" + target.getName() + "' to group '" + groupName + "'.");
                }
                groupManager.TabManager();
                if ("REMOVE_PLAYER".equals(holder.mode) || "PROMOTE_PLAYER".equals(holder.mode) || "DEMOTE_PLAYER".equals(holder.mode)) {
                    String track = selectedTrack.get(player.getUniqueId());
                    if (track != null) {
                        openTrackActionMenu(player, track);
                        return;
                    }
                }
                openGroupActionMenu(player, groupName);
            }
        }

        if (event.getView().getTitle().equals("§8Players")) {
            if (displayName.contains("Back")) {
                openMainMenu(player);
                return;
            }
            if (displayName.startsWith("§a")) {
                String targetName = displayName.replace("§a", "");
                openPlayerActionMenu(player, targetName);
                return;
            }
        }

        if (event.getView().getTitle().equals("§8Player Controls")) {
            String targetName = holder.groupName;
            if (displayName.contains("Back")) {
                openPlayerManagementMenu(player);
                return;
            }
            if (displayName.contains("Set Group")) {
                openUserGroupSelectionMenu(player, targetName);
                return;
            }
            if (displayName.contains("Set Suffix")) {
                pendingInput.put(player.getUniqueId(), new PendingInput(targetName, PendingAction.SET_USER_SUFFIX));
                player.closeInventory();
                player.sendMessage("§aType the player's suffix in chat. Use plain text like '§7VIP§a'. Type §c'clear'§a or §c'cancel'§a to remove it.");
                return;
            }
            if (displayName.contains("Clear Suffix")) {
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    groupManager.setUserSuffix(target, "");
                    groupManager.TabManager();
                    player.sendMessage("§aCleared suffix for '" + target.getName() + "'.");
                    openPlayerActionMenu(player, target.getName());
                }
                return;
            }
            if (displayName.contains("Toggle Permissions")) {
                openUserPermissionSelectionMenu(player, targetName);
                return;
            }
            if (displayName.contains("View Group Settings")) {
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    String group = groupManager.getGroup(target);
                    if (!group.isEmpty()) {
                        selectedGroup.put(player.getUniqueId(), group);
                        openGroupActionMenu(player, group);
                        return;
                    }
                    player.sendMessage("§cThis player has no group assigned.");
                }
                return;
            }
            if (displayName.contains("Open Punish Menu")) {
                new PunishMenuCommand().openPlayerMenu(player, targetName);
                return;
            }
        }

        if (event.getView().getTitle().equals("§8Assign Group")) {
            String targetName = holder.groupName;
            if (displayName.contains("Back")) {
                if (targetName != null && !targetName.isEmpty()) {
                    openPlayerActionMenu(player, targetName);
                }
                return;
            }

            if (displayName.startsWith("§a")) {
                String clickedGroup = displayName.replace("§a", "");
                if (groupManager.groupExists(clickedGroup)) {
                    Player target = Bukkit.getPlayerExact(targetName);
                    if (target != null) {
                        groupManager.setGroup(target, clickedGroup);
                        PermissionManager.applyGroupPermissions(target);
                        player.sendMessage("§aAssigned '" + target.getName() + "' to group '" + clickedGroup + "'.");
                        groupManager.TabManager();
                        openPlayerActionMenu(player, target.getName());
                    }
                }
            }
            return;
        }

        if (event.getView().getTitle().equals("§8Player Permissions")) {
            String targetName = holder.groupName;
            if (displayName.contains("Back")) {
                openPlayerActionMenu(player, targetName);
                return;
            }
            if (displayName.contains("Custom Node")) {
                pendingInput.put(player.getUniqueId(), new PendingInput(targetName, PendingAction.ADD_USER_PERMISSION));
                player.closeInventory();
                player.sendMessage("§aType a custom permission node in chat. Type §c'cancel'§a to cancel.");
                return;
            }
            String permission = displayName.replace("§a", "").replace("§c", "");
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null) {
                if (groupManager.getUserPermissions(target).contains(permission)) {
                    groupManager.removeUserPermission(target, permission);
                    player.sendMessage("§aRemoved custom permission '" + permission + "' from '" + target.getName() + "'.");
                } else {
                    groupManager.addUserPermission(target, permission);
                    player.sendMessage("§aAdded custom permission '" + permission + "' to '" + target.getName() + "'.");
                }
                PermissionManager.applyGroupPermissions(target);
                openUserPermissionSelectionMenu(player, target.getName());
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
        SET_WEIGHT,
        CREATE_TRACK,
        ADD_TO_TRACK,
        REMOVE_FROM_TRACK,
        CUSTOM_PERMISSION,
        SET_USER_SUFFIX,
        ADD_USER_PERMISSION
    }

    private static class GroupMenuHolder implements InventoryHolder {
        private final String groupName;
        private final String mode;
            private final int page;

            private GroupMenuHolder(String groupName, String mode) {
                this(groupName, mode, 0);
        }

            private GroupMenuHolder(String groupName, String mode, int page) {
                this.groupName = groupName;
                this.mode = mode;
                this.page = Math.max(0, page);
            }

            @Override
            public Inventory getInventory() {
                return null;
            }
        }
}
