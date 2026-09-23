package io.sniperjohnny.github.better_admin_commands.trade;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * One player's view of a trade.
 *
 * <p>Each side gets their own window. The slots marked as <em>own</em> are real
 * storage that the owner can move items into and out of; the slots marked as
 * <em>other</em> are a read-only copy of the partner's offer, refreshed whenever
 * either side changes something. Keeping the two areas in separate windows means
 * an item can only ever live in one place, so a trade cannot duplicate or lose
 * anything.</p>
 */
public class TradeMenu implements InventoryHolder {

    public static final int SIZE = 54;

    /** Slots the owner may put items into (two rows of four). */
    public static final int[] OWN_SLOTS = {0, 1, 2, 3, 9, 10, 11, 12};
    /** Slots that mirror the partner's offer; read-only. */
    public static final int[] OTHER_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17};

    public static final int OWN_MONEY = 20;
    public static final int INFO = 22;
    public static final int OTHER_MONEY = 24;
    public static final int OWN_STATUS = 27;
    public static final int OTHER_STATUS = 35;
    public static final int OWN_CONFIRM = 45;
    public static final int CLOSE = 49;
    public static final int OTHER_CONFIRM = 53;

    private final Inventory inventory;
    private final TradeSession session;
    private final UUID owner;

    public TradeMenu(TradeSession session, Player owner, String title) {
        this.session = session;
        this.owner = owner.getUniqueId();
        this.inventory = Bukkit.createInventory(this, SIZE, Msg.component(title));
    }

    public TradeSession session() {
        return session;
    }

    public UUID owner() {
        return owner;
    }

    public UUID partner() {
        return session.other(owner);
    }

    /** Whether a raw slot is part of the owner's own offer. */
    public static boolean isOwnSlot(int rawSlot) {
        return contains(OWN_SLOTS, rawSlot);
    }

    /** Whether a raw slot mirrors the partner's offer. */
    public static boolean isOtherSlot(int rawSlot) {
        return contains(OTHER_SLOTS, rawSlot);
    }

    /** Whether a raw slot holds a control of this menu rather than an item. */
    public static boolean isControl(int rawSlot) {
        return rawSlot == OWN_MONEY || rawSlot == INFO || rawSlot == OTHER_MONEY
                || rawSlot == OWN_STATUS || rawSlot == OTHER_STATUS
                || rawSlot == OWN_CONFIRM || rawSlot == CLOSE || rawSlot == OTHER_CONFIRM;
    }

    private static boolean contains(int[] slots, int rawSlot) {
        for (int slot : slots) {
            if (slot == rawSlot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
