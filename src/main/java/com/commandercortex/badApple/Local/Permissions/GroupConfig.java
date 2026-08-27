package com.commandercortex.badApple.Local.Permissions;

import com.commandercortex.badApple.BadApple;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GroupConfig {
    private GroupConfig() {
    }

    public static BadApple getPluginInstance() {
        return BadApple.getPlugin(BadApple.class);
    }

    public static FileConfiguration getConfig() {
        return getPluginInstance().getConfig();
    }

    public static File getGroupsFolder() {
        return new File(getPluginInstance().getDataFolder(), "groups");
    }

    public static void ensureGroupsLoaded() {
        File groupsFolder = getGroupsFolder();
        if (!groupsFolder.exists()) {
            groupsFolder.mkdirs();
        }

        File[] files = groupsFolder.listFiles();
        if (files == null || files.length == 0) {
            new com.commandercortex.badApple.Local.Folders.SetupFolders().CreateDefaultGroups();
        }
    }

    public static String normalizeGroupName(String groupName) {
        if (groupName == null) {
            return "";
        }
        return groupName.trim();
    }

    public static List<File> getGroupFiles() {
        File groupsFolder = getGroupsFolder();
        if (!groupsFolder.exists()) {
            return Collections.emptyList();
        }

        File[] files = groupsFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return Collections.emptyList();
        }

        List<File> groupFiles = new ArrayList<>();
        Collections.addAll(groupFiles, files);
        return groupFiles;
    }

    public static String resolveGroupName(String groupName) {
        String normalizedName = normalizeGroupName(groupName);
        if (normalizedName.isEmpty()) {
            return null;
        }

        for (File groupFile : getGroupFiles()) {
            String fileName = groupFile.getName();
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                String groupKey = fileName.substring(0, fileName.length() - 4);
                if (groupKey.equalsIgnoreCase(normalizedName)) {
                    return groupKey;
                }
            }
        }
        return null;
    }

    public static boolean groupExists(String groupName) {
        return resolveGroupName(groupName) != null;
    }

    public static YamlConfiguration loadGroupFile(String groupName) {
        String group = resolveGroupName(groupName);
        if (group == null) {
            return null;
        }

        File groupFile = new File(getGroupsFolder(), group + ".yml");
        if (!groupFile.exists()) {
            return null;
        }

        return YamlConfiguration.loadConfiguration(groupFile);
    }

    public static void createGroup(String groupName) {
        String name = normalizeGroupName(groupName);
        if (name.isEmpty() || groupExists(name)) {
            return;
        }

        File file = new File(getGroupsFolder(), name + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("prefix", "&7[" + name + "] &r");
        yaml.set("inherits", "");
        yaml.set("permissions", new ArrayList<String>());

        try {
            yaml.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create group file: " + name, e);
        }
    }

    public static void renameGroup(String oldGroupName, String newGroupName) {
        String oldGroup = resolveGroupName(oldGroupName);
        String newName = normalizeGroupName(newGroupName);
        if (oldGroup == null || newName.isEmpty() || groupExists(newName)) {
            return;
        }

        File oldFile = new File(getGroupsFolder(), oldGroup + ".yml");
        File newFile = new File(getGroupsFolder(), newName + ".yml");
        if (oldFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(oldFile);
            yaml.set("prefix", yaml.getString("prefix", "&7[" + newName + "] &r"));
            try {
                yaml.save(newFile);
                oldFile.delete();
            } catch (IOException e) {
                throw new RuntimeException("Failed to rename group file: " + oldGroup, e);
            }
        }
    }

    public static void deleteGroup(String groupName) {
        String resolved = resolveGroupName(groupName);
        if (resolved == null) {
            return;
        }

        File file = new File(getGroupsFolder(), resolved + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    public static void setGroupPrefix(String groupName, String prefix) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null || prefix == null) {
            return;
        }

        yaml.set("prefix", prefix);
        saveYamlFile(groupName, yaml);
    }

    public static void setParentGroup(String groupName, String parentGroupName) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null) {
            return;
        }

        String parent = parentGroupName == null ? "" : normalizeGroupName(parentGroupName);
        if (!parent.isEmpty() && !groupExists(parent)) {
            return;
        }

        yaml.set("inherits", parent);
        saveYamlFile(groupName, yaml);
    }

    public static String getParentGroup(String groupName) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null) {
            return "";
        }
        return yaml.getString("inherits", "");
    }

    public static String getGroupPrefix(String groupName) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null) {
            return "";
        }
        return yaml.getString("prefix", "");
    }

    public static void addPermission(String groupName, String permission) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null || permission == null) {
            return;
        }

        String permissionNode = permission.trim();
        if (permissionNode.isEmpty()) {
            return;
        }

        List<String> permissions = getPermissions(groupName);
        if (!permissions.contains(permissionNode)) {
            permissions.add(permissionNode);
            yaml.set("permissions", permissions);
            saveYamlFile(groupName, yaml);
        }
    }

    public static void removePermission(String groupName, String permission) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null || permission == null) {
            return;
        }

        String permissionNode = permission.trim();
        if (permissionNode.isEmpty()) {
            return;
        }

        List<String> permissions = getPermissions(groupName);
        permissions.remove(permissionNode);
        yaml.set("permissions", permissions);
        saveYamlFile(groupName, yaml);
    }

    public static List<String> getPermissions(String groupName) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null) {
            return new ArrayList<>();
        }
        List<String> permissions = yaml.getStringList("permissions");
        return permissions == null ? new ArrayList<>() : new ArrayList<>(permissions);
    }

    public static List<String> getEffectivePermissions(String groupName) {
        return getEffectivePermissions(groupName, new HashSet<String>());
    }

    private static List<String> getEffectivePermissions(String groupName, Set<String> visited) {
        String group = resolveGroupName(groupName);
        if (group == null) {
            return new ArrayList<>();
        }

        if (!visited.add(group.toLowerCase(Locale.ROOT))) {
            return new ArrayList<>();
        }

        List<String> effective = new ArrayList<>();

        if (group.equalsIgnoreCase("Developer") || group.equalsIgnoreCase("Dev")) {
            for (String otherGroup : getGroups()) {
                if (!otherGroup.equalsIgnoreCase(group)) {
                    effective.addAll(getEffectivePermissions(otherGroup, new HashSet<>(visited)));
                }
            }
        }

        String inherited = getParentGroup(group);
        if (!inherited.isEmpty()) {
            effective.addAll(getEffectivePermissions(inherited, new HashSet<>(visited)));
        }

        for (String permission : getPermissions(group)) {
            if (!effective.contains(permission)) {
                effective.add(permission);
            }
        }

        return effective;
    }

    public static List<String> getGroups() {
        List<String> groups = new ArrayList<>();
        for (File groupFile : getGroupFiles()) {
            String fileName = groupFile.getName();
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                groups.add(fileName.substring(0, fileName.length() - 4));
            }
        }
        return groups;
    }

    public static void saveYamlFile(String groupName, YamlConfiguration yaml) {
        String group = resolveGroupName(groupName);
        if (group == null) {
            return;
        }

        File groupFile = new File(getGroupsFolder(), group + ".yml");
        try {
            yaml.save(groupFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save group file: " + group, e);
        }
    }
}
