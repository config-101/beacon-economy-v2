package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

public class ProtectionListener implements Listener {
    private final BeaconEconomyPlugin plugin;
    public ProtectionListener(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    private boolean intro(Player p) { return p.getWorld().equals(plugin.introWorld()); }
    private boolean locked(Player p) { return !plugin.data().get(p.getUniqueId()).introComplete; }

    @EventHandler public void onMove(PlayerMoveEvent e) {
        plugin.teleports().handleMove(e.getPlayer(), e.getTo());
        if (locked(e.getPlayer()) && e.getTo()!=null) {
            e.setTo(e.getFrom());
        }
    }
    @EventHandler public void onDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            plugin.teleports().cancel(p, true);
            if (intro(p) || locked(p)) e.setCancelled(true);
        }
    }
    @EventHandler public void onBreak(BlockBreakEvent e) { if (intro(e.getPlayer()) && !e.getPlayer().isOp()) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (intro(e.getPlayer()) && !e.getPlayer().isOp()) e.setCancelled(true); }
    @EventHandler public void onPvP(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player a && e.getEntity() instanceof Player b) {
            if (intro(a) || intro(b)) e.setCancelled(true);
        }
    }
    @EventHandler public void onInteract(PlayerInteractEvent e) { if (intro(e.getPlayer()) && locked(e.getPlayer())) e.setCancelled(true); }
}
