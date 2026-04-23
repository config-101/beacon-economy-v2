package com.danzi.beaconeconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public final class BeaconEconomyPlugin extends org.bukkit.plugin.java.JavaPlugin implements Listener {
    private static final String BRAND = ChatColor.LIGHT_PURPLE + "✦ Beacon Economy ✦ " + ChatColor.GRAY;
    private static final String SPAWN_WORLD = "beacon_spawn";
    private static final int SHOCKVEIL_MODEL_DATA = 10002;
    private static final long SHOCKVEIL_COOLDOWN_MS = 200_000L;
    private static final double SHOCKVEIL_RADIUS = 15.0;

    private NamespacedKey relicKey;
    private final Map<UUID, Long> shockveilCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        this.relicKey = new NamespacedKey(this, "forgotten_relic");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Beacon Economy test build enabled. Shockveil is active.");
    }

    @Override
    public void onDisable() {
        shockveilCooldowns.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isShockveil(item)) return;

        event.setCancelled(true);
        castShockveil(player);
    }

    private void castShockveil(Player player) {
        if (player.getWorld().getName().equalsIgnoreCase(SPAWN_WORLD)) {
            player.sendMessage(BRAND + "Shockveil refuses to fracture the spawn veil.");
            return;
        }

        long now = System.currentTimeMillis();
        long readyAt = shockveilCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now + 999L) / 1000L);
            player.sendMessage(BRAND + "Shockveil is still reforming. " + seconds + "s remaining.");
            return;
        }

        shockveilCooldowns.put(player.getUniqueId(), now + SHOCKVEIL_COOLDOWN_MS);
        player.sendMessage(BRAND + "The veil shatters outward.");

        Location origin = player.getLocation().clone().add(0, 1.0, 0);
        World world = player.getWorld();

        playShockveilSounds(world, origin);
        spawnShockveilParticles(world, origin);
        applyCasterLockIn(player);
        affectNearbyEntities(player, origin);
    }

    private void affectNearbyEntities(Player caster, Location origin) {
        World world = caster.getWorld();
        Collection<Entity> nearby = world.getNearbyEntities(origin, SHOCKVEIL_RADIUS, SHOCKVEIL_RADIUS, SHOCKVEIL_RADIUS);

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity target)) continue;
            if (entity.equals(caster)) continue;
            if (isCastersPet(caster, entity)) continue;

            Location targetLoc = target.getLocation();
            double distance = targetLoc.distance(origin);
            if (distance <= 0.25 || distance > SHOCKVEIL_RADIUS) continue;

            Vector direction = targetLoc.toVector().subtract(origin.toVector()).normalize();

            // Strong horizontal force intended to create roughly 15 blocks of displacement in open terrain.
            double horizontal = 2.65;
            double vertical = distance <= 5.0 ? 0.75 : 0.55;
            Vector velocity = direction.multiply(horizontal).setY(vertical);
            target.setVelocity(velocity);

            if (distance <= 5.0) {
                target.damage(9.0, caster);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, true, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, true, true, true));
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 0.65f, 0.8f);
                maybeVeilTear(target, caster);
            } else if (distance <= 10.0) {
                target.damage(6.0, caster);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true, true));
            } else {
                target.damage(4.0, caster);
            }
        }
    }

    private void maybeVeilTear(LivingEntity target, Player caster) {
        if (Math.random() > 0.20) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) return;
                Location base = target.getLocation();
                for (int attempts = 0; attempts < 8; attempts++) {
                    double x = (Math.random() * 12.0) - 6.0;
                    double z = (Math.random() * 12.0) - 6.0;
                    Location candidate = base.clone().add(x, 0, z);
                    Location safe = findSafeNearby(candidate);
                    if (safe != null) {
                        target.teleport(safe);
                        target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, safe.clone().add(0, 1, 0), 35, 0.5, 0.8, 0.5, 0.04);
                        target.getWorld().playSound(safe, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.7f);
                        return;
                    }
                }
            }
        }.runTaskLater(this, 18L);
    }

    private Location findSafeNearby(Location location) {
        World world = location.getWorld();
        if (world == null) return null;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int startY = Math.min(world.getMaxHeight() - 2, Math.max(world.getMinHeight() + 1, location.getBlockY() + 3));

        for (int y = startY; y >= world.getMinHeight() + 1; y--) {
            Location feet = new Location(world, x + 0.5, y, z + 0.5);
            Material below = feet.clone().add(0, -1, 0).getBlock().getType();
            Material feetMat = feet.getBlock().getType();
            Material headMat = feet.clone().add(0, 1, 0).getBlock().getType();
            if (below.isSolid() && feetMat.isAir() && headMat.isAir()) return feet;
        }
        return null;
    }

    private void applyCasterLockIn(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, true, true, true));
    }

    private void playShockveilSounds(World world, Location loc) {
        world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.6f);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.85f, 0.5f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.7f);
        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.5f);
    }

    private void spawnShockveilParticles(World world, Location origin) {
        world.spawnParticle(Particle.SONIC_BOOM, origin, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.PORTAL, origin, 120, 1.2, 1.0, 1.2, 0.18);
        world.spawnParticle(Particle.REVERSE_PORTAL, origin, 80, 0.9, 0.8, 0.9, 0.08);
        world.spawnParticle(Particle.SMOKE, origin, 90, 1.6, 0.35, 1.6, 0.04);
        world.spawnParticle(Particle.ASH, origin, 120, 1.8, 0.3, 1.8, 0.02);

        new BukkitRunnable() {
            int step = 1;
            @Override
            public void run() {
                if (step > 15) {
                    cancel();
                    return;
                }
                double radius = step;
                int points = Math.max(24, step * 10);
                for (int i = 0; i < points; i++) {
                    double angle = (Math.PI * 2.0 * i) / points;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location point = origin.clone().add(x, -0.65, z);
                    world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(80, 0, 120), 1.35f));
                    if (step % 3 == 0 && i % 8 == 0) {
                        world.spawnParticle(Particle.SMOKE, point.clone().add(0, 0.15, 0), 2, 0.08, 0.02, 0.08, 0.01);
                    }
                }
                step++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private boolean isCastersPet(Player caster, Entity entity) {
        if (!(entity instanceof Tameable tameable)) return false;
        if (!tameable.isTamed()) return false;
        return tameable.getOwner() != null && caster.getUniqueId().equals(tameable.getOwner().getUniqueId());
    }

    private boolean isShockveil(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String value = pdc.get(relicKey, PersistentDataType.STRING);
        return "shockveil".equals(value);
    }

    private ItemStack createShockveil() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "✦ Shockveil ✦");
        meta.setLore(List.of(
                ChatColor.GRAY + "A forgotten scroll sealed beneath a veil of pressure.",
                ChatColor.GRAY + "Right-click to shatter the space around you.",
                ChatColor.DARK_GRAY + "Those closest to the fracture suffer most.",
                "",
                ChatColor.LIGHT_PURPLE + "Forgotten Relic #2"
        ));
        meta.setCustomModelData(SHOCKVEIL_MODEL_DATA);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(relicKey, PersistentDataType.STRING, "shockveil");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shockveiltest")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("beaconeconomy.admin")) {
                player.sendMessage(BRAND + "You do not have permission.");
                return true;
            }
            player.getInventory().addItem(createShockveil());
            player.sendMessage(BRAND + "Shockveil test relic added to your inventory.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("relic")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
                if (isShockveil(player.getInventory().getItemInMainHand())) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "✦ Beacon Economy ✦ " + ChatColor.DARK_PURPLE + "Shockveil");
                    player.sendMessage(ChatColor.GRAY + "Right-click to unleash a violent pulse that forces everything away from you.");
                    player.sendMessage(ChatColor.GRAY + "Those closest to the fracture suffer the worst of it.");
                    player.sendMessage(ChatColor.GRAY + "Cooldown: 200 seconds");
                } else {
                    player.sendMessage(BRAND + "Hold a Forgotten Relic and use /relic info.");
                }
                return true;
            }
        }
        return false;
    }
}
