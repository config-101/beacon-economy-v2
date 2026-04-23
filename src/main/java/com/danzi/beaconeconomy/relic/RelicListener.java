package com.danzi.beaconeconomy.relic;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Random;

public class RelicListener implements Listener {
    private final BeaconEconomyPlugin plugin;
    private final RelicManager relicManager;
    private final Random random = new Random();

    public RelicListener(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
        this.relicManager = plugin.getRelicManager();
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!relicManager.isRiftDagger(hand)) return;
        event.setCancelled(true);
        Msg.send(player, "The blade does not answer your violence. It serves a will older than your own.", NamedTextColor.DARK_PURPLE);
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!relicManager.isRiftDagger(hand)) return;

        RelicManager.ActiveRiftState active = relicManager.getActive(player);
        if (active != null) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                Block targetBlock = resolveTargetBlock(player, event.getClickedBlock());
                if (targetBlock == null) {
                    Msg.send(player, "The relic finds no valid place to sever.", NamedTextColor.RED);
                    return;
                }
                handlePhaseTwo(player, targetBlock, active);
            }
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        if (relicManager.onCooldown(player)) {
            Msg.send(player, "The relic is dormant for " + relicManager.getCooldownRemaining(player) + " more seconds.", NamedTextColor.RED);
            return;
        }
        if (hasBlocksAbove(player)) {
            Msg.send(player, "The relic refuses to open the rift here. Something above you chokes its path.", NamedTextColor.RED);
            return;
        }
        activateRift(player);
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (!relicManager.isRelic(event.getEntity().getItemStack())) return;
        relicManager.reclaimRelic(event.getEntity().getItemStack());
    }

    @EventHandler
    public void onRelicItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        if (!relicManager.isRelic(item.getItemStack())) return;
        switch (event.getCause()) {
            case VOID, LAVA, FIRE, FIRE_TICK, CONTACT, ENTITY_EXPLOSION, BLOCK_EXPLOSION -> {
                event.setCancelled(true);
                relicManager.reclaimRelic(item.getItemStack());
                item.remove();
            }
            default -> {}
        }
    }

    private Block resolveTargetBlock(Player player, Block clickedBlock) {
        double max = plugin.getConfig().getDouble("rift-dagger.max-target-distance", 70.0);
        RayTraceResult trace = player.rayTraceBlocks(max, FluidCollisionMode.NEVER);
        Block hit = trace != null ? trace.getHitBlock() : null;
        if (hit == null) hit = clickedBlock;
        if (hit == null) {
            BlockIterator iterator = new BlockIterator(player, (int) Math.ceil(max));
            while (iterator.hasNext()) {
                Block next = iterator.next();
                if (!next.isEmpty()) hit = next;
            }
        }
        if (hit == null) return null;
        Block feet = hit.getRelative(0, 1, 0);
        Block head = hit.getRelative(0, 2, 0);
        if (!feet.isPassable() || !head.isPassable()) return null;
        return hit;
    }

    private boolean hasBlocksAbove(Player player) {
        Location location = player.getLocation();
        World world = player.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        for (int y = location.getBlockY() + 1; y < world.getMaxHeight(); y++) {
            if (!world.getBlockAt(x, y, z).isEmpty()) return true;
        }
        return false;
    }

    private void activateRift(Player player) {
        Location original = player.getLocation().clone();
        Location hover = original.clone().add(0, plugin.getConfig().getDouble("rift-dagger.rise-height", 20.0), 0);
        boolean originalAllowFlight = player.getAllowFlight();
        player.getWorld().spawnParticle(Particle.PORTAL, original, 200, 0.6, 1.0, 0.6, 0.2);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, hover, 120, 0.5, 0.8, 0.5, 0.1);
        player.getWorld().playSound(original, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
        player.teleport(hover);
        player.setAllowFlight(true);
        player.setFlying(true);
        int hoverTicks = 20 * plugin.getConfig().getInt("rift-dagger.hover-seconds", 5);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, hoverTicks, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, hoverTicks, 1, false, false, false));
        Msg.send(player, "The Rift Dagger tears the world open beneath you.", NamedTextColor.LIGHT_PURPLE);
        relicManager.startCooldown(player);
        relicManager.setActive(player, new RelicManager.ActiveRiftState(original, originalAllowFlight, () -> failReturn(player, original, originalAllowFlight)));
    }

    private void handlePhaseTwo(Player player, Block clicked, RelicManager.ActiveRiftState active) {
        Location target = clicked.getLocation().clone().add(0.5, 1.0, 0.5);
        if (!clicked.getWorld().equals(player.getWorld())) {
            Msg.send(player, "The relic cannot reach that place.", NamedTextColor.RED);
            return;
        }
        if (player.getLocation().distanceSquared(target) > Math.pow(plugin.getConfig().getDouble("rift-dagger.max-target-distance", 70.0), 2)) {
            Msg.send(player, "That destination is beyond the relic's reach.", NamedTextColor.RED);
            return;
        }
        relicManager.clearActive(player.getUniqueId());
        player.setFlying(false);
        player.setAllowFlight(active.originalAllowFlight());
        player.teleport(target);
        World world = player.getWorld();
        world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.4f);
        world.playSound(target, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.4f);
        world.spawnParticle(Particle.DRAGON_BREATH, target, 180, 1.2, 1.0, 1.2, 0.05);
        world.spawnParticle(Particle.PORTAL, target, 150, 1.2, 0.4, 1.2, 0.2);

        double radius = plugin.getConfig().getDouble("rift-dagger.slash-radius", 4.5);
        double displacement = plugin.getConfig().getDouble("rift-dagger.displacement-radius", 15.0);
        for (Entity entity : world.getNearbyEntities(target, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(player)) continue;
            if (entity instanceof Tameable tameable && tameable.getOwnerUniqueId() != null && tameable.getOwnerUniqueId().equals(player.getUniqueId())) continue;
            living.damage(16.0, player);
            Location displaced = target.clone().add(random.nextDouble() * displacement * (random.nextBoolean() ? 1 : -1), 0, random.nextDouble() * displacement * (random.nextBoolean() ? 1 : -1));
            displaced.setY(world.getHighestBlockYAt(displaced) + 1.0);
            living.teleport(displaced);
            living.addPotionEffects(List.of(
                new PotionEffect(PotionEffectType.SLOWNESS, 20 * 5, 1, false, true, true),
                new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0, false, true, true),
                new PotionEffect(PotionEffectType.NAUSEA, 20 * 2, 0, false, true, true)
            ));
            world.spawnParticle(Particle.REVERSE_PORTAL, displaced, 25, 0.3, 0.6, 0.3, 0.05);
        }
    }

    private void failReturn(Player player, Location original, boolean originalAllowFlight) {
        if (!player.isOnline()) return;
        player.teleport(original);
        player.setFlying(false);
        player.setAllowFlight(originalAllowFlight);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 5, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 2, 0, false, true, true));
        player.getWorld().playSound(original, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.7f);
        player.getWorld().spawnParticle(Particle.SMOKE, original, 50, 0.6, 0.4, 0.6, 0.02);
        Msg.send(player, "The rift collapses and throws its burden back upon you.", NamedTextColor.RED);
    }
}
