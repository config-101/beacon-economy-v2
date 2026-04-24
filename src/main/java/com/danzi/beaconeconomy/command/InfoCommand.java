package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.gui.InfoMenus;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class InfoCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    public InfoCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        switch (command.getName().toLowerCase()) {
            case "info" -> Msg.send(p, "Server and plugin made by Danzi. Use /behelp, /tutorial, or /commands for information.", NamedTextColor.GOLD);
            case "commands", "becommands" -> p.openInventory(InfoMenus.commands(plugin, p));
            case "tutorial", "behelp" -> p.openInventory(InfoMenus.help(plugin, p));
            default -> p.openInventory(InfoMenus.help(plugin, p));
        }
        return true;
    }
}
