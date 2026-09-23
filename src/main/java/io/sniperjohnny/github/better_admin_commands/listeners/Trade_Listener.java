package io.sniperjohnny.github.better_admin_commands.listeners;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.trade.TradeMenu;
import io.sniperjohnny.github.better_admin_commands.trade.TradeService;
import io.sniperjohnny.github.better_admin_commands.trade.TradeSession;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Drives the trade windows.
 *
 * <p>Only the owner of a window can interact with it, only their own half takes
 * items, and the mirror of the partner's offer is untouchable. Every other click
 * is swallowed, so the window cannot be used to smuggle items out.</p>
 */
public class Trade_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Trade_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    private TradeService trades() {
        return plugin.trades();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeMenu menu)) {
            return;
        }
        // Nothing in a trade window may move by itself; allowed moves are
        // handled explicitly below.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        TradeSession session = menu.session();
        if (!player.getUniqueId().equals(menu.owner()) || session.isFinished()) {
            return;
        }

        // Collect-to-cursor would sweep items out of the read-only mirror of the
        // partner's offer, so it is never allowed in a trade window.
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            return;
        }

        int raw = event.getRawSlot();
        if (raw == TradeMenu.OWN_CONFIRM) {
            trades().toggleConfirm(player);
            return;
        }
        if (raw == TradeMenu.CLOSE) {
            trades().cancel(player, "you cancelled it");
            return;
        }
        if (raw == TradeMenu.OWN_MONEY) {
            promptMoney(player);
            return;
        }
        // The partner's offer and the informational buttons are read-only.
        if (TradeMenu.isOtherSlot(raw) || TradeMenu.isControl(raw)) {
            return;
        }

        if (TradeMenu.isOwnSlot(raw)) {
            if (event.isShiftClick()) {
                moveBackToInventory(player, menu.getInventory(), raw);
                trades().offerChanged(session);
            } else {
                event.setCancelled(false);
                scheduleRender(session);
            }
            return;
        }

        // A click in the player's real inventory below the trade window.
        if (raw >= menu.getInventory().getSize()) {
            if (event.isShiftClick()) {
                shiftIntoOwn(player, menu.getInventory(), event, session);
                return;
            }
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                return; // stays cancelled: it could pull from the trade half
            }
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TradeMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeMenu menu)) {
            return;
        }
        TradeSession session = menu.session();
        if (!session.isFinished()) {
            trades().cancel(session, "the window was closed");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        trades().onQuit(event.getPlayer());
    }

    /* ------------------------------------------------------------ helpers --- */

    private void scheduleRender(TradeSession session) {
        plugin.getServer().getScheduler().runTask(plugin, () -> trades().offerChanged(session));
    }

    /** Asks for an amount of money to offer, then sets it. */
    private void promptMoney(Player player) {
        if (plugin.economy() == null) {
            Msg.error(player, "The economy is not available.");
            return;
        }
        // The window stays open: closing it would cancel the trade.
        plugin.chatPrompts().request(player, "&7How much money do you want to offer? &8(0 to offer none)",
                answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
                        // Only the money entry is dropped; the trade stays open.
                        Msg.send(player, "&7Money offer left unchanged.");
                        return;
                    }
                    Double amount = Targets.parseDouble(answer);
                    if (amount == null || amount < 0) {
                        Msg.error(player, "That is not a valid amount.");
                        return;
                    }
                    double balance = plugin.economy().getBalance(player.getUniqueId());
                    if (amount > balance) {
                        Msg.error(player, "You only have " + plugin.economy().format(balance) + ".");
                        return;
                    }
                    plugin.trades().setMoney(player, amount);
                }, false);
    }

    /** Moves one own-slot stack back into the player's inventory. */
    private void moveBackToInventory(Player player, Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        inventory.setItem(slot, null);
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    /** Moves a stack the player shift-clicked into their own half of the trade. */
    private void shiftIntoOwn(Player player, Inventory inventory, InventoryClickEvent event,
                              TradeSession session) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        Inventory source = event.getClickedInventory() == null
                ? player.getInventory() : event.getClickedInventory();
        int sourceSlot = event.getSlot();
        for (int slot : TradeMenu.OWN_SLOTS) {
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                inventory.setItem(slot, clicked.clone());
                source.setItem(sourceSlot, null);
                trades().offerChanged(session);
                return;
            }
            if (existing.isSimilar(clicked) && existing.getAmount() < existing.getMaxStackSize()) {
                int room = existing.getMaxStackSize() - existing.getAmount();
                int moved = Math.min(room, clicked.getAmount());
                ItemStack merged = existing.clone();
                merged.setAmount(existing.getAmount() + moved);
                inventory.setItem(slot, merged);
                int left = clicked.getAmount() - moved;
                source.setItem(sourceSlot, left <= 0 ? null : clicked);
                if (left <= 0) {
                    trades().offerChanged(session);
                    return;
                }
            }
        }
        Msg.error(player, "Your trade half is full.");
    }
}
