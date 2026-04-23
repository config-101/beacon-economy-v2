package com.danzi.beaconeconomy.data;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {
    private final BeaconEconomyPlugin plugin;
    private final File file;
    private final Map<UUID, Map<Integer, Location>> homes = new HashMap<>();

    public HomeManager(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String uuidKey : cfg.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            Map<Integer, Location> map = new HashMap<>();
            for (String slotKey : cfg.getConfigurationSection(uuidKey).getKeys(false)) {
                int slot = Integer.parseInt(slotKey);
                String path = uuidKey + "." + slotKey;
                World world = Bukkit.getWorld(cfg.getString(path + ".world"));
                if (world == null) continue;
                Location loc = new Location(
                    world,
                    cfg.getDouble(path + ".x"),
                    cfg.getDouble(path + ".y"),
                    cfg.getDouble(path + ".z"),
                    (float) cfg.getDouble(path + ".yaw"),
                    (float) cfg.getDouble(path + ".pitch")
                );
                map.put(slot, loc);
            }
            homes.put(uuid, map);
        }
    }

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Map<Integer, Location>> entry : homes.entrySet()) {
            String base = entry.getKey().toString();
            for (Map.Entry<Integer, Location> home : entry.getValue().entrySet()) {
                Location loc = home.getValue();
                String path = base + "." + home.getKey();
                cfg.set(path + ".world", loc.getWorld().getName());
                cfg.set(path + ".x", loc.getX());
                cfg.set(path + ".y", loc.getY());
                cfg.set(path + ".z", loc.getZ());
                cfg.set(path + ".yaw", loc.getYaw());
                cfg.set(path + ".pitch", loc.getPitch());
            }
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save homes.yml: " + e.getMessage());
        }
    }

    public Map<Integer, Location> getHomes(UUID uuid) {
        return homes.computeIfAbsent(uuid, ignored -> new HashMap<>());
    }

    public Location getHome(UUID uuid, int slot) {
        return getHomes(uuid).get(slot);
    }

    public boolean hasHome(UUID uuid, int slot) {
        return getHomes(uuid).containsKey(slot);
    }

    public int nextEmptySlot(UUID uuid) {
        for (int i = 1; i <= 3; i++) {
            if (!hasHome(uuid, i)) return i;
        }
        return -1;
    }

    public void setHome(Player player, int slot, Location location) {
        getHomes(player.getUniqueId()).put(slot, location.clone());
        save();
    }

    public void deleteHome(UUID uuid, int slot) {
        getHomes(uuid).remove(slot);
        save();
    }
}
