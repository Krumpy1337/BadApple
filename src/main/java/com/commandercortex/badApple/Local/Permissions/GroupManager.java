package com.commandercortex.badApple.Local.Permissions;

import com.commandercortex.badApple.Global.ColorUtil;
import com.commandercortex.badApple.Local.Players.Data.GetPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public void setParentGroup(String group, String parentGroup) {
        GroupConfig.setParentGroup(group, parentGroup);
    }

    public String getParentGroup(String group) {
        return GroupConfig.getParentGroup(group);
    }

    public List<String> getGroups() {
        return GroupConfig.getGroups();
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
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();

            for (Player player : Bukkit.getOnlinePlayers()) {
                Team team = scoreboard.registerNewTeam(player.getName());
                team.setPrefix(groupPrefix(getGroup(player)));
                team.addEntry(player.getName());
            }

            Objective objective = scoreboard.registerNewObjective("badapple_tab", "dummy", "§b§lBadApple");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            String groupName = getGroup(viewer);
            String displayGroup = groupName == null || groupName.isEmpty() ? "No Group" : groupName;
            int coins = getCoins(viewer);

            objective.getScore("§7Group").setScore(3);
            objective.getScore("§f" + displayGroup).setScore(2);
            objective.getScore("§7Coins").setScore(1);
            objective.getScore("§a" + coins).setScore(0);

            viewer.setScoreboard(scoreboard);
        }
    }
}
