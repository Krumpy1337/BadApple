package com.krumpy1337.badapple.Local.Folders;

import com.krumpy1337.badapple.BadApple;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Creates and verifies the plugin's YAML storage layout. */
public final class SetupFolders {
    private final BadApple plugin;

    public SetupFolders(BadApple plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        this.plugin = plugin;
    }

    public int CreateMainFolder() { return ensureFolder(plugin.getDataFolder()) ? 0 : 1; }

    public int CreateConfigFolder() {
        File folder = new File(plugin.getDataFolder(), "config");
        ensureFolder(folder);
        copyDefaultConfig(folder);
        copyPluginManifest(folder);
        return 0;
    }

    public int CreatePlayersFolder() { ensureFolder(new File(plugin.getDataFolder(), "players")); return 0; }

    public int CreateGroupsFolder() { ensureFolder(new File(plugin.getDataFolder(), "groups")); return 0; }

    public void CreateDefaultGroups() {
        createDefaultGroups();
        createDefaultTracks();
    }

    private boolean ensureFolder(File folder) {
        if (folder.exists()) return true;
        if (folder.mkdirs()) {
            plugin.getLogger().info("Created folder: " + folder.getPath());
            return true;
        }
        plugin.getLogger().warning("Could not create folder: " + folder.getPath());
        return false;
    }

    private void copyDefaultConfig(File folder) {
        File mainConfig = new File(plugin.getDataFolder(), "config.yml");
        File userConfig = new File(folder, "config.yml");
        if (mainConfig.exists() && !userConfig.exists()) {
            try { java.nio.file.Files.copy(mainConfig.toPath(), userConfig.toPath()); }
            catch (IOException exception) { throw new IllegalStateException("Failed to create user-editable config copy", exception); }
        }
    }

    private void copyPluginManifest(File folder) {
        File manifest = new File(folder, "plugin.yml");
        if (manifest.exists()) return;
        try (java.io.InputStream input = plugin.getResource("plugin.yml");
             java.io.OutputStream output = java.nio.file.Files.newOutputStream(manifest.toPath())) {
            if (input == null) throw new IOException("plugin.yml resource was not found");
            input.transferTo(output);
        } catch (IOException exception) { throw new IllegalStateException("Failed to create plugin.yml copy", exception); }
    }

    private void createDefaultGroups() {
        File folder = new File(plugin.getDataFolder(), "groups");
        ensureFolder(folder);
        Map<String, List<String>> cosmetics = new LinkedHashMap<>();
        cosmetics.put("Admin", Arrays.asList("trail: staff_glow", "particle: starfall", "trail: arc_prism", "particle: crystal_bloom", "trail: ember_run", "particle: spark_burst", "wing: staff_wings", "wing: copper_wings", "wing: iron_wings", "wing: gold_wings", "wing: emerald_wings", "wing: diamond_wings"));
        cosmetics.put("Mod", cosmetics.get("Admin"));
        cosmetics.put("Developer", Arrays.asList("trail: staff_glow", "particle: starfall", "trail: arc_prism", "particle: crystal_bloom", "trail: ember_run", "particle: spark_burst", "trail: aether_aura", "particle: rainbow_rush", "wing: staff_wings", "wing: copper_wings", "wing: iron_wings", "wing: gold_wings", "wing: emerald_wings", "wing: diamond_wings"));
        cosmetics.put("Copper", Arrays.asList("trail: ember_run", "particle: spark_burst", "wing: copper_wings"));
        cosmetics.put("Iron", Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "wing: iron_wings"));
        cosmetics.put("Gold", Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "wing: gold_wings"));
        cosmetics.put("Emerald", Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "trail: emerald_swirl", "particle: emerald_bloom", "wing: emerald_wings"));
        cosmetics.put("Diamond", Arrays.asList("trail: ember_run", "particle: spark_burst", "trail: mist_vapor", "particle: ring_of_fate", "trail: gold_dust", "particle: coin_burst", "trail: emerald_swirl", "particle: emerald_bloom", "trail: arc_prism", "particle: crystal_bloom", "wing: diamond_wings"));

        String[][] defaults = {
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
            File file = new File(folder, group[0] + ".yml");
            if (file.exists()) continue;
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("prefix", group[1]);
            yaml.set("inherits", group[2]);
            yaml.set("weight", Integer.parseInt(group[4]));
            yaml.set("permissions", parsePermissions(group[3]));
            yaml.set("cosmetics", cosmetics.getOrDefault(group[0], Collections.emptyList()));
            save(yaml, file, "default group " + group[0]);
        }
    }

    private void createDefaultTracks() {
        File folder = new File(plugin.getDataFolder(), "tracks");
        ensureFolder(folder);
        createTrack(folder, "staff.yml", Arrays.asList("Mod", "Admin", "Developer"));
        createTrack(folder, "users.yml", Arrays.asList("Copper", "Iron", "Gold", "Emerald", "Diamond"));
    }

    private void createTrack(File folder, String fileName, List<String> groups) {
        File file = new File(folder, fileName);
        if (file.exists()) return;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups", groups);
        save(yaml, file, "default track " + fileName);
    }

    private void save(YamlConfiguration yaml, File file, String description) {
        try { yaml.save(file); }
        catch (IOException exception) { throw new IllegalStateException("Failed to create " + description, exception); }
    }

    private List<String> parsePermissions(String rawPermissions) {
        if (rawPermissions == null || rawPermissions.isBlank()) return Collections.emptyList();
        return Arrays.stream(rawPermissions.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }
}
