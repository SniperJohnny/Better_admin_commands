package io.sniperjohnny.github.better_admin_commands.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * One trade between two players: the two windows, the money each side offers and
 * whether each side has confirmed. The {@link TradeService} owns the lifecycle.
 */
public class TradeSession {

    private final UUID first;
    private final String firstName;
    private final UUID second;
    private final String secondName;
    private final long startedAt;

    private TradeMenu firstMenu;
    private TradeMenu secondMenu;
    private double firstMoney;
    private double secondMoney;
    private boolean firstConfirmed;
    private boolean secondConfirmed;
    /** Set once the trade ended (completed or cancelled), so close events stop. */
    private boolean finished;

    public TradeSession(Player first, Player second) {
        this.first = first.getUniqueId();
        this.firstName = first.getName();
        this.second = second.getUniqueId();
        this.secondName = second.getName();
        this.startedAt = System.currentTimeMillis();
    }

    public UUID first() {
        return first;
    }

    public UUID second() {
        return second;
    }

    public String firstName() {
        return firstName;
    }

    public String secondName() {
        return secondName;
    }

    public long startedAt() {
        return startedAt;
    }

    /** The other player of the pair, or {@code null} when the id is not part of it. */
    public UUID other(UUID uuid) {
        if (first.equals(uuid)) {
            return second;
        }
        return second.equals(uuid) ? first : null;
    }

    public boolean involves(UUID uuid) {
        return first.equals(uuid) || second.equals(uuid);
    }

    public String nameOf(UUID uuid) {
        if (first.equals(uuid)) {
            return firstName;
        }
        return second.equals(uuid) ? secondName : null;
    }

    public TradeMenu menu(UUID uuid) {
        return first.equals(uuid) ? firstMenu : secondMenu;
    }

    public void setMenus(TradeMenu firstMenu, TradeMenu secondMenu) {
        this.firstMenu = firstMenu;
        this.secondMenu = secondMenu;
    }

    public double moneyOf(UUID uuid) {
        return first.equals(uuid) ? firstMoney : secondMoney;
    }

    public void setMoney(UUID uuid, double amount) {
        if (first.equals(uuid)) {
            firstMoney = Math.max(0.0, amount);
        } else {
            secondMoney = Math.max(0.0, amount);
        }
    }

    public boolean confirmed(UUID uuid) {
        return first.equals(uuid) ? firstConfirmed : secondConfirmed;
    }

    public void setConfirmed(UUID uuid, boolean value) {
        if (first.equals(uuid)) {
            firstConfirmed = value;
        } else {
            secondConfirmed = value;
        }
    }

    /** Clears both confirmations; called whenever an offer changes. */
    public void resetConfirmations() {
        firstConfirmed = false;
        secondConfirmed = false;
    }

    public boolean bothConfirmed() {
        return firstConfirmed && secondConfirmed;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        this.finished = true;
    }

    /** The items a player put into their own side of the trade. */
    public ItemStack[] itemsOf(UUID uuid) {
        TradeMenu menu = menu(uuid);
        if (menu == null) {
            return new ItemStack[0];
        }
        ItemStack[] items = new ItemStack[TradeMenu.OWN_SLOTS.length];
        for (int index = 0; index < TradeMenu.OWN_SLOTS.length; index++) {
            items[index] = menu.getInventory().getItem(TradeMenu.OWN_SLOTS[index]);
        }
        return items;
    }

    /** Whether either side offered anything at all. */
    public boolean isEmptyTrade() {
        return isBlank(itemsOf(first)) && isBlank(itemsOf(second))
                && firstMoney <= 0.0 && secondMoney <= 0.0;
    }

    private static boolean isBlank(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Empties both own areas, so the items can be handed over exactly once. */
    public void clearOffers() {
        clearOwn(first);
        clearOwn(second);
    }

    private void clearOwn(UUID uuid) {
        TradeMenu menu = menu(uuid);
        if (menu == null) {
            return;
        }
        for (int slot : TradeMenu.OWN_SLOTS) {
            menu.getInventory().setItem(slot, null);
        }
    }
}
