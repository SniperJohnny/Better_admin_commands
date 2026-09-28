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

public class Menu implements InventoryHolder {

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

    public Menu button(int slot, ItemStack item) {
        return button(slot, item, null);
    }

    public Menu fillEmpty(ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!buttons.containsKey(slot)) {
                buttons.put(slot, item);
            }
        }
        return this;
    }

    public Menu border(ItemStack item) {
        if (item == null) {
            return this;
        }
        int size = inventory.getSize();
        int rows = size / 9;
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == rows - 1 || column == 0 || column == 8) {
                buttons.putIfAbsent(slot, item);
            }
        }
        return this;
    }

    public Menu frame() {
        return frame(Theme.border(), Theme.filler());
    }

    public Menu frame(ItemStack borderItem, ItemStack fillerItem) {
        border(borderItem);
        fillEmpty(fillerItem);
        return this;
    }

    public int size() {
        return inventory.getSize();
    }

    public void open(Player player) {
        render();
        player.openInventory(inventory);
    }

    public void render() {
        inventory.clear();
        buttons.forEach(inventory::setItem);
    }

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
