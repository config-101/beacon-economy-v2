package com.danzi.beaconeconomy.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public final class Msg {
    private Msg() {}

    public static Component prefix() {
        return Component.text("✦ Beacon Economy ✦ ", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    public static Component line(String text, NamedTextColor color) {
        return prefix().append(Component.text(text, color));
    }

    public static void send(Player player, String text, NamedTextColor color) {
        player.sendMessage(line(text, color));
    }

    public static Component contactLine() {
        return Component.text("Contact Danzi on Discord if you have questions.", NamedTextColor.GRAY);
    }
}
