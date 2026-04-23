package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.listener.CombatListener;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class WildCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    private final Random random = new Random();

    public WildCommand(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (CombatListener.isTagged(player.getUniqueId())) {
            Msg.send(player, "You cannot use /wild while combat tagged.", NamedTextColor.RED);
            return true;
        }
        World world = plugin.getSurvivalWorld();
        if (world == null) {
            Msg.send(player, "The survival world is not loaded.", NamedTextColor.RED);
            return true;
        }
        Location target = findSafe(world);
        if (target == null) {
            Msg.send(player, "Could not find a safe wilderness location.", NamedTextColor.RED);
            return true;
        }
        plugin.getTeleportManager().queue(player, target, "wild");
        return true;
    }

    private Location findSafe(World world) {
        int min = plugin.getConfig().getInt("wild.min-radius", 1500);
        int max = plugin.getConfig().getInt("wild.max-radius", 5000);
        for (int i = 0; i < 40; i++) {
            int x = randomBetween(min, max) * (random.nextBoolean() ? 1 : -1);
            int z = randomBetween(min, max) * (random.nextBoolean() ? 1 : -1);
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
            Block feet = loc.getBlock();
            Block ground = world.getBlockAt(x, y, z);
            if (ground.getType() == Material.WATER || ground.getType() == Material.LAVA) continue;
            if (!feet.getType().isAir()) continue;
            if (!feet.getRelative(0, 1, 0).getType().isAir()) continue;
            return loc;
        }
        return null;
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }
}
