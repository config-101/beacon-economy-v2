package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.*;

public class CombatListener implements Listener {
    private final BeaconEconomyPlugin plugin;
    private final Map<UUID, Long> taggedUntil = new HashMap<>();
    public CombatListener(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @EventHandler public void onPvP(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (e.getDamager() instanceof Player a && e.getEntity() instanceof Player b) {
            long until = System.currentTimeMillis() + plugin.getConfig().getLong("combat-tag-seconds", 10L) * 1000L;
            taggedUntil.put(a.getUniqueId(), until); taggedUntil.put(b.getUniqueId(), until);
            Msg.send(a, "You are combat tagged. Do not log out.", NamedTextColor.RED);
            Msg.send(b, "You are combat tagged. Do not log out.", NamedTextColor.RED);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (isTagged(p)) {
            p.setHealth(0.0);
            Msg.broadcast(p.getName() + " combat logged and was killed.", NamedTextColor.RED);
        }
    }

    public boolean isTagged(Player p) {
        return taggedUntil.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }
}
