package com.commandercortex.badApple.Local.Folders;

import com.commandercortex.badApple.BadApple;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to create plugin folders and deploy default configuration/group files.
 */
public class SetupFolders {

    String main = "plugins/BadApple/";
    String configFolder = "plugins/BadApple/config/";
    String players = "plugins/BadApple/players/";
    String groups = "plugins/BadApple/groups/";

    public int CreateMainFolder(){
        File _main = new File(main);
        if(!_main.exists())
            _main.mkdir();
        System.out.println("[BadApple] -> Verifying integrity of: \"plugins/BadApple/\"");


        return 0;
    }

    public int CreateConfigFolder(){
        File _config = new File(configFolder);
        if(!_config.exists())
            _config.mkdirs();
        System.out.println("[BadApple] -> Verifying integrity of: \"plugins/BadApple/config/\"");

        File mainConfig = new File("plugins/BadApple/config.yml");
        File userConfig = new File(configFolder + "config.yml");
        if (mainConfig.exists() && !userConfig.exists()) {
            try {
                Files.copy(mainConfig.toPath(), userConfig.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create user-editable config copy.", e);
            }
        }

        File pluginManifest = new File(configFolder + "plugin.yml");
        if (!pluginManifest.exists()) {
            try (java.io.InputStream input = BadApple.getPlugin(BadApple.class).getResource("plugin.yml");
                 java.io.OutputStream output = Files.newOutputStream(pluginManifest.toPath())) {
                if (input == null) {
                    throw new IOException("plugin.yml resource was not found.");
                }
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create user-editable plugin.yml copy.", e);
            }
        }

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

        Map<String, java.util.List<String>> defaultCosmetics = new HashMap<>();
        defaultCosmetics.put("Admin", Arrays.asList(
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
        defaultCosmetics.put("Mod", Arrays.asList(
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
        defaultCosmetics.put("Developer", Arrays.asList(
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
        defaultCosmetics.put("Copper", Arrays.asList("trail: ember_run", "particle: spark_burst", "wing: copper_wings"));
        defaultCosmetics.put("Iron", Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "wing: iron_wings"));
        defaultCosmetics.put("Gold", Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "wing: gold_wings"));
        defaultCosmetics.put("Emerald", Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "trail: emerald_swirl", "particle: emerald_bloom", "wing: emerald_wings"));
        defaultCosmetics.put("Diamond", Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "trail: emerald_swirl", "particle: emerald_bloom", "trail: arc_prism", "particle: crystal_bloom", "wing: diamond_wings"));

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
            File groupFile = new File(groupsFolder, group[0] + ".yml");
            if (groupFile.exists()) {
                continue;
            }

            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("prefix", group[1]);
            yaml.set("inherits", group[2]);
            yaml.set("weight", Integer.parseInt(group[4]));
            yaml.set("permissions", parsePermissions(group[3]));
            yaml.set("cosmetics", defaultCosmetics.getOrDefault(group[0], java.util.Collections.emptyList()));

            try {
                yaml.save(groupFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create default group file: " + groupFile.getName(), e);
            }
        }

        File trackFolder = new File("plugins/BadApple/tracks");
        if (!trackFolder.exists()) {
            trackFolder.mkdirs();
        }

        File staffTrack = new File(trackFolder, "staff.yml");
        if (!staffTrack.exists()) {
            YamlConfiguration staffYaml = new YamlConfiguration();
                    staffYaml.set("groups", java.util.Arrays.asList("Mod", "Admin", "Developer"));
            try {
                staffYaml.save(staffTrack);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create default staff track.", e);
            }
        }

        File userTrack = new File(trackFolder, "users.yml");
        if (!userTrack.exists()) {
            YamlConfiguration userYaml = new YamlConfiguration();
            userYaml.set("groups", java.util.Arrays.asList("Copper", "Iron", "Gold", "Emerald", "Diamond"));
            try {
                userYaml.save(userTrack);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create default users track.", e);
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
