package com.danzi.beaconeconomy.gui;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.PlayerData;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import java.util.concurrent.ThreadLocalRandom;

public class MenuListener implements Listener {
    private final BeaconEconomyPlugin plugin;
    public MenuListener(BeaconEconomyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (title.equals(InfoMenus.INTRO_TITLE)) {
            e.setCancelled(true);
            if (e.getRawSlot() == 14) {
                PlayerData pd = plugin.data().get(p.getUniqueId());
                pd.introComplete = true;
                plugin.data().save();
                p.closeInventory();

                World w = plugin.survivalWorld();
                if (w == null) {
                    Msg.send(p, "Survival world not found.", NamedTextColor.RED);
                    return;
                }
                int min = plugin.getConfig().getInt("wild.min-radius", 1500);
                int max = plugin.getConfig().getInt("wild.max-radius", 5000);
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 40; i++) {
                    int x = r.nextInt(min, max) * (r.nextBoolean() ? 1 : -1);
                    int z = r.nextInt(min, max) * (r.nextBoolean() ? 1 : -1);
                    int y = w.getHighestBlockYAt(x, z) + 1;
                    Block below = w.getBlockAt(x, y - 1, z);
                    if (!below.isLiquid() && below.getType().isSolid()) {
                        p.teleport(new Location(w, x + 0.5, y, z + 0.5));
                        p.setFlying(false);
                        p.setAllowFlight(false);
                        Msg.send(p, "You have begun your journey.", NamedTextColor.GREEN);
                        return;
                    }
                }
                p.setFlying(false);
                p.setAllowFlight(false);
                Msg.send(p, "Could not find a safe wild location. Try /wild.", NamedTextColor.RED);
            }
            return;
        }
        if (title.equals(InfoMenus.HELP_TITLE) || title.equals(InfoMenus.COMMANDS_TITLE)) {
            e.setCancelled(true);
            return;
        }
        if (title.equals(InfoMenus.ADMIN_TITLE)) {
            e.setCancelled(true);
            if (e.getRawSlot() == 14) p.performCommand("clearlag");
            if (e.getRawSlot() == 49) p.closeInventory();
            return;
        }
        if (title.equals(HomeMenu.TITLE)) {
            e.setCancelled(true);
            int slot = switch (e.getRawSlot()) { case 10 -> 1; case 13 -> 2; case 16 -> 3; default -> -1; };
            if (slot < 1) return;
            PlayerData pd = plugin.data().get(p.getUniqueId());
            if (!pd.homes.containsKey(slot)) { Msg.send(p, "That home slot is empty.", NamedTextColor.RED); return; }
            if (e.isRightClick()) {
                pd.homes.remove(slot);
                plugin.data().save();
                p.closeInventory();
                Msg.send(p, "Deleted home " + slot + ".", NamedTextColor.YELLOW);
            } else {
                p.closeInventory();
                plugin.teleports().queue(p, pd.homes.get(slot), "home " + slot);
            }
        }
    }
}
