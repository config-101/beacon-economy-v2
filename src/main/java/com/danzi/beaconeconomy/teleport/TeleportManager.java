package com.danzi.beaconeconomy.teleport;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {
    private final BeaconEconomyPlugin plugin;
    private final Map<UUID, Integer> tasks = new HashMap<>();
    private final Map<UUID, Location> origins = new HashMap<>();

    public TeleportManager(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    public void queue(Player p, Location target, String reason) {
        cancel(p, false);
        int delay = plugin.getConfig().getInt("teleport-delay-seconds", 5);
        origins.put(p.getUniqueId(), p.getLocation().clone());
        int task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tasks.remove(p.getUniqueId());
            origins.remove(p.getUniqueId());
            if (p.isOnline()) {
                p.teleport(target);
                Msg.send(p, "Teleported: " + reason + ".", NamedTextColor.GREEN);
            }
        }, delay * 20L).getTaskId();
        tasks.put(p.getUniqueId(), task);
        Msg.send(p, "Teleporting in " + delay + " seconds. Do not move.", NamedTextColor.YELLOW);
    }

    public void cancel(Player p, boolean notify) {
        Integer id = tasks.remove(p.getUniqueId());
        origins.remove(p.getUniqueId());
        if (id != null) {
            Bukkit.getScheduler().cancelTask(id);
            if (notify) Msg.send(p, "Teleport cancelled.", NamedTextColor.RED);
        }
    }

    public void handleMove(Player p, Location to) {
        Location from = origins.get(p.getUniqueId());
        if (from == null || to == null) return;
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            cancel(p, true);
        }
    }
}
