package io.sniperjohnny.github.better_admin_commands.gui;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A chest menu: a fixed grid of buttons, each with an optional click handler.
 *
 * <p>Menus are cheap and rebuilt whenever a page changes, so a new instance is
 * created per open instead of mutating an existing one. All clicks and drags
 * inside a menu are cancelled by {@link Gui_Listener}, which keeps items from
 * being moved around.</p>
 */
public class Menu implements InventoryHolder {

    /** Highest row count a chest inventory can have. */
    private static final int MAX_ROWS = 6;

    private final Inventory inventory;
    private final Map<Integer, ItemStack> buttons = new LinkedHashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new LinkedHashMap<>();

    public Menu(String title, int rows) {
        this.inventory = Bukkit.createInventory(this, clampRows(rows) * 9, Msg.component(title));
    }

    private static int clampRows(int rows) {
        return Math.max(1, Math.min(MAX_ROWS, rows));
    }

    /** Adds or replaces a button in a slot. A {@code null} handler makes it decorative. */
    public Menu button(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        if (slot >= 0 && slot < inventory.getSize() && item != null) {
            buttons.put(slot, item);
            if (handler == null) {
                handlers.remove(slot);
            } else {
                handlers.put(slot, handler);
            }
        }
        return this;
    }

    /** Adds a decorative button that does nothing when clicked. */
    public Menu button(int slot, ItemStack item) {
        return button(slot, item, null);
    }

    /** Fills every slot that has no button yet with the given item. */
    public Menu fillEmpty(ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!buttons.containsKey(slot)) {
                buttons.put(slot, item);
            }
        }
        return this;
    }

    public int size() {
        return inventory.getSize();
    }

    /** Renders every button and shows the menu to the player. */
    public void open(Player player) {
        render();
        player.openInventory(inventory);
    }

    /** Redraws all buttons without reopening the window. */
    public void render() {
        inventory.clear();
        buttons.forEach(inventory::setItem);
    }

    /** Routes a click to the slot's handler. Always cancels the event. */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Consumer<InventoryClickEvent> handler = handlers.get(event.getRawSlot());
        if (handler != null) {
            handler.accept(event);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
