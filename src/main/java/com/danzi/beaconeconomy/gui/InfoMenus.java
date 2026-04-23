package com.danzi.beaconeconomy.gui;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class InfoMenus {
    public static final String HELP_TITLE = "Beacon Economy Help";
    public static final String TUTORIAL_TITLE = "Beacon Economy Tutorial";

    private InfoMenus() {}

    public static Inventory help(BeaconEconomyPlugin plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, HELP_TITLE);
        inv.setItem(10, HomeMenu.simple(Material.BEACON, "Core Commands", List.of(
            "/spawn, /wild, /tutorial, /behelp",
            "/sethome <1-3>, /home <1-3>, /delhome <1-3>, /homes",
            "/relic info"
        )));
        inv.setItem(13, HomeMenu.simple(Material.NETHERITE_SWORD, "Forgotten Relics", List.of(
            "Legendary one-of-a-kind relics.",
            "Use /relic info while holding one.",
            plugin.getConfig().getString("contact-line", "Contact Danzi on Discord if you have questions.")
        )));
        inv.setItem(16, HomeMenu.simple(Material.BOOK, "About", List.of(
            "Beacon Economy server and plugin made by Danzi.",
            "Use /tutorial for quick guidance.",
            plugin.getConfig().getString("contact-line", "Contact Danzi on Discord if you have questions.")
        )));
        return inv;
    }

    public static Inventory tutorial(BeaconEconomyPlugin plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TUTORIAL_TITLE);
        inv.setItem(10, HomeMenu.simple(Material.BEACON, "Spawn Beacon", List.of(
            "The public spawn beacon is weaker than your own.",
            "For now, this test pack includes the hub setup only."
        )));
        inv.setItem(13, HomeMenu.simple(Material.COMPASS, "Travel", List.of(
            "/wild sends you to wilderness after a short delay.",
            "/spawn returns you to the protected hub.",
            "All non-surrender teleports are delayed."
        )));
        inv.setItem(16, HomeMenu.simple(Material.ENDER_PEARL, "Forgotten Relics", List.of(
            "Rare unique relics with custom powers.",
            "The Rift Dagger is included for this test.",
            plugin.getConfig().getString("contact-line", "Contact Danzi on Discord if you have questions.")
        )));
        return inv;
    }
}
