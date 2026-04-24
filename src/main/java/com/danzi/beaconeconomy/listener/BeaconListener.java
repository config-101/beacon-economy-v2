package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BeaconListener implements Listener {
    private final BeaconEconomyPlugin plugin;
    private final Map<UUID, Long> pending = new HashMap<>();

    public BeaconListener(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.BEACON) return;
        if (e.getPlayer().getWorld().equals(plugin.introWorld())) {
            e.setCancelled(true);
            Msg.send(e.getPlayer(), "Personal beacons cannot be placed in the intro world.", NamedTextColor.RED);
            return;
        }
        long now = System.currentTimeMillis();
        Long until = pending.get(e.getPlayer().getUniqueId());
        if (until == null || until < now) {
            e.setCancelled(true);
            pending.put(e.getPlayer().getUniqueId(), now + 30000L);
            Msg.send(e.getPlayer(), "You are about to place a permanent personal beacon. Place it again within 30 seconds to confirm.", NamedTextColor.YELLOW);
            Msg.send(e.getPlayer(), "Once placed, it cannot be picked back up. If destroyed, it is gone forever.", NamedTextColor.RED);
        } else {
            pending.remove(e.getPlayer().getUniqueId());
            Msg.broadcast(e.getPlayer().getName() + " has placed a personal beacon.", NamedTextColor.GOLD);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.BEACON && !e.getPlayer().isOp()) {
            e.setDropItems(false);
            Msg.send(e.getPlayer(), "That beacon is gone forever.", NamedTextColor.RED);
        }
    }
}
