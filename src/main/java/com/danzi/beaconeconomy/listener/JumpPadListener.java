package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JumpPadListener implements Listener {
    private final BeaconEconomyPlugin plugin;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public JumpPadListener(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().equals(plugin.getSpawnWorld())) return;
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Material below = event.getTo().clone().subtract(0, 1, 0).getBlock().getType();
        if (below != Material.TARGET) return;
        long now = System.currentTimeMillis();
        long until = cooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now < until) return;
        cooldown.put(player.getUniqueId(), now + 1500L);
        Vector dir = player.getLocation().getDirection().setY(0).normalize().multiply(0.7).setY(1.1);
        player.setVelocity(dir);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.7f);
        player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation(), 30, 0.4, 0.1, 0.4, 0.01);
    }
}
