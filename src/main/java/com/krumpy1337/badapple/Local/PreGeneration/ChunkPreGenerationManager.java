package com.krumpy1337.badapple.Local.PreGeneration;

import com.krumpy1337.badapple.BadApple;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;

/** Disk-backed chunk pre-generation task with bounded synchronous throughput. */
public final class ChunkPreGenerationManager {
    private final BadApple plugin;
    private final File stateFile;
    private BukkitTask runner;
    private String worldName;
    private int centerX;
    private int centerZ;
    private int radius;
    private int index;
    private boolean paused;
    private boolean keepLoaded;

    public ChunkPreGenerationManager(BadApple plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "pregeneration.yml");
        load();
    }

    public void start(World world, int centerX, int centerZ, int radius) {
        if (world == null) return;
        stopTask();
        this.worldName = world.getName();
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = clampRadius(radius);
        this.index = 0;
        this.paused = false;
        save();
        startRunner();
    }

    public void pause() { paused = true; stopTask(); save(); }

    public void resume() {
        if (worldName == null || worldName.isBlank()) return;
        paused = false;
        save();
        startRunner();
    }

    public void cancel() {
        stopTask();
        worldName = null;
        centerX = 0;
        centerZ = 0;
        radius = 0;
        index = 0;
        paused = false;
        save();
    }

    public void setRadius(int requestedRadius) {
        radius = clampRadius(requestedRadius);
        save();
    }

    public void setKeepLoaded(boolean keepLoaded) { this.keepLoaded = keepLoaded; save(); }
    public void shutdown() { stopTask(); save(); }
    public boolean isRunning() { return runner != null && !runner.isCancelled() && !paused; }
    public boolean isPaused() { return paused && worldName != null; }
    public boolean hasTask() { return worldName != null && !worldName.isBlank(); }
    public String getWorldName() { return worldName; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public int getRadius() { return radius; }
    public int getProgress() { return index; }
    public int getTotal() { return hasTask() ? (radius * 2 + 1) * (radius * 2 + 1) : 0; }
    public boolean isKeepLoaded() { return keepLoaded; }
    public double getPercentComplete() { int total = getTotal(); return total == 0 ? 0.0 : index * 100.0 / total; }

    public void resumePersistedTask() {
        if (hasTask() && !paused) startRunner();
    }

    private void startRunner() {
        stopTask();
        World world = Bukkit.getWorld(worldName);
        if (world == null) { paused = true; save(); return; }
        int chunksPerTick = Math.max(1, plugin.getConfig().getInt("pregeneration.chunks-per-tick", 2));
        runner = Bukkit.getScheduler().runTaskTimer(plugin, () -> process(world, chunksPerTick), 20L, 1L);
    }

    private void process(World world, int chunksPerTick) {
        if (paused) return;
        int total = getTotal();
        for (int i = 0; i < chunksPerTick && index < total; i++) {
            int side = radius * 2 + 1;
            int dx = (index % side) - radius;
            int dz = (index / side) - radius;
            int chunkX = centerX + dx;
            int chunkZ = centerZ + dz;
            world.loadChunk(chunkX, chunkZ, true);
            if (!keepLoaded) world.unloadChunkRequest(chunkX, chunkZ);
            index++;
        }

        if (index >= total) {
            stopTask();
            paused = false;
            save();
            plugin.getLogger().info("Chunk pre-generation completed for " + worldName + " (" + total + " chunks).");
        } else if ((index % 100) == 0) {
            save();
        }
    }

    private void stopTask() {
        if (runner != null) { runner.cancel(); runner = null; }
    }

    private int clampRadius(int requested) {
        int max = Math.max(1, plugin.getConfig().getInt("pregeneration.max-radius", 2048));
        return Math.max(1, Math.min(requested, max));
    }

    private void load() {
        if (!stateFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        worldName = yaml.getString("world");
        centerX = yaml.getInt("center-x", 0);
        centerZ = yaml.getInt("center-z", 0);
        radius = yaml.getInt("radius", 0);
        index = yaml.getInt("progress", 0);
        paused = yaml.getBoolean("paused", true);
        keepLoaded = yaml.getBoolean("keep-loaded", false);
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        if (worldName != null) {
            yaml.set("world", worldName);
            yaml.set("center-x", centerX);
            yaml.set("center-z", centerZ);
            yaml.set("radius", radius);
            yaml.set("progress", index);
            yaml.set("paused", paused);
        } else {
            yaml.set("world", null);
            yaml.set("progress", 0);
        }
        yaml.set("keep-loaded", keepLoaded);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save chunk pre-generation state: " + exception.getMessage());
        }
    }
}
