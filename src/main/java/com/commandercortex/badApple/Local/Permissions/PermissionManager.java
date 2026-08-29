package com.commandercortex.badApple.Local.Permissions;

import com.commandercortex.badApple.BadApple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages runtime permission attachments for players and applies group/user permissions.
 */
public class PermissionManager {
    private static final Map<UUID, PermissionAttachment> ATTACHMENTS = new HashMap<>();

    public static PermissionAttachment fetchPlayer(UUID uuid) {
        Plugin plugin = BadApple.getPlugin(BadApple.class);
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return null;
        }

        PermissionAttachment attachment = ATTACHMENTS.get(uuid);
        if (attachment == null) {
            attachment = player.addAttachment(plugin);
            ATTACHMENTS.put(uuid, attachment);
        }
        return attachment;
    }

    public static void addPermission(UUID uuid, String permission) {
        PermissionAttachment permissionAttachment = fetchPlayer(uuid);
        if (permissionAttachment != null) {
            permissionAttachment.setPermission(permission, true);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.recalculatePermissions();
            }
        }
    }

    public static void removePermission(UUID uuid, String permission) {
        PermissionAttachment permissionAttachment = fetchPlayer(uuid);
        if (permissionAttachment != null) {
            permissionAttachment.unsetPermission(permission);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.recalculatePermissions();
            }
        }
    }

    public static void clearPermissions(Player player) {
        PermissionAttachment attachment = ATTACHMENTS.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
            player.recalculatePermissions();
        }
    }

    public static void applyGroupPermissions(Player player) {
        clearPermissions(player);

        GroupManager groupManager = new GroupManager();
        String groupName = groupManager.getGroup(player);
        PermissionAttachment attachment = fetchPlayer(player.getUniqueId());
        if (attachment == null) {
            return;
        }

        for (String permission : groupManager.getPermissions(groupName)) {
            if (permission != null && !permission.trim().isEmpty()) {
                attachment.setPermission(permission.trim(), true);
            }
        }

        if (groupName != null && groupName.equalsIgnoreCase("Diamond")) {
            attachment.setPermission("badapple.nickname", true);
        }

        for (String permission : groupManager.getUserPermissions(player)) {
            if (permission != null && !permission.trim().isEmpty()) {
                attachment.setPermission(permission.trim(), true);
            }
        }

        player.recalculatePermissions();
    }
}
