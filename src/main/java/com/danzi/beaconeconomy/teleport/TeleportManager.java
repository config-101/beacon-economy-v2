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
    private final Map<UUID, PendingTeleport> pending = new HashMap<>();

    public TeleportManager(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void queue(Player player, Location target, String reason) {
        cancel(player.getUniqueId(), false);
        int delay = plugin.getConfig().getInt("teleport-delay-seconds", 5);
        Location origin = player.getLocation().clone();
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingTeleport current = pending.remove(player.getUniqueId());
            if (current == null) return;
            if (!player.isOnline() || player.isDead()) return;
            player.teleport(target);
            Msg.send(player, "Teleported: " + reason + ".", NamedTextColor.GREEN);
        }, delay * 20L).getTaskId();
        pending.put(player.getUniqueId(), new PendingTeleport(origin, taskId));
        Msg.send(player, "Teleporting in " + delay + " seconds. Move or take damage to cancel.", NamedTextColor.YELLOW);
    }

    public void handleMove(Player player, Location to) {
        PendingTeleport pt = pending.get(player.getUniqueId());
        if (pt == null || to == null) return;
        if (movedBlock(pt.origin(), to)) {
            cancel(player.getUniqueId(), true);
        }
    }

    public void cancel(UUID uuid, boolean notify) {
        PendingTeleport pt = pending.remove(uuid);
        if (pt == null) return;
        Bukkit.getScheduler().cancelTask(pt.taskId());
        Player player = Bukkit.getPlayer(uuid);
        if (notify && player != null) {
            Msg.send(player, "Teleport cancelled.", NamedTextColor.RED);
        }
    }

    private boolean movedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ();
    }

    public record PendingTeleport(Location origin, int taskId) {}
}
