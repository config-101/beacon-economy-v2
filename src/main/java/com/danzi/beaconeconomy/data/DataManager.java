package com.danzi.beaconeconomy.data;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final BeaconEconomyPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerData> data = new HashMap<>();

    public DataManager(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public PlayerData get(UUID uuid) {
        return data.computeIfAbsent(uuid, ignored -> new PlayerData());
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerData pd = new PlayerData();
                pd.introComplete = cfg.getBoolean(key + ".introComplete", false);
                pd.balance = cfg.getLong(key + ".balance", 0L);
                pd.rankIndex = cfg.getInt(key + ".rankIndex", 0);
                pd.prestige = cfg.getInt(key + ".prestige", 0);
                if (cfg.isConfigurationSection(key + ".homes")) {
                    for (String slotKey : cfg.getConfigurationSection(key + ".homes").getKeys(false)) {
                        String path = key + ".homes." + slotKey;
                        World world = Bukkit.getWorld(cfg.getString(path + ".world", ""));
                        if (world == null) continue;
                        Location loc = new Location(world, cfg.getDouble(path + ".x"), cfg.getDouble(path + ".y"), cfg.getDouble(path + ".z"),
                                (float) cfg.getDouble(path + ".yaw"), (float) cfg.getDouble(path + ".pitch"));
                        pd.homes.put(Integer.parseInt(slotKey), loc);
                    }
                }
                data.put(uuid, pd);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> e : data.entrySet()) {
            String key = e.getKey().toString();
            PlayerData pd = e.getValue();
            cfg.set(key + ".introComplete", pd.introComplete);
            cfg.set(key + ".balance", pd.balance);
            cfg.set(key + ".rankIndex", pd.rankIndex);
            cfg.set(key + ".prestige", pd.prestige);
            for (Map.Entry<Integer, Location> h : pd.homes.entrySet()) {
                Location loc = h.getValue();
                String path = key + ".homes." + h.getKey();
                cfg.set(path + ".world", loc.getWorld().getName());
                cfg.set(path + ".x", loc.getX());
                cfg.set(path + ".y", loc.getY());
                cfg.set(path + ".z", loc.getZ());
                cfg.set(path + ".yaw", loc.getYaw());
                cfg.set(path + ".pitch", loc.getPitch());
            }
        }
        try { cfg.save(file); } catch (IOException ex) { plugin.getLogger().warning("Could not save players.yml: " + ex.getMessage()); }
    }
}
