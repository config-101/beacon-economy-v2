package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.gui.InfoMenus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NpcListener implements Listener {
    private final BeaconEconomyPlugin plugin;

    public NpcListener(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Villager villager)) return;
        if (villager.customName() == null) return;
        String stripped = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(villager.customName());
        String key = switch (stripped) {
            case "Danzi" -> "welcome";
            case "Beacon Broker" -> "beacons";
            case "The Registrar" -> "ranks";
            case "The Warden Clerk" -> "pvp";
            case "Relic Whisperer" -> "relics";
            case "Shady Broker" -> "blackmarket";
            case "Stablemaster" -> "pets";
            case "Vault Warden" -> "spawners";
            case "Archivist" -> "faq";
            default -> null;
        };
        if (key == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.openInventory(InfoMenus.section(plugin, key));
    }
}
