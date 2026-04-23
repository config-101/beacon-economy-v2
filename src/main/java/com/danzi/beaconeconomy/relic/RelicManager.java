package com.danzi.beaconeconomy.relic;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RelicManager {
    private final BeaconEconomyPlugin plugin;
    private final NamespacedKey relicIdKey;
    private final Map<UUID, Long> riftCooldownUntil = new HashMap<>();
    private final Map<UUID, ActiveRiftState> activeRifts = new HashMap<>();

    public RelicManager(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
        this.relicIdKey = new NamespacedKey(plugin, "forgotten_relic");
    }

    public ItemStack createRiftDagger() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Rift Dagger", net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE));
        meta.lore(List.of(
            "Forgotten Relic",
            "Right-click to invoke its will.",
            "Use /relic info while holding it."
        ));
        meta.getPersistentDataContainer().set(relicIdKey, PersistentDataType.STRING, "rift_dagger");
        meta.setCustomModelData(plugin.getConfig().getInt("rift-dagger.custom-model-data", 10001));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isRiftDagger(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer().get(relicIdKey, PersistentDataType.STRING);
        return "rift_dagger".equals(id);
    }

    public long getCooldownRemaining(Player player) {
        return Math.max(0L, (riftCooldownUntil.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis()) / 1000L);
    }

    public void startCooldown(Player player) {
        long seconds = plugin.getConfig().getLong("rift-dagger.cooldown-seconds", 250L);
        riftCooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean onCooldown(Player player) {
        return getCooldownRemaining(player) > 0;
    }

    public void setActive(Player player, ActiveRiftState state) {
        clearActive(player.getUniqueId());
        activeRifts.put(player.getUniqueId(), state);
        new BukkitRunnable() {
            @Override
            public void run() {
                ActiveRiftState current = activeRifts.get(player.getUniqueId());
                if (current != null && current == state) {
                    state.onTimeout().run();
                    activeRifts.remove(player.getUniqueId());
                }
            }
        }.runTaskLater(plugin, plugin.getConfig().getLong("rift-dagger.hover-seconds", 5L) * 20L);
    }

    public ActiveRiftState getActive(Player player) {
        return activeRifts.get(player.getUniqueId());
    }

    public void clearActive(UUID uuid) {
        activeRifts.remove(uuid);
    }

    public record ActiveRiftState(org.bukkit.Location originalLocation, boolean originalAllowFlight, Runnable onTimeout) {}
}
