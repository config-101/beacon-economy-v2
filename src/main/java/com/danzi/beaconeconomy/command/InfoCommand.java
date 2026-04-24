package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InfoCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;

    public InfoCommand(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Beacon Economy server and plugin made by Danzi.");
            return true;
        }
        player.sendMessage(Msg.line("This server and plugin were made by Danzi.", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Use /behelp or /tutorial for information.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Use /wild to begin your adventure when you are ready.", NamedTextColor.GRAY));
        player.sendMessage(Msg.contactLine());
        return true;
    }
}
