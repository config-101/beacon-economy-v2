package com.danzi.beaconeconomy.listener;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.relic.RelicType;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class RelicListener implements Listener {
    private final BeaconEconomyPlugin plugin;
    public RelicListener(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        RelicType type = plugin.relics().type(hand);
        if (type == null) return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (plugin.relics().finishRift(p)) return;
            plugin.relics().use(p, type);
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            RelicType type = plugin.relics().type(p.getInventory().getItemInMainHand());
            if (type != null) {
                if (type == RelicType.RIFT_DAGGER) {
                    e.setCancelled(true);
                    Msg.send(p, "The blade does not answer your violence. It serves a will older than your own.", NamedTextColor.DARK_PURPLE);
                }
            }
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent e) {
        if (plugin.relics().isRelic(e.getEntity().getItemStack())) plugin.relics().reclaim();
    }

    @EventHandler
    public void onItemDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Item item && plugin.relics().isRelic(item.getItemStack())) {
            if (e.getCause() == EntityDamageEvent.DamageCause.VOID || e.getCause() == EntityDamageEvent.DamageCause.LAVA || e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || e.getCause() == EntityDamageEvent.DamageCause.CONTACT || e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                item.remove();
                plugin.relics().reclaim();
            }
        }
    }
}
