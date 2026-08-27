package com.commandercortex.badApple.Local.Folders;

import com.commandercortex.badApple.BadApple;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SetupFolders {

    String main = "plugins/BadApple/";
    String players = "plugins/BadApple/players/";
    String groups = "plugins/BadApple/groups/";

    public int CreateMainFolder(){
        File _main = new File(main);
        if(!_main.exists())
            _main.mkdir();
        System.out.println("[BadApple] -> Verifying integrity of: \"plugins/BadApple/\"");

        return 0;
    }
    public int CreatePlayersFolder(){
        File _players = new File(players);
        if(!_players.exists())
            _players.mkdir();
        System.out.println("[BadApple] -> Verifying integrity of: \"plugins/BadApple/players/\"");

        return 0;
    }
    public int CreateGroupsFolder(){
        File _groups = new File(groups);
        if(!_groups.exists())
            _groups.mkdirs();
        System.out.println("[BadApple] -> Verifying integrity of: \"plugins/BadApple/groups/\"");

        return 0;
    }
    public void CreateDefaultGroups(){
        File groupsFolder = new File(groups);
        if(!groupsFolder.exists()) {
            groupsFolder.mkdirs();
        }

        String[][] defaults = new String[][] {
            {"Admin", "&7[&4&lADMIN&r&7] &r", "", "essentials.ban,essentials.kick"},
            {"Mod", "&7[&6&lMOD&r&7] &r", "", "essentials.mute,essentials.kick"},
            {"Developer", "&7[&d&lDEV&r&7] &r", "Admin", "*"}
        };

        for (String[] group : defaults) {
            File groupFile = new File(groupsFolder, group[0] + ".yml");
            if (groupFile.exists()) {
                continue;
            }

            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("prefix", group[1]);
            yaml.set("inherits", group[2]);
            yaml.set("permissions", parsePermissions(group[3]));

            try {
                yaml.save(groupFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create default group file: " + groupFile.getName(), e);
            }
        }
    }

    private java.util.List<String> parsePermissions(String rawPermissions) {
        java.util.List<String> permissions = new java.util.ArrayList<>();
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
}
