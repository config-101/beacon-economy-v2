package com.danzi.beaconeconomy.world;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import java.util.List;

public final class SpawnHubBuilder {
    private SpawnHubBuilder() {}

    public static void ensureHub(BeaconEconomyPlugin plugin, World world, Location center) {
        int cx = center.getBlockX();
        int cy = center.getBlockY() - 1;
        int cz = center.getBlockZ();

        clearNearbyNpcs(world, center, 120);

        disc(world, cx, cy, cz, 18, Material.SMOOTH_QUARTZ, Material.POLISHED_BLACKSTONE_BRICKS);
        disc(world, cx, cy + 1, cz, 8, Material.QUARTZ_BLOCK, Material.QUARTZ_PILLAR);
        ring(world, cx, cy + 2, cz, 11, Material.POLISHED_BLACKSTONE_WALL);

        // central monument
        disc(world, cx, cy + 2, cz, 3, Material.QUARTZ_BLOCK, Material.QUARTZ_BLOCK);
        set(world, cx, cy + 3, cz, Material.BEACON);
        for (int y = cy + 3; y <= cy + 10; y++) set(world, cx, y, cz, y == cy + 10 ? Material.SEA_LANTERN : Material.LIGHT);

        // main bridges
        bridge(world, cx, cy, cz, 0, -42, 8, Material.SMOOTH_QUARTZ, Material.POLISHED_BLACKSTONE_BRICKS);
        bridge(world, cx, cy, cz, 0, 42, 8, Material.SMOOTH_QUARTZ, Material.POLISHED_BLACKSTONE_BRICKS);
        bridge(world, cx, cy, cz, 42, 0, 8, Material.SMOOTH_QUARTZ, Material.POLISHED_BLACKSTONE_BRICKS);
        bridge(world, cx, cy, cz, -42, 0, 8, Material.SMOOTH_QUARTZ, Material.POLISHED_BLACKSTONE_BRICKS);

        // diagonals / lower links
        bridge(world, cx, cy - 2, cz, 30, 30, 6, Material.QUARTZ_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS);
        bridge(world, cx, cy - 2, cz, -30, 30, 6, Material.QUARTZ_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS);
        bridge(world, cx, cy - 2, cz, 30, -30, 6, Material.QUARTZ_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS);
        bridge(world, cx, cy - 2, cz, -30, -30, 6, Material.QUARTZ_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS);

        // districts
        districtBeaconMarket(world, cx, cy, cz - 52);
        districtTutorial(world, cx - 54, cy, cz);
        districtBlackMarket(world, cx + 54, cy - 2, cz);
        districtPet(world, cx - 34, cy - 3, cz + 36);
        districtSpawner(world, cx + 36, cy - 3, cz + 36);
        districtAfk(world, cx, cy + 8, cz + 48);
        districtEvent(world, cx + 54, cy + 2, cz - 34);
        districtSpawnBeacon(world, cx, cy, cz + 24);
        dragons(world, cx, cy + 15, cz);

        // jump pads around plaza
        jumpPad(world, cx + 10, cy + 2, cz);
        jumpPad(world, cx - 10, cy + 2, cz);
        jumpPad(world, cx, cy + 2, cz + 10);
        jumpPad(world, cx, cy + 2, cz - 10);

        // tutorial NPCs
        npc(world, new Location(world, cx - 53.5, cy + 2, cz - 10.5, 90, 0), "Danzi", Villager.Profession.LIBRARIAN);
        npc(world, new Location(world, cx - 53.5, cy + 2, cz - 5.5, 90, 0), "Beacon Broker", Villager.Profession.CARTOGRAPHER);
        npc(world, new Location(world, cx - 53.5, cy + 2, cz - 0.5, 90, 0), "The Registrar", Villager.Profession.CLERIC);
        npc(world, new Location(world, cx - 53.5, cy + 2, cz + 4.5, 90, 0), "The Warden Clerk", Villager.Profession.ARMORER);
        npc(world, new Location(world, cx - 53.5, cy + 2, cz + 9.5, 90, 0), "Relic Whisperer", Villager.Profession.MASON);
        npc(world, new Location(world, cx - 46.5, cy + 2, cz - 7.5, -90, 0), "Shady Broker", Villager.Profession.NITWIT);
        npc(world, new Location(world, cx - 46.5, cy + 2, cz - 2.5, -90, 0), "Stablemaster", Villager.Profession.SHEPHERD);
        npc(world, new Location(world, cx - 46.5, cy + 2, cz + 2.5, -90, 0), "Vault Warden", Villager.Profession.WEAPONSMITH);
        npc(world, new Location(world, cx - 46.5, cy + 2, cz + 7.5, -90, 0), "Archivist", Villager.Profession.LIBRARIAN);

        // ambient particles on anchors
        world.spawnParticle(Particle.ENCHANT, center.clone().add(0, 3, 0), 120, 1.5, 2.0, 1.5, 0.02);
    }

    public static Location tutorialLocation(Location spawn) {
        return spawn.clone().add(-54, 2, 0);
    }

    private static void districtBeaconMarket(World world, int x, int y, int z) {
        disc(world, x, y, z, 14, Material.SMOOTH_QUARTZ, Material.QUARTZ_BRICKS);
        ring(world, x, y + 1, z, 14, Material.POLISHED_BLACKSTONE_WALL);
        for (int dx : List.of(-8, 0, 8)) {
            pillar(world, x + dx, y + 1, z - 3, 5, Material.QUARTZ_PILLAR, Material.SEA_LANTERN);
            pillar(world, x + dx, y + 1, z + 3, 5, Material.QUARTZ_PILLAR, Material.SEA_LANTERN);
        }
        set(world, x, y + 2, z, Material.BEACON);
        set(world, x, y + 3, z, Material.LIGHT);
    }

    private static void districtTutorial(World world, int x, int y, int z) {
        rect(world, x - 12, y, z - 14, x + 12, y, z + 14, Material.SMOOTH_QUARTZ);
        rectWalls(world, x - 12, y + 1, z - 14, x + 12, y + 4, z + 14, Material.POLISHED_BLACKSTONE_WALL);
        aisle(world, x - 10, y, z, x + 10, z, Material.CRYING_OBSIDIAN);
        for (int dz = -10; dz <= 10; dz += 5) {
            set(world, x - 8, y + 1, z + dz, Material.LECTERN);
            set(world, x + 8, y + 1, z + dz, Material.LECTERN);
        }
    }

    private static void districtBlackMarket(World world, int x, int y, int z) {
        disc(world, x, y, z, 15, Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE);
        ring(world, x, y + 1, z, 15, Material.BLACKSTONE_WALL);
        for (int i = -10; i <= 10; i += 5) {
            set(world, x + i, y + 1, z - 4, Material.CRYING_OBSIDIAN);
            set(world, x + i, y + 1, z + 4, Material.CRYING_OBSIDIAN);
            pillar(world, x + i, y + 2, z - 6, 4, Material.CHAIN, Material.SOUL_LANTERN);
        }
    }

    private static void districtPet(World world, int x, int y, int z) {
        disc(world, x, y, z, 12, Material.QUARTZ_BRICKS, Material.OAK_PLANKS);
        ring(world, x, y + 1, z, 12, Material.OAK_FENCE);
        set(world, x, y + 1, z, Material.MOSS_BLOCK);
        set(world, x + 4, y + 1, z + 2, Material.HAY_BLOCK);
        set(world, x - 4, y + 1, z - 2, Material.OAK_LEAVES);
    }

    private static void districtSpawner(World world, int x, int y, int z) {
        disc(world, x, y, z, 13, Material.DEEPSLATE_TILES, Material.OBSIDIAN);
        ring(world, x, y + 1, z, 13, Material.IRON_BARS);
        set(world, x, y + 1, z, Material.SPAWNER);
        pillar(world, x + 5, y + 1, z + 5, 6, Material.CRYING_OBSIDIAN, Material.LAVA_CAULDRON);
        pillar(world, x - 5, y + 1, z - 5, 6, Material.CRYING_OBSIDIAN, Material.LAVA_CAULDRON);
    }

    private static void districtAfk(World world, int x, int y, int z) {
        disc(world, x, y, z, 10, Material.SMOOTH_QUARTZ, Material.QUARTZ_BRICKS);
        disc(world, x - 18, y - 3, z + 6, 7, Material.QUARTZ_BRICKS, Material.SMOOTH_QUARTZ);
        disc(world, x + 18, y - 3, z + 6, 7, Material.QUARTZ_BRICKS, Material.SMOOTH_QUARTZ);
        disc(world, x, y + 5, z - 8, 7, Material.QUARTZ_BLOCK, Material.QUARTZ_BLOCK);
    }

    private static void districtEvent(World world, int x, int y, int z) {
        disc(world, x, y, z, 13, Material.OBSIDIAN, Material.CRYING_OBSIDIAN);
        ring(world, x, y + 1, z, 13, Material.BLACKSTONE_WALL);
        for (int dx : List.of(-6, 0, 6)) pillar(world, x + dx, y + 1, z, 5, Material.CRYING_OBSIDIAN, Material.SOUL_LANTERN);
        world.spawnParticle(Particle.PORTAL, new Location(world, x + 0.5, y + 2, z + 0.5), 60, 2, 1, 2, 0.05);
    }

    private static void districtSpawnBeacon(World world, int x, int y, int z) {
        disc(world, x, y, z, 7, Material.QUARTZ_BLOCK, Material.SMOOTH_QUARTZ);
        set(world, x, y + 1, z, Material.BEACON);
        set(world, x, y + 2, z, Material.LIGHT);
    }

    private static void dragons(World world, int x, int y, int z) {
        dragon(world, x - 22, y, z - 24, Material.QUARTZ_BLOCK, Material.GOLD_BLOCK, false);
        dragon(world, x + 26, y - 2, z + 18, Material.BLACKSTONE, Material.CRYING_OBSIDIAN, true);
    }

    private static void dragon(World world, int x, int y, int z, Material body, Material eye, boolean invert) {
        for (int i = 0; i < 14; i++) set(world, x + (invert ? -i : i), y + (i / 5), z, body);
        for (int i = 0; i < 8; i++) {
            set(world, x + 4 + (invert ? -i : i), y + 2 + (i / 3), z + 2, body);
            set(world, x + 4 + (invert ? -i : i), y + 2 + (i / 3), z - 2, body);
        }
        set(world, x, y + 1, z, eye);
        set(world, x + (invert ? -1 : 1), y + 1, z, eye);
    }

    private static void jumpPad(World world, int x, int y, int z) {
        rect(world, x - 1, y, z - 1, x + 1, y, z + 1, Material.POLISHED_BLACKSTONE_BRICKS);
        set(world, x, y + 1, z, Material.TARGET);
    }

    private static void npc(World world, Location loc, String name, Villager.Profession profession) {
        Villager villager = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
        villager.customName(Component.text(name, NamedTextColor.GOLD));
        villager.setCustomNameVisible(true);
        villager.setProfession(profession);
        villager.setVillagerLevel(5);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setRemoveWhenFarAway(false);
        villager.setCanPickupItems(false);
    }

    private static void clearNearbyNpcs(World world, Location center, double radius) {
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Villager) entity.remove();
        }
    }

    private static void pillar(World world, int x, int y, int z, int height, Material shaft, Material top) {
        for (int i = 0; i < height; i++) set(world, x, y + i, z, i == height - 1 ? top : shaft);
    }

    private static void disc(World world, int cx, int y, int cz, int radius, Material inner, Material outer) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > radius) continue;
                set(world, cx + x, y, cz + z, dist > radius - 2 ? outer : inner);
            }
        }
    }

    private static void ring(World world, int cx, int y, int cz, int radius, Material wall) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > radius - 0.5 && dist < radius + 0.5) set(world, cx + x, y, cz + z, wall);
            }
        }
    }

    private static void bridge(World world, int sx, int sy, int sz, int dx, int dz, int width, Material floor, Material edge) {
        int ex = sx + dx;
        int ez = sz + dz;
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int x = (int) Math.round(sx + dx * t);
            int z = (int) Math.round(sz + dz * t);
            for (int w = -width / 2; w <= width / 2; w++) {
                boolean alongX = Math.abs(dx) > Math.abs(dz);
                int bx = alongX ? x : x + w;
                int bz = alongX ? z + w : z;
                set(world, bx, sy, bz, Math.abs(w) == width / 2 ? edge : floor);
            }
        }
        // support anchor islands at end points
        disc(world, ex, sy - 1, ez, width / 2 + 2, floor, edge);
    }

    private static void rect(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(world, x, y, z, material);
                }
            }
        }
    }

    private static void rectWalls(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                set(world, x, y, z1, material);
                set(world, x, y, z2, material);
            }
            for (int z = z1; z <= z2; z++) {
                set(world, x1, y, z, material);
                set(world, x2, y, z, material);
            }
        }
    }

    private static void aisle(World world, int x1, int y, int z1, int x2, int z2, Material material) {
        for (int x = x1; x <= x2; x++) set(world, x, y, z1, material);
    }

    private static void set(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != material) block.setType(material, false);
    }
}
