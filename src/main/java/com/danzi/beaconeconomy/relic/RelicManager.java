package com.danzi.beaconeconomy.relic;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

public class RelicManager {
    private final BeaconEconomyPlugin plugin;
    private final NamespacedKey relicKey;
    private final Map<UUID, Map<RelicType, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, ActiveRift> activeRifts = new HashMap<>();
    private final Map<String, Long> auraCooldowns = new HashMap<>();

    public RelicManager(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
        this.relicKey = new NamespacedKey(plugin, "forgotten_relic");
    }

    public ItemStack create(RelicType type) {
        ItemStack it = new ItemStack(type.material);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text(type.display, NamedTextColor.DARK_PURPLE));
        m.lore(List.of(
            Component.text("Forgotten Relic", NamedTextColor.GRAY),
            Component.text(type.shortInfo, NamedTextColor.LIGHT_PURPLE),
            Component.text("Use /relic info while holding it.", NamedTextColor.GRAY)
        ));
        m.getPersistentDataContainer().set(relicKey, PersistentDataType.STRING, type.id);
        m.setCustomModelData(10000 + type.ordinal() + 1);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    public RelicType type(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return null;
        String id = it.getItemMeta().getPersistentDataContainer().get(relicKey, PersistentDataType.STRING);
        return id == null ? null : RelicType.byId(id);
    }

    public boolean isRelic(ItemStack it) { return type(it) != null; }

    public boolean isNullshardSuppressed(Player p) {
        double radius = plugin.getConfig().getDouble("relics.nullshard-radius", 50);
        for (Player other : p.getWorld().getPlayers()) {
            if (other.getLocation().distanceSquared(p.getLocation()) <= radius * radius && playerHasRelic(other, RelicType.NULLSHARD)) return true;
        }
        return false;
    }

    public boolean playerHasRelic(Player p, RelicType type) {
        for (ItemStack it : p.getInventory().getContents()) if (type(it) == type) return true;
        return false;
    }

    public long cooldown(Player p, RelicType type) {
        return Math.max(0, cooldowns.getOrDefault(p.getUniqueId(), Map.of()).getOrDefault(type, 0L) - System.currentTimeMillis()) / 1000L;
    }

    public void setCooldown(Player p, RelicType type) {
        cooldowns.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(RelicType.class)).put(type, System.currentTimeMillis() + type.cooldownSeconds * 1000L);
    }

    public void info(Player p, RelicType type) {
        Msg.send(p, "Forgotten Relic: " + type.display, NamedTextColor.DARK_PURPLE);
        switch (type) {
            case RIFT_DAGGER -> {
                Msg.send(p, "Cannot attack. Right-click: rise 20 blocks, hover 5s, select target within 70 blocks, then slash reality.", NamedTextColor.LIGHT_PURPLE);
                Msg.send(p, "Cooldown: 250s. Failing to choose a target returns and punishes you.", NamedTextColor.GRAY);
            }
            case VOID_PULSE_CONTROL -> {
                Msg.send(p, "Right-click target within 30 blocks to pull it to you. If no target, releases a 15-block void push.", NamedTextColor.LIGHT_PURPLE);
                Msg.send(p, "Closer entities are hit harder and pushed farther.", NamedTextColor.GRAY);
            }
            case NULLSHARD -> Msg.send(p, "Passive: within 50 blocks, relics cannot activate, active magic is suppressed, effects are dispelled, and relic carriers glow to you.", NamedTextColor.LIGHT_PURPLE);
            default -> {
                Msg.send(p, type.shortInfo, NamedTextColor.LIGHT_PURPLE);
                Msg.send(p, "Prototype ability is enabled in this systems build. Final tuning comes in later jumps.", NamedTextColor.GRAY);
            }
        }
    }

    public void use(Player p, RelicType type) {
        if (type == RelicType.NULLSHARD) { Msg.send(p, "The Nullshard is already awake around you.", NamedTextColor.LIGHT_PURPLE); return; }
        if (isNullshardSuppressed(p) && type != RelicType.NULLSHARD) { Msg.send(p, "A Nullshard nearby smothers the relic's power.", NamedTextColor.RED); return; }
        if (cooldown(p, type) > 0) { Msg.send(p, "The relic is dormant for " + cooldown(p, type) + "s.", NamedTextColor.RED); return; }
        switch (type) {
            case RIFT_DAGGER -> useRiftDagger(p);
            case VOID_PULSE_CONTROL -> useVoidPulse(p);
            default -> usePrototype(p, type);
        }
    }

    private void usePrototype(Player p, RelicType type) {
        setCooldown(p, type);
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 80, 1, 1, 1, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.6f);
        switch (type) {
            case BLACK_LEDGER -> {
                RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getLocation().getDirection(), 30, e -> e instanceof LivingEntity && e != p);
                if (r != null && r.getHitEntity() instanceof LivingEntity le) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 12*20, 1));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 12*20, 1));
                    Msg.send(p, "Debt has been marked.", NamedTextColor.DARK_PURPLE);
                }
            }
            case GRAVE_COIL -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 10*20, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 8*20, 1));
            }
            case ASHWAKE_IDOL -> {
                for (Entity e : p.getNearbyEntities(8, 4, 8)) if (e instanceof LivingEntity le && e != p) { le.setFireTicks(80); le.damage(6.0, p); }
            }
            case HUNTERS_EYE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20*20, 1));
                for (Player other : p.getWorld().getPlayers()) if (!other.equals(p) && other.getLocation().distanceSquared(p.getLocation()) < 160*160) other.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 10*20, 0));
            }
            case WARDEN_FANG -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 15*20, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 15*20, 0));
            }
            case STORM_LANTERN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 12*20, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 12*20, 1));
                for (Entity e : p.getNearbyEntities(7, 4, 7)) e.setVelocity(e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.4).setY(0.5));
            }
            case CROWN_LAST_KING -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 10*20, 1));
                for (Entity e : p.getNearbyEntities(10, 4, 10)) if (e instanceof LivingEntity le && e != p) le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 10*20, 1));
            }
            default -> {}
        }
    }

    private void useVoidPulse(Player p) {
        setCooldown(p, RelicType.VOID_PULSE_CONTROL);
        RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getLocation().getDirection(), 30, e -> e instanceof LivingEntity && e != p);
        if (r != null && r.getHitEntity() instanceof LivingEntity target) {
            Vector v = p.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(3.2);
            target.setVelocity(v.setY(0.25));
            target.damage(8.0, p);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.4f);
            target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation(), 60, 0.8, 0.8, 0.8, 0.1);
            Msg.send(p, "The void pulls your target violently toward you.", NamedTextColor.DARK_PURPLE);
            return;
        }
        for (Entity e : p.getNearbyEntities(15, 8, 15)) {
            if (e instanceof LivingEntity le && e != p) {
                double d = Math.max(1.0, e.getLocation().distance(p.getLocation()));
                double strength = Math.max(0.6, (16 - d) / 4.0);
                Vector push = e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(strength).setY(0.45);
                e.setVelocity(push);
                le.damage(Math.max(3.0, 14.0 - d), p);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(40 + (16-d)*8), 1));
                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int)(40 + (16-d)*5), 0));
            }
        }
        p.getWorld().spawnParticle(Particle.SONIC_BOOM, p.getLocation(), 1);
        p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, p.getLocation(), 120, 3, 2, 3, 0.2);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.7f);
    }

    private void useRiftDagger(Player p) {
        for (int y = 1; y <= 20; y++) if (!p.getLocation().clone().add(0,y,0).getBlock().isPassable()) { Msg.send(p, "The relic refuses to open the rift here. Something above you chokes its path.", NamedTextColor.RED); return; }
        setCooldown(p, RelicType.RIFT_DAGGER);
        Location original = p.getLocation().clone();
        boolean allow = p.getAllowFlight();
        activeRifts.put(p.getUniqueId(), new ActiveRift(original, allow));
        p.setAllowFlight(true); p.setFlying(true);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 6*20, 0, true, false, false));
        p.teleport(original.clone().add(0, 20, 0));
        p.getWorld().spawnParticle(Particle.PORTAL, original, 100, 1, 2, 1, 0.2);
        p.getWorld().playSound(original, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        Msg.send(p, "The Rift Dagger tears the world open beneath you.", NamedTextColor.DARK_PURPLE);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ActiveRift state = activeRifts.remove(p.getUniqueId());
            if (state != null && p.isOnline()) {
                p.teleport(state.original);
                p.setAllowFlight(state.allowFlight);
                p.setFlying(false);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5*20, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2*20, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 2*20, 0));
                Msg.send(p, "The rift collapses on you.", NamedTextColor.RED);
            }
        }, 5*20L);
    }

    public boolean finishRift(Player p) {
        ActiveRift state = activeRifts.remove(p.getUniqueId());
        if (state == null) return false;
        RayTraceResult ray = p.rayTraceBlocks(70);
        if (ray == null || ray.getHitBlock() == null) {
            activeRifts.put(p.getUniqueId(), state);
            Msg.send(p, "No valid rift target found.", NamedTextColor.RED);
            return true;
        }
        Location dest = ray.getHitBlock().getLocation().add(0.5, 1, 0.5);
        p.teleport(dest);
        p.setAllowFlight(state.allowFlight);
        p.setFlying(false);
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, dest, 8, 2, 1, 2, 0.1);
        p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, dest, 140, 3, 2, 3, 0.2);
        p.getWorld().playSound(dest, Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.7f);
        for (Entity e : p.getNearbyEntities(4.5, 4.5, 4.5)) {
            if (e instanceof LivingEntity le && e != p) {
                le.damage(16.0, p);
                double ang = Math.random() * Math.PI * 2;
                double rad = Math.random() * 15;
                Location t = le.getLocation().add(Math.cos(ang)*rad, 0, Math.sin(ang)*rad);
                le.teleport(t);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5*20, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2*20, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 2*20, 0));
            }
        }
        return true;
    }

    public void reclaim() {
        Msg.broadcast("The void has reclaimed one of its belongings.", NamedTextColor.DARK_PURPLE);
    }

    public void startTasks() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double radius = plugin.getConfig().getDouble("relics.aura-radius", 20);
            long cd = plugin.getConfig().getLong("relics.aura-cooldown-minutes", 20) * 60_000L;
            for (Player holder : Bukkit.getOnlinePlayers()) {
                RelicType held = heldRelic(holder);
                if (held == null) continue;
                for (Player near : holder.getWorld().getPlayers()) {
                    if (near.equals(holder)) continue;
                    if (near.getLocation().distanceSquared(holder.getLocation()) <= radius*radius) {
                        String key = near.getUniqueId() + ":" + held.id;
                        long last = auraCooldowns.getOrDefault(key, 0L);
                        if (System.currentTimeMillis() - last >= cd) {
                            auraCooldowns.put(key, System.currentTimeMillis());
                            Msg.send(near, "You feel an overwhelming aura of power nearby.", NamedTextColor.DARK_PURPLE);
                            near.playSound(near.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.6f, 0.7f);
                        }
                    }
                }
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (playerHasRelic(p, RelicType.NULLSHARD)) {
                    for (Player other : p.getWorld().getPlayers()) {
                        if (!other.equals(p) && other.getLocation().distanceSquared(p.getLocation()) <= 50*50 && heldRelic(other) != null) {
                            other.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false, false));
                        }
                    }
                }
            }
        }, 40L, 40L);
    }

    private RelicType heldRelic(Player p) {
        RelicType t = type(p.getInventory().getItemInMainHand());
        if (t != null) return t;
        return type(p.getInventory().getItemInOffHand());
    }

    public record ActiveRift(Location original, boolean allowFlight) {}
}
