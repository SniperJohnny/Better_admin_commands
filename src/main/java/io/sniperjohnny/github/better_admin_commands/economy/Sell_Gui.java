package io.sniperjohnny.github.better_admin_commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Sell_Gui implements Listener {

    public static final class SellHolder implements InventoryHolder {

        private Inventory inventory;

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final Better_Admin_Commands plugin;

    public Sell_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("sell.gui-rows", 6)));
        SellHolder holder = new SellHolder();
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, Component.text(Msg.color("&8Sell items")));
        holder.setInventory(inventory);
        player.openInventory(inventory);
        Msg.send(player, "&7Drop in everything you want to sell, then close the window."
                + " Items the server does not buy are given back.");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof SellHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            inventory.clear();
            return;
        }

        ItemStack[] contents = inventory.getContents();
        inventory.clear();

        double total = 0.0;
        int sold = 0;
        List<ItemStack> returned = new ArrayList<>();

        for (ItemStack stack : contents) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            double price = plugin.worth().price(stack.getType());
            if (price <= 0.0) {
                returned.add(stack);
                continue;
            }
            total += price * stack.getAmount();
            sold += stack.getAmount();
        }

        if (total > 0.0) {
            plugin.economy().deposit(player.getUniqueId(), total);
            plugin.economy().saveAsync();
        }

        for (ItemStack stack : returned) {
            give(player, stack);
        }

        if (sold == 0) {
            if (returned.isEmpty()) {
                return; // an empty window says nothing
            }
            Msg.error(player, "None of that can be sold, so you got it back.");
            return;
        }

        Msg.success(player, "Sold " + sold + " item(s) for " + plugin.worth().format(total)
                + ". New balance: "
                + plugin.worth().format(plugin.economy().getBalance(player.getUniqueId())) + ".");
        if (!returned.isEmpty()) {
            Msg.send(player, "&7" + returned.size() + " stack(s) could not be sold and were given back.");
        }
    }

    private static void give(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }
}
