package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

public class WildCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    public WildCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) return true;
        World w = plugin.survivalWorld();
        if (w == null) { Msg.send(p, "Survival world not found.", NamedTextColor.RED); return true; }
        int min = plugin.getConfig().getInt("wild.min-radius", 1500);
        int max = plugin.getConfig().getInt("wild.max-radius", 5000);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i=0;i<40;i++) {
            int x = r.nextInt(min, max) * (r.nextBoolean()?1:-1);
            int z = r.nextInt(min, max) * (r.nextBoolean()?1:-1);
            int y = w.getHighestBlockYAt(x, z) + 1;
            Block below = w.getBlockAt(x, y-1, z);
            if (!below.isLiquid() && below.getType().isSolid()) {
                plugin.teleports().queue(p, new Location(w, x+0.5, y, z+0.5), "wilderness");
                return true;
            }
        }
        Msg.send(p, "Could not find a safe wild location. Try again.", NamedTextColor.RED);
        return true;
    }
}
