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

        // Main platform.
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 24) continue;
                Material floor = dist < 5 ? Material.POLISHED_BLACKSTONE_BRICKS : (dist < 12 ? Material.SMOOTH_QUARTZ : Material.QUARTZ_BRICKS);
                set(world, cx + x, cy, cz + z, floor);
                if (dist > 20 && dist <= 24) set(world, cx + x, cy + 1, cz + z, Material.POLISHED_BLACKSTONE_WALL);
            }
        }

        // Cross paths.
        for (int i = -24; i <= 24; i++) {
            set(world, cx + i, cy, cz, Material.CRYING_OBSIDIAN);
            set(world, cx, cy, cz + i, Material.CRYING_OBSIDIAN);
        }

        // Diagonal accents.
        for (int i = -17; i <= 17; i++) {
            set(world, cx + i, cy, cz + i, Material.AMETHYST_BLOCK);
            set(world, cx + i, cy, cz - i, Material.AMETHYST_BLOCK);
        }

        // Central beacon dais.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                set(world, cx + x, cy + 1, cz + z, Material.QUARTZ_BLOCK);
            }
        }
        set(world, cx, cy + 2, cz, Material.BEACON);
        set(world, cx, cy + 3, cz, Material.LIGHT);

        // Four outer pads so the hub looks obviously generated.
        pad(world, cx + 14, cy, cz, Material.EMERALD_BLOCK, Material.SEA_LANTERN);
        pad(world, cx - 14, cy, cz, Material.ENDER_CHEST, Material.SEA_LANTERN);
        pad(world, cx, cy, cz + 14, Material.ENCHANTING_TABLE, Material.SEA_LANTERN);
        pad(world, cx, cy, cz - 14, Material.AMETHYST_BLOCK, Material.SEA_LANTERN);

        // Corner pillars.
        pillar(world, cx + 12, cy + 1, cz + 12);
        pillar(world, cx - 12, cy + 1, cz + 12);
        pillar(world, cx + 12, cy + 1, cz - 12);
        pillar(world, cx - 12, cy + 1, cz - 12);
    }

    private static void pad(World world, int x, int y, int z, Material center, Material lamp) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                set(world, x + dx, y + 1, z + dz, Math.abs(dx) == 2 || Math.abs(dz) == 2 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.SMOOTH_QUARTZ);
            }
        }
        set(world, x, y + 2, z, center);
        set(world, x + 2, y + 2, z, lamp);
        set(world, x - 2, y + 2, z, lamp);
        set(world, x, y + 2, z + 2, lamp);
        set(world, x, y + 2, z - 2, lamp);
    }

    private static void pillar(World world, int x, int y, int z) {
        for (int i = 0; i < 6; i++) {
            set(world, x, y + i, z, i == 5 ? Material.SEA_LANTERN : Material.QUARTZ_PILLAR);
        }
    }

    private static void set(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != material) block.setType(material, false);
    }
}
