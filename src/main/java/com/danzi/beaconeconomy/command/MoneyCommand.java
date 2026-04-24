package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.PlayerData;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    public MoneyCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) return true;
        PlayerData pd = plugin.data().get(p.getUniqueId());
        if (a.length == 0 || a[0].equalsIgnoreCase("balance")) {
            Msg.send(p, "Balance: $" + pd.balance, NamedTextColor.GOLD); return true;
        }
        if (a[0].equalsIgnoreCase("pay") && a.length >= 3) {
            Player target = Bukkit.getPlayerExact(a[1]);
            long amount = parse(a[2]);
            if (target == null || amount <= 0) { Msg.send(p, "Use /money pay <player> <amount>.", NamedTextColor.RED); return true; }
            if (pd.balance < amount) { Msg.send(p, "You do not have enough money.", NamedTextColor.RED); return true; }
            pd.balance -= amount;
            plugin.data().get(target.getUniqueId()).balance += amount;
            plugin.data().save();
            Msg.send(p, "Paid " + target.getName() + " $" + amount + ".", NamedTextColor.GREEN);
            Msg.send(target, p.getName() + " paid you $" + amount + ".", NamedTextColor.GREEN);
            return true;
        }
        Msg.send(p, "Use /money, /money balance, or /money pay <player> <amount>.", NamedTextColor.YELLOW);
        return true;
    }
    private long parse(String s) { try { return Long.parseLong(s); } catch(Exception e) { return -1; } }
}
