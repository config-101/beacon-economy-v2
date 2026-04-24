package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.PlayerData;
import com.danzi.beaconeconomy.gui.InfoMenus;
import com.danzi.beaconeconomy.relic.RelicType;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    public AdminCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("beaconeconomy.admin") && !s.isOp()) { s.sendMessage("No permission."); return true; }
        if (s instanceof Player p && a.length == 0) { p.openInventory(InfoMenus.admin(plugin, p)); return true; }

        if (a.length >= 1 && a[0].equalsIgnoreCase("giverelic")) {
            if (!(s instanceof Player p) && a.length < 3) { s.sendMessage("Console: /beadmin giverelic <id> <player>"); return true; }
            if (a.length < 2) { s.sendMessage("Use /beadmin giverelic <id> [player]"); return true; }
            RelicType type = RelicType.byId(a[1]);
            if (type == null) { s.sendMessage("Unknown relic id."); return true; }
            Player target = a.length >= 3 ? Bukkit.getPlayerExact(a[2]) : (Player) s;
            if (target == null) { s.sendMessage("Player not found."); return true; }
            target.getInventory().addItem(plugin.relics().create(type));
            Msg.send(target, "You received " + type.display + ".", NamedTextColor.DARK_PURPLE);
            return true;
        }

        if (a.length >= 3 && a[0].equalsIgnoreCase("money")) {
            Player t = Bukkit.getPlayerExact(a[1]);
            long amount = parse(a[2]);
            if (t == null || amount < 0) { s.sendMessage("Use /beadmin money <player> <amount>"); return true; }
            PlayerData pd = plugin.data().get(t.getUniqueId());
            pd.balance = amount;
            plugin.data().save();
            s.sendMessage("Set " + t.getName() + " balance to $" + amount);
            return true;
        }

        s.sendMessage("Admin: /beadmin, /beadmin giverelic <id> [player], /beadmin money <player> <amount>");
        return true;
    }
    private long parse(String s) { try { return Long.parseLong(s); } catch(Exception e) { return -1; } }
}
