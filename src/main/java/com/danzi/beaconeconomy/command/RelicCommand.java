package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.relic.RelicType;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class RelicCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    public RelicCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0 || !a[0].equalsIgnoreCase("info")) {
            Msg.send(p, "Use /relic info while holding a Forgotten Relic.", NamedTextColor.YELLOW);
            return true;
        }
        RelicType type = plugin.relics().type(p.getInventory().getItemInMainHand());
        if (type == null) type = plugin.relics().type(p.getInventory().getItemInOffHand());
        if (type == null) { Msg.send(p, "You are not holding a Forgotten Relic.", NamedTextColor.RED); return true; }
        plugin.relics().info(p, type);
        return true;
    }
}
