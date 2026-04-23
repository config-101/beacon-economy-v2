package com.danzi.beaconeconomy.command;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.HomeManager;
import com.danzi.beaconeconomy.gui.HomeMenu;
import com.danzi.beaconeconomy.listener.CombatListener;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomeCommand implements CommandExecutor {
    private final BeaconEconomyPlugin plugin;

    public HomeCommand(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        String name = command.getName().toLowerCase();
        switch (name) {
            case "homes" -> player.openInventory(HomeMenu.create(plugin, player));
            case "sethome" -> handleSetHome(player, args);
            case "home" -> handleHome(player, args);
            case "delhome" -> handleDelHome(player, args);
        }
        return true;
    }

    private void handleSetHome(Player player, String[] args) {
        HomeManager manager = plugin.getHomeManager();
        if (player.getWorld().equals(plugin.getSpawnWorld())) {
            Msg.send(player, "You cannot set a home in the spawn world.", NamedTextColor.RED);
            return;
        }
        if (player.getWorld().getEnvironment() == World.Environment.NETHER && player.getLocation().getY() >= player.getWorld().getMaxHeight() - 2) {
            Msg.send(player, "You cannot set a home on the Nether roof.", NamedTextColor.RED);
            return;
        }
        int slot;
        if (args.length > 0) {
            try {
                slot = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                Msg.send(player, "Use /sethome <1-3>.", NamedTextColor.RED);
                return;
            }
        } else {
            slot = manager.nextEmptySlot(player.getUniqueId());
            if (slot == -1) {
                Msg.send(player, "You already have 3 homes. Delete one first with /delhome <1-3>.", NamedTextColor.RED);
                return;
            }
        }
        if (slot < 1 || slot > 3) {
            Msg.send(player, "Home slot must be between 1 and 3.", NamedTextColor.RED);
            return;
        }
        if (manager.hasHome(player.getUniqueId(), slot)) {
            Msg.send(player, "That home slot already exists. Delete it first with /delhome " + slot + ".", NamedTextColor.RED);
            return;
        }
        manager.setHome(player, slot, player.getLocation());
        Msg.send(player, "Set home " + slot + ".", NamedTextColor.GREEN);
    }

    private void handleHome(Player player, String[] args) {
        if (CombatListener.isTagged(player.getUniqueId())) {
            Msg.send(player, "You cannot use /home while combat tagged.", NamedTextColor.RED);
            return;
        }
        if (args.length == 0) {
            player.openInventory(HomeMenu.create(plugin, player));
            return;
        }
        int slot;
        try {
            slot = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            Msg.send(player, "Use /home <1-3>.", NamedTextColor.RED);
            return;
        }
        Location home = plugin.getHomeManager().getHome(player.getUniqueId(), slot);
        if (home == null) {
            Msg.send(player, "That home does not exist.", NamedTextColor.RED);
            return;
        }
        plugin.getTeleportManager().queue(player, home, "home " + slot);
    }

    private void handleDelHome(Player player, String[] args) {
        if (args.length == 0) {
            player.openInventory(HomeMenu.create(plugin, player));
            Msg.send(player, "Right click a home in the menu to delete it.", NamedTextColor.YELLOW);
            return;
        }
        int slot;
        try {
            slot = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            Msg.send(player, "Use /delhome <1-3>.", NamedTextColor.RED);
            return;
        }
        if (!plugin.getHomeManager().hasHome(player.getUniqueId(), slot)) {
            Msg.send(player, "That home slot is already empty.", NamedTextColor.RED);
            return;
        }
        plugin.getHomeManager().deleteHome(player.getUniqueId(), slot);
        Msg.send(player, "Deleted home " + slot + ".", NamedTextColor.YELLOW);
    }
}
