package com.danzi.beaconeconomy.gui;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.List;

public final class InfoMenus {
    public static final String INTRO_TITLE = "Beacon Economy Intro";
    public static final String HELP_TITLE = "Beacon Economy Help";
    public static final String COMMANDS_TITLE = "Beacon Economy Commands";
    public static final String ADMIN_TITLE = "Beacon Economy Admin";
    private InfoMenus() {}

    public static Inventory intro(BeaconEconomyPlugin plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, INTRO_TITLE);
        inv.setItem(10, HomeMenu.item(Material.BEACON, "Beacon Economy", List.of("Dark PvP economy survival.", "Make money, rank up, hunt relics.", plugin.getConfig().getString("contact-line"))));
        inv.setItem(12, HomeMenu.item(Material.NETHER_STAR, "Forgotten Relics", List.of("10 unique relics exist.", "Only one of each can exist.", "Use /relic info while holding one.")));
        inv.setItem(14, HomeMenu.item(Material.COMPASS, "Start Your Journey", List.of("Click here to use /wild.", "You will be released into survival.")));
        inv.setItem(16, HomeMenu.item(Material.BOOK, "Help", List.of("Use /behelp, /tutorial, or /commands.", "All common questions are answered there.")));
        return inv;
    }

    public static Inventory help(BeaconEconomyPlugin plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, HELP_TITLE);
        inv.setItem(10, HomeMenu.item(Material.COMPASS, "Getting Started", List.of("Use /wild to start.", "Use /spawn to return to intro void.", "Teleports have a 5s delay.")));
        inv.setItem(11, HomeMenu.item(Material.BEACON, "Beacons", List.of("Personal beacons are permanent.", "Future build: beacon selling and /sellable.")));
        inv.setItem(12, HomeMenu.item(Material.NETHER_STAR, "Ranks & Prestige", List.of("Drifter -> Survivor.", "Prestige gives stars.", "Prestige boosts personal beacon revenue later.")));
        inv.setItem(13, HomeMenu.item(Material.IRON_SWORD, "PvP", List.of("10s combat tag.", "Logging out while tagged kills you.")));
        inv.setItem(14, HomeMenu.item(Material.ENDER_PEARL, "Forgotten Relics", List.of("10 one-of-one relics.", "Long cooldowns.", "Use /relic info.")));
        inv.setItem(15, HomeMenu.item(Material.CHEST, "Homes", List.of("Max 3 homes.", "/sethome 1-3, /home 1-3, /homes.")));
        inv.setItem(16, HomeMenu.item(Material.PAPER, "Commands", List.of("Use /commands or /becommands.")));
        inv.setItem(31, HomeMenu.item(Material.NAME_TAG, "Support", List.of(plugin.getConfig().getString("contact-line"))));
        return inv;
    }

    public static Inventory commands(BeaconEconomyPlugin plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, COMMANDS_TITLE);
        inv.setItem(10, HomeMenu.item(Material.COMPASS, "Travel", List.of("/wild", "/spawn", "/tutorial")));
        inv.setItem(11, HomeMenu.item(Material.CHEST, "Homes", List.of("/sethome <1-3>", "/home <1-3>", "/homes", "/delhome <1-3>")));
        inv.setItem(12, HomeMenu.item(Material.EMERALD, "Money", List.of("/money", "/balance")));
        inv.setItem(13, HomeMenu.item(Material.NETHER_STAR, "Progression", List.of("/rank", "/ranks", "/rankup", "/prestige", "/prestiges")));
        inv.setItem(14, HomeMenu.item(Material.ENDER_EYE, "Relics", List.of("/relic info")));
        inv.setItem(15, HomeMenu.item(Material.BOOK, "Help", List.of("/info", "/behelp", "/tutorial", "/commands")));
        inv.setItem(16, HomeMenu.item(Material.REDSTONE_TORCH, "Staff", List.of("/beadmin", "/staffhelp")));
        return inv;
    }

    public static Inventory admin(BeaconEconomyPlugin plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, ADMIN_TITLE);
        inv.setItem(10, HomeMenu.item(Material.PLAYER_HEAD, "Players", List.of("Player control panel scaffold.")));
        inv.setItem(11, HomeMenu.item(Material.ENDER_EYE, "Relics", List.of("Give/test/reclaim relics.", "Use /beadmin giverelic <id> [player].")));
        inv.setItem(12, HomeMenu.item(Material.EMERALD, "Economy", List.of("Money and rank tools.")));
        inv.setItem(13, HomeMenu.item(Material.ICE, "Moderation", List.of("Freeze, vanish, spectate, invsee.")));
        inv.setItem(14, HomeMenu.item(Material.TNT, "ClearLag", List.of("Click to run /clearlag.")));
        inv.setItem(15, HomeMenu.item(Material.BOOK, "Help", List.of("Everything plugin-related will be reachable here.")));
        inv.setItem(49, HomeMenu.item(Material.BARRIER, "Close", List.of("Close menu.")));
        return inv;
    }
}
