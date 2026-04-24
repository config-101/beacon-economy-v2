package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import java.util.*;

public class StaffCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;
    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();
    public StaffCommand(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("beaconeconomy.admin") && !s.isOp()) { s.sendMessage("No permission."); return true; }
        String cmd = c.getName().toLowerCase();
        if (cmd.equals("clearlag")) {
            Msg.broadcast("⚠ Clearlag warning: dropped items will be removed in 10 seconds.", NamedTextColor.YELLOW);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                int count = 0;
                for (World w : Bukkit.getWorlds()) for (Item item : w.getEntitiesByClass(Item.class)) { item.remove(); count++; }
                Msg.broadcast("✓ Clearlag removed " + count + " dropped items.", NamedTextColor.GREEN);
            }, 200L);
            return true;
        }
        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        if (cmd.equals("staffhelp")) { Msg.send(p, "/invsee /vanish /spectate /freeze /unfreeze /clearlag /beadmin", NamedTextColor.YELLOW); return true; }
        if (cmd.equals("vanish")) {
            if (vanished.remove(p.getUniqueId())) {
                Bukkit.getOnlinePlayers().forEach(o -> o.showPlayer(plugin, p));
                Msg.send(p, "Vanish disabled.", NamedTextColor.YELLOW);
            } else {
                vanished.add(p.getUniqueId());
                Bukkit.getOnlinePlayers().stream().filter(o -> !o.isOp()).forEach(o -> o.hidePlayer(plugin, p));
                Msg.send(p, "Vanish enabled.", NamedTextColor.GREEN);
            }
            return true;
        }
        if (a.length < 1) { Msg.send(p, "Target required.", NamedTextColor.RED); return true; }
        Player t = Bukkit.getPlayerExact(a[0]);
        if (t == null) { Msg.send(p, "Player not found.", NamedTextColor.RED); return true; }
        switch (cmd) {
            case "invsee" -> p.openInventory(t.getInventory());
            case "spectate" -> { p.setGameMode(GameMode.SPECTATOR); p.teleport(t); Msg.send(p, "Spectating " + t.getName() + ".", NamedTextColor.GREEN); }
            case "freeze" -> { frozen.add(t.getUniqueId()); Msg.send(t, "You have been frozen by staff. Do not log out.", NamedTextColor.RED); Msg.send(p, "Frozen " + t.getName() + ".", NamedTextColor.GREEN); }
            case "unfreeze" -> { frozen.remove(t.getUniqueId()); Msg.send(t, "You have been unfrozen.", NamedTextColor.GREEN); Msg.send(p, "Unfrozen " + t.getName() + ".", NamedTextColor.GREEN); }
        }
        return true;
    }

    public boolean isFrozen(Player p) { return frozen.contains(p.getUniqueId()); }
}
