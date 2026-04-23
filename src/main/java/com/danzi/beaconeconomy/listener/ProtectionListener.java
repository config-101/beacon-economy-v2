package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class ProtectionListener implements Listener {
    private final BeaconEconomyPlugin plugin;

    public ProtectionListener(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isProtectedHub(Player player) {
        return player.getWorld().equals(plugin.getSpawnWorld()) && !player.isOp();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!isProtectedHub(event.getPlayer())) return;
        event.setCancelled(true);
        Msg.send(event.getPlayer(), "You cannot break blocks in spawn.", NamedTextColor.RED);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!isProtectedHub(event.getPlayer())) return;
        event.setCancelled(true);
        Msg.send(event.getPlayer(), "You cannot place blocks in spawn.", NamedTextColor.RED);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!isProtectedHub(event.getPlayer())) return;
        event.setCancelled(true);
        Msg.send(event.getPlayer(), "You cannot do that in spawn.", NamedTextColor.RED);
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!isProtectedHub(event.getPlayer())) return;
        event.setCancelled(true);
        Msg.send(event.getPlayer(), "You cannot do that in spawn.", NamedTextColor.RED);
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player damager)) return;
        if (victim.getWorld().equals(plugin.getSpawnWorld()) || damager.getWorld().equals(plugin.getSpawnWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().equals(plugin.getSpawnWorld())) {
            event.setCancelled(true);
        }
    }
}
