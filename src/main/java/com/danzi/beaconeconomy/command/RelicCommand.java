package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RelicCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;

    public RelicCommand(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0 || !args[0].equalsIgnoreCase("info")) {
            Msg.send(player, "Use /relic info while holding a Forgotten Relic.", NamedTextColor.YELLOW);
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!plugin.getRelicManager().isRiftDagger(hand)) {
            Msg.send(player, "You are not holding a Forgotten Relic.", NamedTextColor.RED);
            return true;
        }
        player.sendMessage(Msg.prefix().append(Component.text("Forgotten Relic: Rift Dagger", NamedTextColor.DARK_PURPLE)));
        player.sendMessage(Component.text("A relic of rupture and displacement. The blade refuses to serve as a common weapon.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Ability:", NamedTextColor.GOLD).append(Component.text(" Right-click to open a rift above you.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Rules:", NamedTextColor.GOLD).append(Component.text(" 250s cooldown, no blocks above you, 5s hover, target within 70 blocks.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Impact:", NamedTextColor.GOLD).append(Component.text(" The landing slash deals double Netherite Sword damage and disorients nearby entities.", NamedTextColor.WHITE)));
        player.sendMessage(Msg.contactLine());
        return true;
    }
}
