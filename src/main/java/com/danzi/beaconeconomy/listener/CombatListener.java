package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {
    private static final Map<UUID, Long> combatTaggedUntil = new HashMap<>();
    private final BeaconEconomyPlugin plugin;

    public CombatListener(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) return;
        tag(damager);
        tag(victim);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        plugin.getTeleportManager().cancel(event.getPlayer().getUniqueId(), false);
        if (!isTagged(event.getPlayer().getUniqueId())) return;
        Bukkit.broadcast(Msg.line(event.getPlayer().getName() + " combat logged and was killed.", NamedTextColor.RED));
        event.getPlayer().setHealth(0.0);
        combatTaggedUntil.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        plugin.getTeleportManager().handleMove(event.getPlayer(), event.getTo());
    }

    private void tag(Player player) {
        combatTaggedUntil.put(player.getUniqueId(), System.currentTimeMillis() + 10_000L);
        Msg.send(player, "You are in combat for 10 seconds. Do not log out.", NamedTextColor.RED);
    }

    public static boolean isTagged(UUID uuid) {
        return combatTaggedUntil.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }
}
