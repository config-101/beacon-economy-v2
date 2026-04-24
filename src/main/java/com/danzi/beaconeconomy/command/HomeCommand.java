package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.PlayerData;
import com.danzi.beaconeconomy.gui.HomeMenu;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    public HomeCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        String name = command.getName().toLowerCase();
        PlayerData pd = plugin.data().get(p.getUniqueId());
        if (name.equals("homes")) { p.openInventory(HomeMenu.homes(plugin, p)); return true; }
        int slot = args.length > 0 ? parse(args[0]) : 1;
        if (slot < 1 || slot > 3) { Msg.send(p, "Use a home slot from 1 to 3.", NamedTextColor.RED); return true; }

        if (name.equals("sethome")) {
            if (p.getWorld().equals(plugin.introWorld())) { Msg.send(p, "You cannot set a home in the intro world.", NamedTextColor.RED); return true; }
            if (p.getWorld().getEnvironment() == World.Environment.NETHER && p.getLocation().getY() >= 127) { Msg.send(p, "You cannot set a home on the Nether roof.", NamedTextColor.RED); return true; }
            if (pd.homes.size() >= 3 && !pd.homes.containsKey(slot)) { Msg.send(p, "You already have 3 homes. Delete one first.", NamedTextColor.RED); return true; }
            pd.homes.put(slot, p.getLocation().clone());
            plugin.data().save();
            Msg.send(p, "Home " + slot + " set.", NamedTextColor.GREEN);
            return true;
        }
        if (name.equals("home")) {
            Location loc = pd.homes.get(slot);
            if (loc == null) { Msg.send(p, "Home " + slot + " is not set.", NamedTextColor.RED); return true; }
            plugin.teleports().queue(p, loc, "home " + slot);
            return true;
        }
        if (name.equals("delhome")) {
            if (pd.homes.remove(slot) != null) {
                plugin.data().save();
                Msg.send(p, "Deleted home " + slot + ".", NamedTextColor.YELLOW);
            } else Msg.send(p, "Home " + slot + " is not set.", NamedTextColor.RED);
            return true;
        }
        return true;
    }
    private int parse(String s) { try { return Integer.parseInt(s); } catch(Exception e) { return -1; } }
}
