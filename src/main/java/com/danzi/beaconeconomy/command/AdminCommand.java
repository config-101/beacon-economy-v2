package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import com.danzi.beaconeconomy.world.SpawnHubBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdminCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;

    public AdminCommand(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/beadmin giveriftdagger [player] | hubregen");
            return true;
        }
        if (args[0].equalsIgnoreCase("giveriftdagger")) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                target = null;
            }
            if (target == null) {
                sender.sendMessage("Target not found.");
                return true;
            }
            target.getInventory().addItem(plugin.getRelicManager().createRiftDagger());
            Msg.send(target, "A Forgotten Relic has found its way into your hands.", NamedTextColor.LIGHT_PURPLE);
            sender.sendMessage("Gave Rift Dagger to " + target.getName());
            return true;
        }
        if (args[0].equalsIgnoreCase("hubregen")) {
            SpawnHubBuilder.ensureHub(plugin.getSpawnWorld(), plugin.getSpawnLocation());
            sender.sendMessage("Regenerated the spawn hub platform.");
            return true;
        }
        sender.sendMessage("Unknown subcommand.");
        return true;
    }
}
