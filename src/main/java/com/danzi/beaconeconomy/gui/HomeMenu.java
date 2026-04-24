package com.danzi.beaconeconomy.gui;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class HomeMenu {
    public static final String TITLE = "Beacon Economy Homes";
    private HomeMenu() {}

    public static Inventory homes(BeaconEconomyPlugin plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        PlayerData data = plugin.data().get(p.getUniqueId());
        inv.setItem(10, item(Material.LODESTONE, label(data,1), lore(data.homes.get(1))));
        inv.setItem(13, item(Material.LODESTONE, label(data,2), lore(data.homes.get(2))));
        inv.setItem(16, item(Material.LODESTONE, label(data,3), lore(data.homes.get(3))));
        inv.setItem(22, item(Material.BOOK, "Home Rules", List.of("Max 3 homes.", "Left click: teleport.", "Right click: delete.", "No homes in intro world or nether roof.")));
        return inv;
    }
    private static String label(PlayerData data, int slot) { return data.homes.containsKey(slot) ? "Home " + slot : "Home " + slot + " (empty)"; }
    private static List<String> lore(Location loc) {
        if (loc == null) return List.of("Use /sethome " + "1-3");
        return List.of(loc.getWorld().getName(), "X " + loc.getBlockX() + " Y " + loc.getBlockY() + " Z " + loc.getBlockZ(), "Left click to teleport", "Right click to delete");
    }
    public static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }
}
