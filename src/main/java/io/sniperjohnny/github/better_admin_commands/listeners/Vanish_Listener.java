package io.sniperjohnny.github.better_admin_commands.listeners;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.EnderChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

public class Vanish_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Vanish_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (!mobsIgnore()) {
            return;
        }
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        if (!plugin.vanish().isVanished(player)) {
            return;
        }
        // Cancel and forget the target, so the mob does not stay on the player.
        event.setTarget(null);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!silentContainers()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.vanish().isVanished(player)) {
            return;
        }
        Inventory inventory = containerOf(block, player);
        if (inventory == null) {
            return;
        }
        // Cancelling the interaction stops the server from sending the block
        // action (lid animation + sound); opening the inventory ourselves then
        // shows the same contents without it.
        event.setCancelled(true);
        // One tick later, so the cancelled interaction is fully out of the way
        // before the window opens.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && inventory.getViewers().isEmpty()) {
                player.openInventory(inventory);
            }
        });
    }

    private static Inventory containerOf(Block block, Player player) {
        BlockState state = block.getState();
        if (state instanceof EnderChest) {
            return player.getEnderChest();
        }
        if (state instanceof Container container) {
            return container.getInventory();
        }
        return null;
    }

    private boolean mobsIgnore() {
        return plugin.getConfig().getBoolean("moderation.vanish-mobs-ignore", true);
    }

    private boolean silentContainers() {
        return plugin.getConfig().getBoolean("moderation.vanish-silent-containers", true);
    }
}
