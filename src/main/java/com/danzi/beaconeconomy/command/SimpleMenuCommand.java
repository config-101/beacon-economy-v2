package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.gui.InfoMenus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SimpleMenuCommand implements CommandExecutor {
    public enum Mode { HELP, TUTORIAL }

    private final BeaconEconomyPlugin plugin;
    private final Mode mode;

    public SimpleMenuCommand(BeaconEconomyPlugin plugin, Mode mode) {
        this.plugin = plugin;
        this.mode = mode;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (mode == Mode.HELP) {
            player.openInventory(InfoMenus.help(plugin, player));
            return true;
        }
        plugin.getTeleportManager().queue(player, plugin.getTutorialLocation(), "tutorial");
        return true;
    }
}
