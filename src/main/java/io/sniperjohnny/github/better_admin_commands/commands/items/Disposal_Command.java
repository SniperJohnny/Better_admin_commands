package io.sniperjohnny.github.better_admin_commands.commands.items;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Opens a trash can whose contents disappear when it is closed. */
public class Disposal_Command implements TabExecutor {

    /** Marks the trash inventory so it can be cleared on close. */
    public static final class DisposalHolder implements InventoryHolder {

        private Inventory inventory;

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    /** Empties the trash when the window is closed. */
    public static final class Disposal_Listener implements Listener {

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            Inventory inventory = event.getInventory();
            if (inventory.getHolder() instanceof DisposalHolder) {
                inventory.clear();
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        DisposalHolder holder = new DisposalHolder();
        Inventory inventory = Bukkit.createInventory(holder, 36, Component.text(Msg.color("&8Disposal")));
        holder.setInventory(inventory);
        player.openInventory(inventory);
        Msg.send(player, "&7Anything left in here disappears when you close it.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
