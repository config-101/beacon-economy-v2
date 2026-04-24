package com.danzi.beaconeconomy.gui;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class InfoMenus {
    public static final String HELP_TITLE = "Beacon Economy Help";
    public static final String WELCOME_TITLE = "Welcome to Beacon Economy";
    public static final String BEACONS_TITLE = "Beacon Market Guide";
    public static final String RANKS_TITLE = "Ranks and Prestige";
    public static final String PVP_TITLE = "Death, PvP and Logging";
    public static final String RELICS_TITLE = "Forgotten Relics";
    public static final String BLACK_MARKET_TITLE = "Black Market";
    public static final String PETS_TITLE = "Pet Quarter";
    public static final String SPAWNERS_TITLE = "Spawner Vault";
    public static final String FAQ_TITLE = "Beacon Economy FAQ";

    private InfoMenus() {}

    public static Inventory help(BeaconEconomyPlugin plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, HELP_TITLE);
        inv.setItem(10, HomeMenu.simple(Material.KNOWLEDGE_BOOK, "Getting Started", List.of("Use /tutorial to visit the tutorial district.", "Use /wild to begin survival.", plugin.getConfig().getString("contact-line"))));
        inv.setItem(11, HomeMenu.simple(Material.BEACON, "Beacon Selling", List.of("Public spawn beacon: 40% payout.", "Personal beacons: 100% payout.", "Personal beacons are permanent once confirmed.")));
        inv.setItem(12, HomeMenu.simple(Material.NETHER_STAR, "Ranks and Prestige", List.of("Rank ladder: Drifter -> Survivor.", "Prestige gives stars and better personal beacon revenue.", "Use /rankup and /prestige later as they are added.")));
        inv.setItem(13, HomeMenu.simple(Material.IRON_SWORD, "Death and PvP", List.of("Death takes 30% money in the full design.", "PvP gives a 10-second combat tag.", "Logging out while tagged kills you.")));
        inv.setItem(14, HomeMenu.simple(Material.ENDER_EYE, "Forgotten Relics", List.of("Unique legendary items with ancient powers.", "Use /relic info while holding one.", "Their power can be felt nearby.")));
        inv.setItem(15, HomeMenu.simple(Material.RECOVERY_COMPASS, "Commands", List.of("/spawn /wild /tutorial /behelp /info", "/sethome /home /homes /delhome", plugin.getConfig().getString("contact-line"))));
        inv.setItem(16, HomeMenu.simple(Material.BOOK, "Quick Support", List.of("Contact Danzi on Discord if you have questions.", "Use /behelp or /tutorial for information.")));
        return inv;
    }

    public static Inventory section(BeaconEconomyPlugin plugin, String key) {
        return switch (key.toLowerCase()) {
            case "welcome" -> simpleSection(WELCOME_TITLE, Material.KNOWLEDGE_BOOK, List.of(
                "Welcome to Beacon Economy.",
                "This server and plugin were made by Danzi.",
                "Use /wild when you are ready to leave the capital city.",
                plugin.getConfig().getString("contact-line")
            ));
            case "beacons" -> simpleSection(BEACONS_TITLE, Material.BEACON, List.of(
                "There is only one public beacon at spawn.",
                "Public spawn beacon pays 40% revenue.",
                "Personal beacons pay 100% revenue.",
                "Personal beacons are permanent once placed and confirmed."
            ));
            case "ranks" -> simpleSection(RANKS_TITLE, Material.NETHER_STAR, List.of(
                "Rank path: Drifter, Vagrant, Scavenger, Outcast, Rogue, Survivor.",
                "Prestige adds up to 3 glowing stars.",
                "Each prestige star increases personal beacon revenue.",
                "Costs rise sharply with each prestige."
            ));
            case "pvp" -> simpleSection(PVP_TITLE, Material.IRON_SWORD, List.of(
                "Logging out while combat tagged kills you.",
                "Combat tag lasts 10 seconds in PvP.",
                "Most teleports are delayed and blocked while tagged.",
                "Use good positioning and do not panic log out."
            ));
            case "relics" -> simpleSection(RELICS_TITLE, Material.ENDER_EYE, List.of(
                "Forgotten Relics are unique server-wide artifacts.",
                "Only one of each relic can exist at a time.",
                "Use /relic info while holding one.",
                "The void may reclaim a relic if it is lost."
            ));
            case "blackmarket" -> simpleSection(BLACK_MARKET_TITLE, Material.ENDER_CHEST, List.of(
                "The Black Market sells risky, expensive and sometimes unfair offers.",
                "It exists as a late-game money sink.",
                "Some deals may be illegal, cursed or simply terrible."
            ));
            case "pets" -> simpleSection(PETS_TITLE, Material.WOLF_ARMOR, List.of(
                "Pets are prestige companions and utility assets.",
                "Only one active pet at a time in the full design.",
                "Passive pets are protected; attack pets can fight for you."
            ));
            case "spawners" -> simpleSection(SPAWNERS_TITLE, Material.SPAWNER, List.of(
                "Spawner purchases are meant to be extremely expensive.",
                "Each purchase scales upward in cost.",
                "This is a late-game money sink, not an early shortcut."
            ));
            default -> simpleSection(FAQ_TITLE, Material.BOOK, List.of(
                "Use /wild to begin survival.",
                "Use /spawn to return to the city.",
                "Use /behelp or /tutorial for information.",
                plugin.getConfig().getString("contact-line")
            ));
        };
    }

    private static Inventory simpleSection(String title, Material icon, List<String> lines) {
        Inventory inv = Bukkit.createInventory(null, 27, title);
        inv.setItem(13, HomeMenu.simple(icon, title, lines));
        return inv;
    }
}
