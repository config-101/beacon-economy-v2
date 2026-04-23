package com.danzi.beaconeconomy.gui;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class HomeMenu {
    public static final String TITLE = "Beacon Economy Homes";
    private HomeMenu() {}

    public static Inventory create(BeaconEconomyPlugin plugin, Player player) {
        HomeManager manager = plugin.getHomeManager();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        inv.setItem(10, homeItem(manager, player, 1));
        inv.setItem(13, homeItem(manager, player, 2));
        inv.setItem(16, homeItem(manager, player, 3));
        inv.setItem(22, simple(Material.BOOK, "How to use", List.of("Left click a home to teleport.", "Right click a home to delete it.", "Use /sethome <1-3> to create one.")));
        return inv;
    }

    private static ItemStack homeItem(HomeManager manager, Player player, int slot) {
        Location home = manager.getHome(player.getUniqueId(), slot);
        if (home == null) {
            return simple(Material.GRAY_DYE, "Home " + slot + " (empty)", List.of("Use /sethome " + slot));
        }
        List<String> lore = new ArrayList<>();
        lore.add(home.getWorld().getName());
        lore.add("X: " + home.getBlockX() + " Y: " + home.getBlockY() + " Z: " + home.getBlockZ());
        lore.add("Left click: teleport");
        lore.add("Right click: delete");
        return simple(Material.LODESTONE, "Home " + slot, lore);
    }

    public static ItemStack simple(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
