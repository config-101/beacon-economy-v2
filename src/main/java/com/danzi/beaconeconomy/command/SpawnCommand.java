package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    public SpawnCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) return true;
        plugin.teleports().queue(p, plugin.introLocation(), "spawn");
        return true;
    }
}
