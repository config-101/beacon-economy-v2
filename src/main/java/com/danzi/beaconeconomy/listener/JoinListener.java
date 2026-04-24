package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.PlayerData;
import com.danzi.beaconeconomy.gui.InfoMenus;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinListener implements Listener {
    private final BeaconEconomyPlugin plugin;
    public JoinListener(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData data = plugin.data().get(p.getUniqueId());
        if (!data.introComplete) {
            p.teleport(plugin.introLocation());
            p.setAllowFlight(true);
            p.setFlying(true);
            Msg.send(p, "You wake above the void. Read the guide, then choose Wild to begin.", NamedTextColor.LIGHT_PURPLE);
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(InfoMenus.intro(plugin, p)), 20L);
            new BukkitRunnable() {
                int ticks = 0;
                @Override public void run() {
                    if (!p.isOnline() || plugin.data().get(p.getUniqueId()).introComplete) { cancel(); return; }
                    p.spawnParticle(Particle.PORTAL, p.getLocation(), 40, 1.2, 1.2, 1.2, 0.05);
                    p.spawnParticle(Particle.REVERSE_PORTAL, p.getLocation().add(0,-0.5,0), 10, 0.6, 0.2, 0.6, 0.01);
                    if (ticks % 80 == 0) p.playSound(p.getLocation(), Sound.AMBIENT_BASALT_DELTAS_MOOD, 0.45f, 0.6f);
                    ticks += 10;
                }
            }.runTaskTimer(plugin, 0L, 10L);
        }
    }
}
