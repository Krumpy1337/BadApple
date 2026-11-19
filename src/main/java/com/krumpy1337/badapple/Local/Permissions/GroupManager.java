package com.krumpy1337.badapple.Local.Permissions;

import com.krumpy1337.badapple.Global.ColorUtil;
import com.krumpy1337.badapple.Local.Players.Data.GetPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Runtime facade for group operations tied to players: get/set group, manage user-specific data and tablist.
 */
public class GroupManager extends Groups {

    public String getGroup(Player player) {
        String playerfile = GetPlayerData.getPlayerData(player);
        File playerData = new File(playerfile);
        if (!playerData.exists()) {
            return "";
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerData);
        String group = yaml.getString("Group:");
        return group == null ? "" : group;
    }

    public void setGroup(Player player, String group) {
        String playerfile = GetPlayerData.getPlayerData(player);
        File playerData = new File(playerfile);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerData);

        yaml.set("Group:", group);
        try {
            yaml.save(playerfile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public YamlConfiguration getPlayerYAML(Player player) {
        String playerfile = GetPlayerData.getPlayerData(player);
        File playerData = new File(playerfile);
        if (!playerData.exists()) {
            try {
                File parent = playerData.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                playerData.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create player data file for " + player.getName(), e);
            }
        }
        return YamlConfiguration.loadConfiguration(playerData);
    }

    public void savePlayerYAML(Player player, YamlConfiguration yaml) {
        String playerfile = GetPlayerData.getPlayerData(player);
        File playerData = new File(playerfile);
        try {
            yaml.save(playerData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save player data for " + player.getName(), e);
        }
    }

    public String getUserSuffix(Player player) {
        if (player == null) {
            return "";
        }
        YamlConfiguration yaml = getPlayerYAML(player);
        String suffix = yaml.getString("custom-suffix", "");
        return suffix == null ? "" : suffix;
    }

    public void setUserSuffix(Player player, String suffix) {
        if (player == null) {
            return;
        }
        YamlConfiguration yaml = getPlayerYAML(player);
        yaml.set("custom-suffix", suffix == null ? "" : suffix);
        savePlayerYAML(player, yaml);
    }

    public List<String> getUserPermissions(Player player) {
        if (player == null) {
            return new ArrayList<>();
        }
        YamlConfiguration yaml = getPlayerYAML(player);
        List<String> permissions = yaml.getStringList("custom-permissions");
        return permissions == null ? new ArrayList<>() : new ArrayList<>(permissions);
    }

    public void addUserPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return;
        }
        String cleaned = permission.trim();
        if (cleaned.isEmpty()) {
            return;
        }
        List<String> permissions = getUserPermissions(player);
        if (!permissions.contains(cleaned)) {
            permissions.add(cleaned);
            YamlConfiguration yaml = getPlayerYAML(player);
            yaml.set("custom-permissions", permissions);
            savePlayerYAML(player, yaml);
        }
    }

    public void removeUserPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return;
        }
        String cleaned = permission.trim();
        if (cleaned.isEmpty()) {
            return;
        }
        List<String> permissions = getUserPermissions(player);
        permissions.remove(cleaned);
        YamlConfiguration yaml = getPlayerYAML(player);
        yaml.set("custom-permissions", permissions);
        savePlayerYAML(player, yaml);
    }

    public String getPlayerDisplayName(Player player) {
        if (player == null) {
            return "";
        }
        String prefix = getGroup(player).isEmpty() ? "" : groupPrefix(getGroup(player));
        String suffix = getUserSuffix(player);
        String formattedSuffix = suffix == null || suffix.trim().isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', suffix.trim());
        return prefix + player.getName() + (formattedSuffix.isEmpty() ? "" : " " + formattedSuffix);
    }

    public boolean groupExists(String group) {
        return GroupConfig.groupExists(group);
    }

    public void createGroup(String group) {
        GroupConfig.createGroup(group);
    }

    public void renameGroup(String oldGroup, String newGroup) {
        GroupConfig.renameGroup(oldGroup, newGroup);
    }

    public void deleteGroup(String group) {
        GroupConfig.deleteGroup(group);
    }

    public void setGroupPrefix(String group, String prefix) {
        GroupConfig.setGroupPrefix(group, prefix);
    }

    public void addPermission(String group, String permission) {
        GroupConfig.addPermission(group, permission);
    }

    public void removePermission(String group, String permission) {
        GroupConfig.removePermission(group, permission);
    }

    public List<String> getPermissions(String group) {
        return GroupConfig.getEffectivePermissions(group);
    }

    public List<String> getCosmetics(String group) {
        return GroupConfig.getEffectiveCosmetics(group);
    }

    public List<String> getPlayerCosmetics(Player player) {
        if (player == null) {
            return new ArrayList<>();
        }
        String group = getGroup(player);
        if (group == null || group.isEmpty()) {
            return new ArrayList<>();
        }
        return GroupConfig.getEffectiveCosmetics(group);
    }

    public List<String> getEnabledCosmetics(Player player) {
        if (player == null) {
            return new ArrayList<>();
        }
        YamlConfiguration yaml = getPlayerYAML(player);
        List<String> enabled = yaml.getStringList("enabled-cosmetics");
        if (enabled == null || enabled.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> normalized = new ArrayList<>();
        for (String cosmetic : enabled) {
            if (cosmetic != null && !cosmetic.trim().isEmpty()) {
                normalized.add(cosmetic.trim());
            }
        }

        if (normalized.size() > 1) {
            normalized = new ArrayList<>(java.util.Collections.singletonList(normalized.get(normalized.size() - 1)));
            yaml.set("enabled-cosmetics", normalized);
            savePlayerYAML(player, yaml);
        }

        return normalized;
    }

    public void setEnabledCosmetics(Player player, List<String> cosmetics) {
        if (player == null) {
            return;
        }

        List<String> normalized = new ArrayList<>();
        if (cosmetics != null) {
            for (String cosmetic : cosmetics) {
                if (cosmetic != null && !cosmetic.trim().isEmpty()) {
                    normalized.add(cosmetic.trim());
                }
            }
        }

        if (normalized.size() > 1) {
            normalized = new ArrayList<>(java.util.Collections.singletonList(normalized.get(normalized.size() - 1)));
        }

        YamlConfiguration yaml = getPlayerYAML(player);
        yaml.set("enabled-cosmetics", normalized);
        savePlayerYAML(player, yaml);
    }

    public void toggleCosmetic(Player player, String cosmetic) {
        if (player == null || cosmetic == null) {
            return;
        }
        List<String> unlocked = getPlayerCosmetics(player);
        String cleaned = cosmetic.trim();
        if (cleaned.isEmpty() || !unlocked.contains(cleaned)) {
            return;
        }

        List<String> active = getEnabledCosmetics(player);
        if (active.contains(cleaned)) {
            setEnabledCosmetics(player, new ArrayList<>());
            return;
        }

        setEnabledCosmetics(player, java.util.Collections.singletonList(cleaned));
    }

    public boolean isCosmeticEnabled(Player player, String cosmetic) {
        if (player == null || cosmetic == null) {
            return false;
        }
        return getEnabledCosmetics(player).contains(cosmetic.trim());
    }

    public void setParentGroup(String group, String parentGroup) {
        GroupConfig.setParentGroup(group, parentGroup);
    }

    public void setGroupWeight(String group, int weight) {
        GroupConfig.setGroupWeight(group, weight);
    }

    public int getGroupWeight(String group) {
        return GroupConfig.getGroupWeight(group);
    }

    public String getParentGroup(String group) {
        return GroupConfig.getParentGroup(group);
    }

    public List<String> getGroups() {
        return GroupConfig.getGroups();
    }

    public List<String> getTracks() {
        return GroupConfig.getTracks();
    }

    public List<String> getTrackGroups(String track) {
        return GroupConfig.getTrackGroups(track);
    }

    public String getTrackForGroup(String group) {
        return GroupConfig.getTrackForGroup(group);
    }

    public void createTrack(String track, List<String> groups) {
        GroupConfig.createTrack(track, groups);
    }

    public void addGroupToTrack(String track, String group) {
        GroupConfig.addGroupToTrack(track, group);
    }

    public void removeGroupFromTrack(String track, String group) {
        GroupConfig.removeGroupFromTrack(track, group);
    }

    public void promotePlayer(Player player, String track) {
        String playerGroup = getGroup(player);
        List<String> groups = GroupConfig.getTrackGroups(track);
        if (playerGroup == null || playerGroup.isEmpty() || groups.isEmpty()) {
            return;
        }

        int index = groups.indexOf(playerGroup);
        if (index >= 0 && index < groups.size() - 1) {
            setGroup(player, groups.get(index + 1));
            TabManager();
        }
    }

    public void demotePlayer(Player player, String track) {
        String playerGroup = getGroup(player);
        List<String> groups = GroupConfig.getTrackGroups(track);
        if (playerGroup == null || playerGroup.isEmpty() || groups.isEmpty()) {
            return;
        }

        int index = groups.indexOf(playerGroup);
        if (index > 0) {
            setGroup(player, groups.get(index - 1));
            TabManager();
        }
    }

    public String groupPrefix(String group) {
        String resolvedGroup = GroupConfig.resolveGroupName(group);
        if (resolvedGroup != null) {
            String prefix = GroupConfig.getGroupPrefix(resolvedGroup);
            if (prefix != null && !prefix.isEmpty()) {
                return ColorUtil.FormatMessage(prefix);
            }
        }

        if (Objects.equals(group, Developer))
            return ColorUtil.FormatMessage("&7[&d&lDEV&r&7] &r");
        if (Objects.equals(group, Admin))
            return ColorUtil.FormatMessage("&7[&4&lADMIN&r&7] &r");
        if (Objects.equals(group, Mod))
            return ColorUtil.FormatMessage("&7[&6&lMOD&r&7] &r");
        return "";
    }

    public int getCoins(Player player) {
        String playerfile = GetPlayerData.getPlayerData(player);
        File playerData = new File(playerfile);
        if (!playerData.exists()) {
            return 0;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerData);
        return yaml.getInt("Coins:", 0);
    }

    public void setCoins(Player player, int coins) {
        String playerfile = GetPlayerData.getPlayerData(player);
        File playerData = new File(playerfile);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerData);

        yaml.set("Coins:", Math.max(0, coins));
        try {
            yaml.save(playerfile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addCoins(Player player, int amount) {
        setCoins(player, getCoins(player) + amount);
    }

    public void TabManager() {
        // Precompute group list sorted by weight
        List<String> groups = new ArrayList<>(getGroups());
        groups.sort((left, right) -> Integer.compare(getGroupWeight(left), getGroupWeight(right)));

        // Map group -> list of player names and suffix map
        Map<String, List<String>> groupToPlayers = new HashMap<>();
        Map<String, String> playerSuffix = new HashMap<>();
        Map<String, String> playerGroup = new HashMap<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String grp = getGroup(p);
            playerGroup.put(p.getName(), grp == null ? "" : grp);
            groupToPlayers.computeIfAbsent(grp == null ? "" : grp, k -> new ArrayList<>()).add(p.getName());
            String suffix = getUserSuffix(p);
            playerSuffix.put(p.getName(), (suffix == null || suffix.trim().isEmpty()) ? "" : ChatColor.translateAlternateColorCodes('&', suffix.trim()));
        }

        // Precompute prefixes per group
        Map<String, String> groupPrefixMap = new HashMap<>();
        for (String group : groups) {
            groupPrefixMap.put(group, groupPrefix(group));
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();

            int teamIndex = 1;
            for (String group : groups) {
                String teamName = String.format("%03d_%s", getGroupWeight(group), group);
                Team team = scoreboard.registerNewTeam(teamName);
                team.setPrefix(groupPrefixMap.getOrDefault(group, ""));

                List<String> members = groupToPlayers.getOrDefault(group, new ArrayList<>());
                for (String name : members) {
                    team.addEntry(name);
                    String formattedSuffix = playerSuffix.getOrDefault(name, "");
                    if (!formattedSuffix.isEmpty()) {
                        team.setSuffix(" " + formattedSuffix);
                    }
                }

                if (teamIndex > 999) {
                    break;
                }
                teamIndex++;
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setPlayerListName(getPlayerDisplayName(p));
            }

            String groupName = playerGroup.getOrDefault(viewer.getName(), "");
            String displayGroup = groupName == null || groupName.isEmpty() ? "No Group" : groupName;
            int coins = getCoins(viewer);

            String header = "\n§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                    + "§b§lBADAPPLE\n"
                    + "§7Group: §f" + displayGroup + "\n"
                    + "§7Coins: §a" + coins + "\n"
                    + "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
            String footer = "\n§7Welcome back, §f" + viewer.getName() + "§7!\n";

            viewer.setPlayerListHeaderFooter(header, footer);
            viewer.setScoreboard(scoreboard);
        }
    }
}
