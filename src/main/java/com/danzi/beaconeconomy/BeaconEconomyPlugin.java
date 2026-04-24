package com.danzi.beaconeconomy;

import com.danzi.beaconeconomy.command.*;
import com.danzi.beaconeconomy.data.DataManager;
import com.danzi.beaconeconomy.gui.MenuListener;
import com.danzi.beaconeconomy.listener.*;
import com.danzi.beaconeconomy.relic.RelicManager;
import com.danzi.beaconeconomy.teleport.TeleportManager;
import com.danzi.beaconeconomy.world.VoidChunkGenerator;
import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BeaconEconomyPlugin extends JavaPlugin {
    private DataManager dataManager;
    private TeleportManager teleportManager;
    private RelicManager relicManager;
    private World introWorld;
    private Location introLocation;

    public static final String[] RANKS = {"Drifter","Vagrant","Scavenger","Outcast","Rogue","Survivor"};

    @Override
    public void onEnable() {
        saveDefaultConfig();
        introWorld = createIntroWorld();
        introLocation = new Location(introWorld,
                getConfig().getDouble("intro-location.x"),
                getConfig().getDouble("intro-location.y"),
                getConfig().getDouble("intro-location.z"),
                (float) getConfig().getDouble("intro-location.yaw"),
                (float) getConfig().getDouble("intro-location.pitch"));

        dataManager = new DataManager(this);
        dataManager.load();
        teleportManager = new TeleportManager(this);
        relicManager = new RelicManager(this);
        relicManager.startTasks();

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RelicListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BeaconListener(this), this);

        getLogger().info("Beacon Economy Systems Build enabled.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.save();
    }

    private World createIntroWorld() {
        String name = getConfig().getString("intro-world", "beacon_intro");
        World w = Bukkit.getWorld(name);
        if (w == null) {
            WorldCreator wc = new WorldCreator(name);
            wc.generator(new VoidChunkGenerator());
            w = Bukkit.createWorld(wc);
        }
        if (w != null) {
            w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setTime(18000);
            w.setSpawnLocation(new Location(w, getConfig().getDouble("intro-location.x"), getConfig().getDouble("intro-location.y"), getConfig().getDouble("intro-location.z")));
        }
        return w;
    }

    private void registerCommands() {
        bind("info", new InfoCommand(this));
        bind("behelp", new InfoCommand(this));
        bind("tutorial", new InfoCommand(this));
        bind("commands", new InfoCommand(this));
        bind("becommands", new InfoCommand(this));
        bind("wild", new WildCommand(this));
        bind("spawn", new SpawnCommand(this));
        HomeCommand hc = new HomeCommand(this);
        bind("sethome", hc); bind("home", hc); bind("homes", hc); bind("delhome", hc);
        bind("relic", new RelicCommand(this));
        RankCommand rc = new RankCommand(this);
        bind("rank", rc); bind("ranks", rc); bind("rankup", rc); bind("prestige", rc); bind("prestiges", rc);
        MoneyCommand mc = new MoneyCommand(this);
        bind("money", mc); bind("balance", mc);
        StaffCommand sc = new StaffCommand(this);
        bind("clearlag", sc); bind("invsee", sc); bind("vanish", sc); bind("spectate", sc); bind("freeze", sc); bind("unfreeze", sc); bind("staffhelp", sc);
        bind("beadmin", new AdminCommand(this));
    }

    private void bind(String name, Object exec) {
        PluginCommand c = getCommand(name);
        if (c == null) return;
        if (exec instanceof org.bukkit.command.CommandExecutor ce) c.setExecutor(ce);
        if (exec instanceof org.bukkit.command.TabCompleter tc) c.setTabCompleter(tc);
    }

    public DataManager data() { return dataManager; }
    public TeleportManager teleports() { return teleportManager; }
    public RelicManager relics() { return relicManager; }
    public World introWorld() { return introWorld; }
    public Location introLocation() { return introLocation.clone(); }
    public World survivalWorld() { return Bukkit.getWorld(getConfig().getString("survival-world", "world")); }
}
