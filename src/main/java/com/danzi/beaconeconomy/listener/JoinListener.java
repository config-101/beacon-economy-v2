package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class JoinListener implements Listener {
    private final BeaconEconomyPlugin plugin;

    public JoinListener(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        event.getPlayer().teleport(plugin.getSpawnLocation());
        event.getPlayer().sendMessage(Msg.line("Welcome to Beacon Economy. Use /behelp or /tutorial for information.", NamedTextColor.YELLOW));
        event.getPlayer().sendMessage(Component.text("Use /wild to leave the hub and begin survival.", NamedTextColor.GRAY));
        event.getPlayer().sendMessage(Msg.contactLine());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(plugin.getSpawnLocation());
    }
}
