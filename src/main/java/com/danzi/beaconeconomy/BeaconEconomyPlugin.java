package com.danzi.beaconeconomy;

import com.danzi.beaconeconomy.command.AdminCommand;
import com.danzi.beaconeconomy.command.HomeCommand;
import com.danzi.beaconeconomy.command.RelicCommand;
import com.danzi.beaconeconomy.command.SimpleMenuCommand;
import com.danzi.beaconeconomy.command.SpawnCommand;
import com.danzi.beaconeconomy.command.WildCommand;
import com.danzi.beaconeconomy.data.HomeManager;
import com.danzi.beaconeconomy.gui.MenuListener;
import com.danzi.beaconeconomy.listener.CombatListener;
import com.danzi.beaconeconomy.listener.JoinListener;
import com.danzi.beaconeconomy.listener.ProtectionListener;
import com.danzi.beaconeconomy.relic.RelicListener;
import com.danzi.beaconeconomy.relic.RelicManager;
import com.danzi.beaconeconomy.teleport.TeleportManager;
import com.danzi.beaconeconomy.world.SpawnHubBuilder;
import com.danzi.beaconeconomy.world.VoidChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class BeaconEconomyPlugin extends JavaPlugin {
    private HomeManager homeManager;
    private TeleportManager teleportManager;
    private RelicManager relicManager;
    private World spawnWorld;
    private Location spawnLocation;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.spawnWorld = createOrLoadSpawnWorld();
        ConfigurationSection spawnSection = getConfig().getConfigurationSection("spawn-location");
        this.spawnLocation = new Location(
            spawnWorld,
            spawnSection.getDouble("x"),
            spawnSection.getDouble("y"),
            spawnSection.getDouble("z"),
            (float) spawnSection.getDouble("yaw"),
            (float) spawnSection.getDouble("pitch")
        );

        SpawnHubBuilder.ensureHub(spawnWorld, spawnLocation);
        spawnWorld.setSpawnLocation(spawnLocation.getBlockX(), spawnLocation.getBlockY(), spawnLocation.getBlockZ());

        this.homeManager = new HomeManager(this);
        this.homeManager.load();
        this.teleportManager = new TeleportManager(this);
        this.relicManager = new RelicManager(this);

        registerCommands();
        registerListeners();

        getLogger().info("Beacon Economy Paper Test enabled.");
    }

    @Override
    public void onDisable() {
        if (homeManager != null) {
            homeManager.save();
        }
    }

    private World createOrLoadSpawnWorld() {
        String worldName = getConfig().getString("spawn-world", "beacon_spawn");
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) return existing;
        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new VoidChunkGenerator());
        World world = Bukkit.createWorld(creator);
        if (world != null) {
            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setTime(18000);
        }
        return world;
    }

    private void registerCommands() {
        bind("spawn", new SpawnCommand(this));
        bind("wild", new WildCommand(this));
        bind("tutorial", new SimpleMenuCommand(this, SimpleMenuCommand.Mode.TUTORIAL));
        bind("behelp", new SimpleMenuCommand(this, SimpleMenuCommand.Mode.HELP));
        HomeCommand homeCommand = new HomeCommand(this);
        bind("sethome", homeCommand);
        bind("home", homeCommand);
        bind("homes", homeCommand);
        bind("delhome", homeCommand);
        bind("relic", new RelicCommand(this));
        bind("beadmin", new AdminCommand(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RelicListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
    }

    private void bind(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command == null) return;
        if (executor instanceof org.bukkit.command.CommandExecutor cmd) {
            command.setExecutor(cmd);
        }
        if (executor instanceof org.bukkit.command.TabCompleter tab) {
            command.setTabCompleter(tab);
        }
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public RelicManager getRelicManager() {
        return relicManager;
    }

    public World getSpawnWorld() {
        return spawnWorld;
    }

    public Location getSpawnLocation() {
        return spawnLocation.clone();
    }

    public World getSurvivalWorld() {
        return Bukkit.getWorld(getConfig().getString("survival-world", "world"));
    }
}
