package com.danzi.beaconeconomy.gui;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.data.PlayerData;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;

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
                p.setFlying(false);
                p.setAllowFlight(false);
                plugin.getServer().dispatchCommand(p, "wild");
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
