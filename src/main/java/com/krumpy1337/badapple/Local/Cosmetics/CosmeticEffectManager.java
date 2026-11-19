package com.krumpy1337.badapple.Local.Cosmetics;

import com.krumpy1337.badapple.BadApple;
import com.krumpy1337.badapple.Local.Permissions.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * Manages scheduled cosmetic effects (trails, particles, wings) for players with enabled cosmetics.
 */
public final class CosmeticEffectManager {
    private final BadApple plugin;
    private final GroupManager groupManager = new GroupManager();

    public CosmeticEffectManager(BadApple plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String active = groupManager.getEnabledCosmetics(player).stream().findFirst().orElse("");
                if (active.isEmpty()) {
                    continue;
                }
                if (active.startsWith("trail:")) {
                    spawnTrail(player, active.substring("trail:".length()).trim().toLowerCase(Locale.ROOT));
                } else if (active.startsWith("particle:")) {
                    spawnParticle(player, active.substring("particle:".length()).trim().toLowerCase(Locale.ROOT));
                } else if (active.startsWith("wing:")) {
                    spawnWings(player, active.substring("wing:".length()).trim().toLowerCase(Locale.ROOT));
                }
            }
        }, 0L, 2L);
    }

    private void spawnTrail(Player player, String style) {
        Location location = player.getLocation().clone().add(0, 0.2, 0);
        Vector direction = player.getLocation().getDirection();

        double offsetX = direction.getX() * 0.4;
        double offsetZ = direction.getZ() * 0.4;
        Location trailLocation = location.clone().subtract(offsetX, 0.15, offsetZ);

        Particle particle = resolveParticle(style, true);
        if (particle == null) {
            return;
        }

        player.getWorld().spawnParticle(
                particle,
                trailLocation,
                4,
                0.15D,
                0.05D,
                0.15D,
                0.02D
        );
    }

    private void spawnParticle(Player player, String style) {
        Particle particle = resolveParticle(style, false);
        if (particle == null) {
            return;
        }

        Location location = player.getLocation().clone().add(0, 0.8, 0);
        player.getWorld().spawnParticle(
                particle,
                location,
                8,
                0.35D,
                0.45D,
                0.35D,
                0.02D
        );
    }

    private void spawnWings(Player player, String style) {
        Particle wingParticle = resolveWingParticle(style);
        if (wingParticle == null) {
            return;
        }

        Location origin = player.getLocation().clone().add(0, 1.15, 0);
        Vector forward = player.getLocation().getDirection().normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        Vector up = new Vector(0, 1, 0);

        for (int side : new int[] { -1, 1 }) {
            for (int i = 0; i < 14; i++) {
                double progress = i / 13.0D;
                double spread = Math.sin(progress * Math.PI);
                double lift = Math.sin(progress * Math.PI * 0.8D) * 0.9D;
                double depth = -0.65D + (progress * 1.5D);

                Vector offset = forward.clone().multiply(depth)
                        .add(right.clone().multiply(side * (0.5D + spread * 0.95D)))
                        .add(up.clone().multiply(lift));

                Location particleLocation = origin.clone().add(offset);
                particleLocation.getWorld().spawnParticle(wingParticle, particleLocation, 1, 0, 0, 0, 0);
            }
        }

        for (int i = 0; i < 8; i++) {
            double t = i / 7.0D;
            double arc = 0.2D + t * 0.9D;
            Vector offset = forward.clone().multiply(0.2D + t * 1.1D)
                    .add(right.clone().multiply(arc))
                    .add(up.clone().multiply(Math.sin(t * Math.PI) * 0.7D));
            Location particleLocation = origin.clone().add(offset);
            particleLocation.getWorld().spawnParticle(wingParticle, particleLocation, 1, 0, 0, 0, 0);
        }
    }

    private Particle resolveWingParticle(String style) {
        switch (style) {
            case "staff_wings":
                return Particle.END_ROD;
            case "copper_wings":
                return Particle.FLAME;
            case "iron_wings":
                return Particle.CLOUD;
            case "gold_wings":
                return Particle.NOTE;
            case "emerald_wings":
                return Particle.GLOW_SQUID_INK;
            case "diamond_wings":
                return Particle.ENCHANT;
            default:
                return Particle.END_ROD;
        }
    }

    private Particle resolveParticle(String style, boolean trailMode) {
        switch (style) {
            case "staff_glow":
                return trailMode ? Particle.END_ROD : Particle.ELECTRIC_SPARK;
            case "arc_prism":
                return trailMode ? Particle.ENCHANT : Particle.END_ROD;
            case "ember_run":
                return trailMode ? Particle.FLAME : Particle.FLAME;
            case "mist_vapor":
                return trailMode ? Particle.CAMPFIRE_COSY_SMOKE : Particle.CAMPFIRE_SIGNAL_SMOKE;
            case "gold_dust":
                return trailMode ? Particle.NOTE : Particle.NOTE;
            case "emerald_swirl":
                return trailMode ? Particle.GLOW_SQUID_INK : Particle.GLOW_SQUID_INK;
            case "aether_aura":
                return trailMode ? Particle.TOTEM_OF_UNDYING : Particle.TOTEM_OF_UNDYING;
            case "starfall":
                return trailMode ? Particle.CRIT : Particle.CRIT;
            case "spark_burst":
                return trailMode ? Particle.CRIT : Particle.FIREWORK;
            case "ring_of_fate":
                return trailMode ? Particle.SONIC_BOOM : Particle.SONIC_BOOM;
            case "coin_burst":
                return trailMode ? Particle.FIREWORK : Particle.FIREWORK;
            case "crystal_bloom":
                return trailMode ? Particle.SCULK_SOUL : Particle.SCULK_SOUL;
            case "rainbow_rush":
                return trailMode ? Particle.SOUL_FIRE_FLAME : Particle.SOUL_FIRE_FLAME;
            default:
                return trailMode ? Particle.END_ROD : Particle.CRIT;
        }
    }
}
