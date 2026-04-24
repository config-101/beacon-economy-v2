package com.danzi.beaconeconomy.gui;

import com.danzi.beaconeconomy.BeaconEconomyPlugin;
import com.danzi.beaconeconomy.util.Msg;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class MenuListener implements Listener {
    private final BeaconEconomyPlugin plugin;

    public MenuListener(BeaconEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = view.getTitle();
        if (title.equals(HomeMenu.TITLE)) {
            event.setCancelled(true);
            int raw = event.getRawSlot();
            int slot = switch (raw) {
                case 10 -> 1;
                case 13 -> 2;
                case 16 -> 3;
                default -> -1;
            };
            if (slot == -1) return;
            if (!plugin.getHomeManager().hasHome(player.getUniqueId(), slot)) {
                Msg.send(player, "That home slot is empty.", NamedTextColor.RED);
                return;
            }
            if (event.isRightClick()) {
                plugin.getHomeManager().deleteHome(player.getUniqueId(), slot);
                player.closeInventory();
                Msg.send(player, "Deleted home " + slot + ".", NamedTextColor.YELLOW);
            } else {
                player.closeInventory();
                plugin.getTeleportManager().queue(player, plugin.getHomeManager().getHome(player.getUniqueId(), slot), "home " + slot);
            }
            return;
        }
        if (title.equals(InfoMenus.HELP_TITLE) || title.equals(InfoMenus.WELCOME_TITLE) || title.equals(InfoMenus.BEACONS_TITLE) || title.equals(InfoMenus.RANKS_TITLE) || title.equals(InfoMenus.PVP_TITLE) || title.equals(InfoMenus.RELICS_TITLE) || title.equals(InfoMenus.BLACK_MARKET_TITLE) || title.equals(InfoMenus.PETS_TITLE) || title.equals(InfoMenus.SPAWNERS_TITLE) || title.equals(InfoMenus.FAQ_TITLE)) {
            event.setCancelled(true);
        }
    }
}
