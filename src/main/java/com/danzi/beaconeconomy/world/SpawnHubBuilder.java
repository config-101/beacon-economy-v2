package com.danzi.beaconeconomy.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SpawnHubBuilder {
    private SpawnHubBuilder() {}

    public static void ensureHub(World world, Location center) {
        int cx = center.getBlockX();
        int cy = center.getBlockY() - 1;
        int cz = center.getBlockZ();

        for (int x = -18; x <= 18; x++) {
            for (int z = -18; z <= 18; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 18) continue;
                set(world, cx + x, cy, cz + z, dist < 6 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.SMOOTH_QUARTZ);
                if (dist > 15 && dist <= 18) {
                    set(world, cx + x, cy + 1, cz + z, Material.POLISHED_BLACKSTONE_WALL);
                }
            }
        }

        // Cross paths.
        for (int i = -18; i <= 18; i++) {
            set(world, cx + i, cy, cz, Material.CRYING_OBSIDIAN);
            set(world, cx, cy, cz + i, Material.CRYING_OBSIDIAN);
        }

        // Beacon pedestal.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(world, cx + x, cy + 1, cz + z, Material.QUARTZ_BLOCK);
            }
        }
        set(world, cx, cy + 2, cz, Material.BEACON);
        set(world, cx, cy + 3, cz, Material.LIGHT);

        // Tutorial markers.
        set(world, cx + 8, cy + 1, cz, Material.ENCHANTING_TABLE);
        set(world, cx - 8, cy + 1, cz, Material.ENDER_CHEST);
        set(world, cx, cy + 1, cz + 8, Material.EMERALD_BLOCK);
        set(world, cx, cy + 1, cz - 8, Material.AMETHYST_BLOCK);
    }

    private static void set(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != material) {
            block.setType(material, false);
        }
    }
}
