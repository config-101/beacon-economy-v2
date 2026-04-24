package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.PlayerData;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.List;

public class RankCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    public RankCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) return true;
        PlayerData pd = plugin.data().get(p.getUniqueId());
        String cmd = c.getName().toLowerCase();
        if (cmd.equals("rank")) {
            Msg.send(p, "Rank: " + BeaconEconomyPlugin.RANKS[pd.rankIndex] + " | Prestige: " + stars(pd.prestige), NamedTextColor.GOLD);
            return true;
        }
        if (cmd.equals("ranks")) {
            Msg.send(p, "Ranks: Drifter -> Vagrant -> Scavenger -> Outcast -> Rogue -> Survivor.", NamedTextColor.YELLOW);
            return true;
        }
        if (cmd.equals("rankup")) {
            if (pd.rankIndex >= BeaconEconomyPlugin.RANKS.length - 1) { Msg.send(p, "You are already Survivor. Use /prestige.", NamedTextColor.YELLOW); return true; }
            long base = plugin.getConfig().getLongList("rankup.costs").get(pd.rankIndex);
            double mult = plugin.getConfig().getDoubleList("rankup.prestige-multipliers").get(Math.min(pd.prestige, 3));
            long cost = Math.round(base * mult);
            if (pd.balance < cost) { Msg.send(p, "Next rank costs $" + cost + ".", NamedTextColor.RED); return true; }
            pd.balance -= cost;
            pd.rankIndex++;
            plugin.data().save();
            Msg.send(p, "Ranked up to " + BeaconEconomyPlugin.RANKS[pd.rankIndex] + " for $" + cost + ".", NamedTextColor.GREEN);
            return true;
        }
        if (cmd.equals("prestiges")) {
            Msg.send(p, "Prestige stars: ★ +10%, ★★ +20%, ★★★ +30% personal beacon revenue later.", NamedTextColor.LIGHT_PURPLE);
            return true;
        }
        if (cmd.equals("prestige")) {
            if (pd.prestige >= 3) { Msg.send(p, "You are already max prestige.", NamedTextColor.RED); return true; }
            if (pd.rankIndex < BeaconEconomyPlugin.RANKS.length - 1) { Msg.send(p, "You must be Survivor to prestige.", NamedTextColor.RED); return true; }
            long cost = plugin.getConfig().getLongList("prestige-costs").get(pd.prestige);
            if (pd.balance < cost) { Msg.send(p, "Prestige costs $" + cost + ".", NamedTextColor.RED); return true; }
            pd.balance -= cost;
            pd.prestige++;
            pd.rankIndex = 0;
            plugin.data().save();
            Msg.send(p, "Prestiged to " + stars(pd.prestige) + ". Your rank reset to Drifter.", NamedTextColor.GOLD);
            return true;
        }
        return true;
    }

    private String stars(int n) { return n <= 0 ? "None" : "★".repeat(n); }
}
