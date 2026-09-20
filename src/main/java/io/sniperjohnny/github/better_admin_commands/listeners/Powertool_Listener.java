package io.sniperjohnny.github.better_admin_commands.listeners;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Runs commands bound to an item through /powertool. */
public class Powertool_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Powertool_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK
                && event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Material material = event.getItem() == null ? Material.AIR : event.getItem().getType();
        if (material.isAir()) {
            return;
        }
        if (plugin.powerTools().commandFor(event.getPlayer(), material) == null) {
            return;
        }
        event.setCancelled(true);
        plugin.powerTools().run(event.getPlayer(), material);
    }
}
