package io.sniperjohnny.github.better_admin_commands.listeners;

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/** Keeps items of players who are in unlimited mode. */
public class Unlimited_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Unlimited_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!plugin.unlimited().isUnlimited(player)) {
            return;
        }
        ItemStack placed = event.getItemInHand().clone();
        plugin.getServer().getScheduler().runTask(plugin, () -> player.getInventory().setItemInMainHand(placed));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.unlimited().isUnlimited(player)) {
            return;
        }
        ItemStack consumed = event.getItem().clone();
        plugin.getServer().getScheduler().runTask(plugin, () -> player.getInventory().setItemInMainHand(consumed));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemFrame(PlayerItemFrameChangeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.unlimited().isUnlimited(player) || event.getItemStack().getType() == Material.AIR) {
            return;
        }
        ItemStack used = event.getItemStack().clone();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.getInventory().containsAtLeast(used, 1)) {
                player.getInventory().addItem(used);
            }
        });
    }
}
