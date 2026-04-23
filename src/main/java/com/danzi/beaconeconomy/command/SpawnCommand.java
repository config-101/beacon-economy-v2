package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.listener.CombatListener;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;

    public SpawnCommand(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (CombatListener.isTagged(player.getUniqueId())) {
            Msg.send(player, "You cannot use /spawn while combat tagged.", NamedTextColor.RED);
            return true;
        }
        plugin.getTeleportManager().queue(player, plugin.getSpawnLocation(), "spawn");
        return true;
    }
}
