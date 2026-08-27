package com.commandercortex.badApple.Local.Permissions;

import com.commandercortex.badApple.BadApple;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

        ensureDefaultGroupMetadata();
    }

    private static void ensureDefaultGroupMetadata() {
        Map<String, List<String>> defaultCosmetics = new HashMap<>();
        defaultCosmetics.put("Admin", java.util.Arrays.asList(
                "trail: staff_glow",
                "particle: starfall",
                "trail: arc_prism",
                "particle: crystal_bloom",
                "trail: ember_run",
                "particle: spark_burst",
                "wing: staff_wings",
                "wing: copper_wings",
                "wing: iron_wings",
                "wing: gold_wings",
                "wing: emerald_wings",
                "wing: diamond_wings"
        ));
        defaultCosmetics.put("Mod", java.util.Arrays.asList(
                "trail: staff_glow",
                "particle: starfall",
                "trail: arc_prism",
                "particle: crystal_bloom",
                "trail: ember_run",
                "particle: spark_burst",
                "wing: staff_wings",
                "wing: copper_wings",
                "wing: iron_wings",
                "wing: gold_wings",
                "wing: emerald_wings",
                "wing: diamond_wings"
        ));
        defaultCosmetics.put("Developer", java.util.Arrays.asList(
                "trail: staff_glow",
                "particle: starfall",
                "trail: arc_prism",
                "particle: crystal_bloom",
                "trail: ember_run",
                "particle: spark_burst",
                "trail: aether_aura",
                "particle: rainbow_rush",
                "wing: staff_wings",
                "wing: copper_wings",
                "wing: iron_wings",
                "wing: gold_wings",
                "wing: emerald_wings",
                "wing: diamond_wings"
        ));
        defaultCosmetics.put("Copper", java.util.Arrays.asList("trail: ember_run", "particle: spark_burst", "wing: copper_wings"));
        defaultCosmetics.put("Iron", java.util.Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "wing: iron_wings"));
        defaultCosmetics.put("Gold", java.util.Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "wing: gold_wings"));
        defaultCosmetics.put("Emerald", java.util.Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "trail: emerald_swirl", "particle: emerald_bloom", "wing: emerald_wings"));
        defaultCosmetics.put("Diamond", java.util.Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "trail: emerald_swirl", "particle: emerald_bloom", "trail: arc_prism", "particle: crystal_bloom", "wing: diamond_wings"));

        String[][] defaults = new String[][] {
            {"Admin", "&7[&4&lADMIN&r&7] &r", "Mod", "essentials.ban,essentials.kick", "10"},
            {"Mod", "&7[&6&lMOD&r&7] &r", "Diamond", "essentials.mute,essentials.kick", "20"},
            {"Developer", "&7[&d&lDEV&r&7] &r", "Admin", "*", "1"},
            {"Copper", "&8[&6Copper&8] &r", "", "", "100"},
            {"Iron", "&8[&7Iron&8] &r", "Copper", "", "200"},
            {"Gold", "&8[&eGold&8] &r", "Iron", "", "300"},
            {"Emerald", "&8[&aEmerald&8] &r", "Gold", "", "400"},
            {"Diamond", "&8[&bDiamond&8] &r", "Emerald", "badapple.nickname", "500"}
        };

        for (String[] group : defaults) {
            String groupName = group[0];
            File groupFile = new File(getGroupsFolder(), groupName + ".yml");
            YamlConfiguration yaml = groupFile.exists() ? YamlConfiguration.loadConfiguration(groupFile) : new YamlConfiguration();

            List<String> defaultValues = defaultCosmetics.getOrDefault(groupName, java.util.Collections.emptyList());
            if (!groupFile.exists()) {
                yaml.set("prefix", group[1]);
                yaml.set("inherits", group[2]);
                yaml.set("weight", Integer.parseInt(group[4]));
                yaml.set("permissions", parsePermissions(group[3]));
                yaml.set("cosmetics", defaultValues);
            } else {
                if (yaml.getString("prefix") == null || yaml.getString("prefix").trim().isEmpty()) {
                    yaml.set("prefix", group[1]);
                }
                if (yaml.getString("inherits") == null || yaml.getString("inherits").trim().isEmpty()) {
                    yaml.set("inherits", group[2]);
                }
                if (yaml.getInt("weight", 0) <= 0) {
                    yaml.set("weight", Integer.parseInt(group[4]));
                }
                if (yaml.getStringList("permissions").isEmpty() && !group[3].trim().isEmpty()) {
                    yaml.set("permissions", parsePermissions(group[3]));
                }
                List<String> currentCosmetics = yaml.getStringList("cosmetics");
                yaml.set("cosmetics", mergeCosmetics(defaultValues, currentCosmetics));
            }

            List<String> deduped = dedupePreserveOrder(yaml.getStringList("cosmetics"));
            yaml.set("cosmetics", deduped);

            try {
                yaml.save(groupFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to sync default metadata for group: " + groupName, e);
            }
        }
    }

    private static List<String> mergeCosmetics(List<String> defaults, List<String> current) {
        List<String> merged = new ArrayList<>();
        if (defaults != null) {
                merged.addAll(defaults);
        }
        if (current != null) {
                for (String cosmetic : current) {
                    if (cosmetic != null && !cosmetic.trim().isEmpty() && !merged.contains(cosmetic.trim())) {
                        merged.add(cosmetic.trim());
                    }
                }
        }
        return dedupePreserveOrder(merged);
    }

    private static List<String> dedupePreserveOrder(List<String> values) {
        List<String> cleaned = new ArrayList<>();
        if (values == null) {
                return cleaned;
        }
        for (String value : values) {
                String trimmed = value == null ? "" : value.trim();
                if (!trimmed.isEmpty() && !cleaned.contains(trimmed)) {
                    cleaned.add(trimmed);
                }
        }
        return cleaned;
    }

    private static List<String> parsePermissions(String rawPermissions) {
        List<String> permissions = new ArrayList<>();
        if (rawPermissions == null || rawPermissions.trim().isEmpty()) {
            return permissions;
        }
        for (String permission : rawPermissions.split(",")) {
            String trimmed = permission.trim();
            if (!trimmed.isEmpty()) {
                permissions.add(trimmed);
            }
        }
        return permissions;
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
        yaml.set("weight", 100);
        yaml.set("permissions", new ArrayList<String>());
        yaml.set("cosmetics", new ArrayList<String>());

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

    public static void setGroupWeight(String groupName, int weight) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null) {
            return;
        }

        yaml.set("weight", Math.max(1, weight));
        saveYamlFile(groupName, yaml);
    }

    public static int getGroupWeight(String groupName) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null) {
            return 100;
        }
        return Math.max(1, yaml.getInt("weight", 100));
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

    public static List<String> getCosmetics(String groupName) {
        YamlConfiguration yaml = loadGroupFile(groupName);
        if (yaml == null) {
            return new ArrayList<>();
        }
        List<String> cosmetics = yaml.getStringList("cosmetics");
        return cosmetics == null ? new ArrayList<>() : new ArrayList<>(cosmetics);
    }

    public static List<String> getEffectiveCosmetics(String groupName) {
        return getEffectiveCosmetics(groupName, new HashSet<String>());
    }

    private static List<String> getEffectiveCosmetics(String groupName, Set<String> visited) {
        String group = resolveGroupName(groupName);
        if (group == null) {
            return new ArrayList<>();
        }

        if (!visited.add(group.toLowerCase(Locale.ROOT))) {
            return new ArrayList<>();
        }

        List<String> effective = new ArrayList<>();
        String inherited = getParentGroup(group);
        if (!inherited.isEmpty()) {
            effective.addAll(getEffectiveCosmetics(inherited, new HashSet<>(visited)));
        }

        if (group.equalsIgnoreCase("Admin") || group.equalsIgnoreCase("Mod") || group.equalsIgnoreCase("Developer") || group.equalsIgnoreCase("Dev")) {
            for (String otherGroup : getGroups()) {
                if (!otherGroup.equalsIgnoreCase(group)) {
                    effective.addAll(getEffectiveCosmetics(otherGroup, new HashSet<>(visited)));
                }
            }
        }

        for (String cosmetic : dedupePreserveOrder(getCosmetics(group))) {
            if (!effective.contains(cosmetic)) {
                effective.add(cosmetic);
            }
        }

        return dedupePreserveOrder(effective);
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

    public static File getTracksFolder() {
        return new File(getPluginInstance().getDataFolder(), "tracks");
    }

    public static void ensureTracksLoaded() {
        File tracksFolder = getTracksFolder();
        if (!tracksFolder.exists()) {
            tracksFolder.mkdirs();
        }

        if (getTracks().isEmpty()) {
            createTrack("staff", new ArrayList<>(java.util.Arrays.asList("Diamond", "Mod", "Admin", "Developer")));
        } else {
            String staffTrack = resolveTrackName("staff");
            if (staffTrack != null) {
                List<String> staffGroups = new ArrayList<>(java.util.Arrays.asList("Diamond", "Mod", "Admin", "Developer"));
                YamlConfiguration yaml = loadTrackFile(staffTrack);
                if (yaml != null) {
                    List<String> groups = yaml.getStringList("groups");
                    for (String group : staffGroups) {
                        if (!groups.contains(group)) {
                            groups.add(group);
                        }
                    }
                    yaml.set("groups", groups);
                    saveTrackFile(staffTrack, yaml);
                    applyTrackInheritance(staffTrack, groups);
                }
            }
        }

        if (resolveTrackName("users") == null) {
            createTrack("users", new ArrayList<>(java.util.Arrays.asList("Copper", "Iron", "Gold", "Emerald", "Diamond")));
        }
    }

    public static List<String> getTracks() {
        File tracksFolder = getTracksFolder();
        if (!tracksFolder.exists()) {
            return new ArrayList<>();
        }

        List<String> tracks = new ArrayList<>();
        File[] files = tracksFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return tracks;
        }
        for (File file : files) {
            tracks.add(file.getName().substring(0, file.getName().length() - 4));
        }
        return tracks;
    }

    public static String normalizeTrackName(String trackName) {
        return trackName == null ? "" : trackName.trim();
    }

    public static String resolveTrackName(String trackName) {
        String normalized = normalizeTrackName(trackName);
        if (normalized.isEmpty()) {
            return null;
        }
        for (String track : getTracks()) {
            if (track.equalsIgnoreCase(normalized)) {
                return track;
            }
        }
        return null;
    }

    public static YamlConfiguration loadTrackFile(String trackName) {
        String track = resolveTrackName(trackName);
        if (track == null) {
            return null;
        }
        File trackFile = new File(getTracksFolder(), track + ".yml");
        if (!trackFile.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(trackFile);
    }

    public static void createTrack(String trackName, List<String> groups) {
        String track = normalizeTrackName(trackName);
        if (track.isEmpty() || resolveTrackName(track) != null) {
            return;
        }

        List<String> orderedGroups = new ArrayList<>();
        for (String group : groups) {
            String resolved = resolveGroupName(group);
            if (resolved != null && !orderedGroups.contains(resolved)) {
                orderedGroups.add(resolved);
            }
        }

        File trackFile = new File(getTracksFolder(), track + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups", orderedGroups);
        try {
            yaml.save(trackFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create track: " + track, e);
        }

        if (!orderedGroups.isEmpty()) {
            for (int i = 1; i < orderedGroups.size(); i++) {
                String parent = orderedGroups.get(i - 1);
                String child = orderedGroups.get(i);
                setParentGroup(child, parent);
            }
        }
    }

    public static void addGroupToTrack(String trackName, String groupName) {
        String track = resolveTrackName(trackName);
        if (track == null) {
            return;
        }
        String group = resolveGroupName(groupName);
        if (group == null) {
            return;
        }

        YamlConfiguration yaml = loadTrackFile(track);
        if (yaml == null) {
            return;
        }

        List<String> groups = yaml.getStringList("groups");
        if (!groups.contains(group)) {
            groups.add(group);
            yaml.set("groups", groups);
            saveTrackFile(track, yaml);
            applyTrackInheritance(track, groups);
        }
    }

    public static void removeGroupFromTrack(String trackName, String groupName) {
        String track = resolveTrackName(trackName);
        if (track == null) {
            return;
        }
        String group = resolveGroupName(groupName);
        if (group == null) {
            return;
        }

        YamlConfiguration yaml = loadTrackFile(track);
        if (yaml == null) {
            return;
        }

        List<String> groups = yaml.getStringList("groups");
        groups.remove(group);
        yaml.set("groups", groups);
        saveTrackFile(track, yaml);
        applyTrackInheritance(track, groups);
    }

    public static List<String> getTrackGroups(String trackName) {
        YamlConfiguration yaml = loadTrackFile(trackName);
        if (yaml == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(yaml.getStringList("groups"));
    }

    public static String getTrackForGroup(String groupName) {
        String group = resolveGroupName(groupName);
        if (group == null) {
            return "";
        }
        for (String track : getTracks()) {
            if (getTrackGroups(track).contains(group)) {
                return track;
            }
        }
        return "";
    }

    public static void applyTrackInheritance(String trackName, List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        for (int i = 1; i < groups.size(); i++) {
            String previous = resolveGroupName(groups.get(i - 1));
            String current = resolveGroupName(groups.get(i));
            if (previous != null && current != null) {
                setParentGroup(current, previous);
            }
        }

        for (String group : groups) {
            String resolved = resolveGroupName(group);
            if (resolved == null) {
                continue;
            }
            String parent = getParentGroup(resolved);
            if (parent.isEmpty()) {
                continue;
            }
            if (groups.contains(parent)) {
                continue;
            }
            String parentTrack = getTrackForGroup(parent);
            if (!parentTrack.isEmpty() && !parentTrack.equalsIgnoreCase(trackName)) {
                continue;
            }
            setParentGroup(resolved, "");
        }
    }

    public static void saveTrackFile(String trackName, YamlConfiguration yaml) {
        String track = resolveTrackName(trackName);
        if (track == null) {
            return;
        }

        File trackFile = new File(getTracksFolder(), track + ".yml");
        try {
            yaml.save(trackFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save track file: " + track, e);
        }
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
