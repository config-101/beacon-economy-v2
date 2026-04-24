package com.danzi.beaconeconomy.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Msg {
    private Msg() {}

    public static Component prefix() {
        return Component.text("✦ Beacon Economy ✦ ", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    public static Component staffPrefix() {
        return Component.text("✦ Beacon Economy Staff ✦ ", NamedTextColor.RED, TextDecoration.BOLD);
    }

    public static Component line(String text, NamedTextColor color) {
        return prefix().append(Component.text(text, color));
    }

    public static void send(Player player, String text, NamedTextColor color) {
        player.sendMessage(line(text, color));
    }

    public static void broadcast(String text, NamedTextColor color) {
        Bukkit.broadcast(line(text, color));
    }

    public static void staff(String text) {
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.isOp() || p.hasPermission("beaconeconomy.admin"))
            .forEach(p -> p.sendMessage(staffPrefix().append(Component.text(text, NamedTextColor.YELLOW))));
    }
}
