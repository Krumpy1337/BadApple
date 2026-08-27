package com.commandercortex.badApple.Local.Permissions;

import com.commandercortex.badApple.BadApple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class PermissionManager {
    public static PermissionAttachment fetchPlayer(UUID uuid){
        Plugin plugin = BadApple.getPlugin(BadApple.class);
        Player player = Bukkit.getPlayer(uuid);
        if(player != null)
            return player.addAttachment(plugin);

        return null;
    }

    public static void addPermission(UUID uuid, String Permission){
        PermissionAttachment permissionAttachment = fetchPlayer(uuid);
        assert permissionAttachment != null;
        permissionAttachment.setPermission(Permission, true);
    }

    public static void removePermission(UUID uuid, String Permission){
        PermissionAttachment permissionAttachment = fetchPlayer(uuid);
        assert permissionAttachment != null;
        permissionAttachment.unsetPermission(Permission);
    }
    
}
