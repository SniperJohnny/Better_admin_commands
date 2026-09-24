package io.sniperjohnny.github.better_admin_commands.trade;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.auction.AuctionService;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player to player trading.
 *
 * <p>{@code /trade <player>} first asks the other side to accept (unless
 * {@code trade.require-accept} is off) and then opens two windows, one per
 * player. Each side drops items and money into their own half and confirms;
 * when both sides confirm, the offers are exchanged and the trade is written to
 * the history.</p>
 *
 * <p>Every failure path returns whatever was placed back to its owner, so a
 * closed window, a disconnect or too little money can never eat an item.</p>
 */
public class TradeService {

    /** A pending trade request, keyed by the player who has to answer it. */
    private record Request(UUID requester, long expiresAt) {
    }

    private final Better_Admin_Commands plugin;
    private final Map<UUID, Request> requests = new ConcurrentHashMap<>();
    private final Map<UUID, TradeSession> sessions = new ConcurrentHashMap<>();

    public TradeService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    /* ------------------------------------------------------------ config --- */

    public boolean enabled() {
        // Honours both the module switch and the older trade.enabled key.
        return plugin.features().enabled("trade");
    }

    private boolean requireAccept() {
        return plugin.getConfig().getBoolean("trade.require-accept", true);
    }

    private long requestExpiry() {
        return Math.max(5L, plugin.getConfig().getLong("trade.request-expire-seconds", 60L)) * 1000L;
    }

    public boolean isTrading(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    /** The open session of a player, or {@code null} when they are not trading. */
    public TradeSession sessionOf(Player player) {
        return player == null ? null : sessions.get(player.getUniqueId());
    }

    /* ---------------------------------------------------------- requests --- */

    /** Starts a trade with a player, asking them first when that is configured. */
    public void request(Player requester, Player target) {
        if (!enabled()) {
            Msg.error(requester, "Trading is disabled on this server.");
            return;
        }
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            Msg.error(requester, "You cannot trade with yourself.");
            return;
        }
        if (isTrading(requester.getUniqueId())) {
            Msg.error(requester, "You are already trading.");
            return;
        }
        if (isTrading(target.getUniqueId())) {
            Msg.error(requester, target.getName() + " is already trading.");
            return;
        }

        if (!requireAccept()) {
            open(requester, target);
            return;
        }

        requests.put(target.getUniqueId(), new Request(requester.getUniqueId(),
                System.currentTimeMillis() + requestExpiry()));
        Msg.send(requester, "&7Trade request sent to &f" + target.getName() + "&7.");
        net.kyori.adventure.text.Component line = Msg.component(
                Msg.color("&7") + target.getName() + " ")
                .append(Msg.component("&e" + requester.getName() + " &7wants to trade with you. "))
                .append(Msg.button("&a[Accept]", "/trade accept " + requester.getName(),
                        "&7Click to open the trade window"))
                .append(Msg.component(" "))
                .append(Msg.button("&c[Deny]", "/trade deny " + requester.getName(),
                        "&7Click to decline"));
        target.sendMessage(line);
    }

    /** Accepts a pending request, opening the trade window. */
    public void accept(Player target, String requesterName) {
        Request request = requests.get(target.getUniqueId());
        if (request == null) {
            Msg.error(target, "You have no pending trade request.");
            return;
        }
        UUID requesterUuid = request.requester();
        if (requesterName != null && Bukkit.getPlayer(requesterUuid) != null
                && !Bukkit.getPlayer(requesterUuid).getName().equalsIgnoreCase(requesterName)) {
            Msg.error(target, "That is not the player who asked you.");
            return;
        }
        if (System.currentTimeMillis() > request.expiresAt()) {
            requests.remove(target.getUniqueId());
            Msg.error(target, "That trade request has expired.");
            return;
        }
        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester == null) {
            requests.remove(target.getUniqueId());
            Msg.error(target, "That player is no longer online.");
            return;
        }
        requests.remove(target.getUniqueId());
        open(requester, target);
        Msg.send(requester, "&a" + target.getName() + " accepted your trade request.");
    }

    /** Declines a pending request. */
    public void deny(Player target, String requesterName) {
        Request request = requests.remove(target.getUniqueId());
        if (request == null) {
            Msg.error(target, "You have no pending trade request.");
            return;
        }
        Player requester = Bukkit.getPlayer(request.requester());
        Msg.send(target, "&7Trade request declined.");
        if (requester != null) {
            Msg.send(requester, "&c" + target.getName() + " declined your trade request.");
        }
    }

    /* ---------------------------------------------------------- sessions --- */

    private void open(Player first, Player second) {
        if (isTrading(first.getUniqueId()) || isTrading(second.getUniqueId())) {
            Msg.error(first, "One of you is already trading.");
            return;
        }
        TradeSession session = new TradeSession(first, second);
        TradeMenu firstMenu = new TradeMenu(session, first,
                "&8Trade &7\u00bb &f" + first.getName() + " &8\u2194 &f" + second.getName());
        TradeMenu secondMenu = new TradeMenu(session, second,
                "&8Trade &7\u00bb &f" + second.getName() + " &8\u2194 &f" + first.getName());
        session.setMenus(firstMenu, secondMenu);
        sessions.put(first.getUniqueId(), session);
        sessions.put(second.getUniqueId(), session);
        render(session);
        first.openInventory(firstMenu.getInventory());
        second.openInventory(secondMenu.getInventory());
        Msg.send(first, "&7Trading with &f" + second.getName() + "&7. Add items and confirm when ready.");
        Msg.send(second, "&7Trading with &f" + first.getName() + "&7. Add items and confirm when ready.");
    }

    /** Redraws both windows from the current state. */
    public void render(TradeSession session) {
        if (session.isFinished()) {
            return;
        }
        for (UUID owner : List.of(session.first(), session.second())) {
            renderSide(session, owner);
        }
    }

    /**
     * Reopens the window of one side after a dialog answered. Opening the dialog
     * closed the window without cancelling the trade, so this puts the player
     * back where they were. Does nothing when the trade ended meanwhile or when
     * the window is already open again.
     */
    public void reopen(TradeSession session, Player player) {
        if (session == null || session.isFinished()) {
            return;
        }
        session.setAwaitingInput(false);
        TradeMenu menu = session.menu(player.getUniqueId());
        if (menu == null) {
            return;
        }
        if (player.getUniqueId().equals(menu.owner())
                && player.getOpenInventory().getTopInventory().getHolder() instanceof TradeMenu) {
            return; // answered in chat while the window stayed open
        }
        render(session);
        if (player.isOnline()) {
            player.openInventory(menu.getInventory());
        }
    }

    private void renderSide(TradeSession session, UUID owner) {
        TradeMenu menu = session.menu(owner);
        if (menu == null) {
            return;
        }
        Inventory inventory = menu.getInventory();
        UUID partner = session.other(owner);

        // Background, without touching the owner's own storage slots.
        for (int slot = 0; slot < TradeMenu.SIZE; slot++) {
            if (!TradeMenu.isOwnSlot(slot)) {
                inventory.setItem(slot, Items.filler());
            }
        }

        // A read-only copy of what the partner offers.
        ItemStack[] partnerItems = session.itemsOf(partner);
        for (int index = 0; index < TradeMenu.OTHER_SLOTS.length; index++) {
            ItemStack item = index < partnerItems.length ? partnerItems[index] : null;
            inventory.setItem(TradeMenu.OTHER_SLOTS[index],
                    item == null || item.getType().isAir() ? Items.filler() : item.clone());
        }

        inventory.setItem(TradeMenu.OWN_MONEY, moneyItem(session.moneyOf(owner), true));
        inventory.setItem(TradeMenu.OTHER_MONEY, moneyItem(session.moneyOf(partner), false));
        inventory.setItem(TradeMenu.INFO, Items.of(Material.PAPER, "&6Trade",
                "&7Put items in your side on the &fleft&7,",
                "&7money with the &6gold&7 button.",
                "",
                "&7Both sides have to confirm."));
        inventory.setItem(TradeMenu.OWN_STATUS, statusItem("You", session.confirmed(owner), true));
        inventory.setItem(TradeMenu.OTHER_STATUS,
                statusItem(session.nameOf(partner), session.confirmed(partner), false));
        inventory.setItem(TradeMenu.OWN_CONFIRM, confirmItem(session.confirmed(owner), session.confirmed(partner)));
        inventory.setItem(TradeMenu.OTHER_CONFIRM, otherConfirmItem(session.confirmed(partner)));
        inventory.setItem(TradeMenu.CLOSE, Items.of(Material.BARRIER, "&cCancel trade",
                "&7Everything is returned to its owner.",
                "", "&eClick to cancel"));
        if (plugin.economy() == null) {
            inventory.setItem(TradeMenu.OWN_MONEY, Items.of(Material.BARRIER, "&cMoney unavailable"));
        }
    }

    private ItemStack moneyItem(double amount, boolean own) {
        if (own) {
            return Items.of(Material.GOLD_INGOT, "&6Your money: &f" + plugin.economy().format(amount),
                    "&7You offer this to the other side.",
                    "", "&eClick to change the amount");
        }
        return Items.of(Material.GOLD_NUGGET, "&6Their money: &f" + plugin.economy().format(amount),
                "&7The other side offers this.");
    }

    private ItemStack statusItem(String name, boolean confirmed, boolean own) {
        return confirmed
                ? Items.of(Material.LIME_DYE, "&a" + name + " &7ready",
                        "&7They confirmed the trade.")
                : Items.of(Material.GRAY_DYE, "&7" + name + " &7not ready yet",
                        own ? "&7Press confirm when your offer is right."
                                : "&7Waiting for them to confirm.");
    }

    private ItemStack confirmItem(boolean ownConfirmed, boolean otherConfirmed) {
        if (ownConfirmed) {
            return Items.of(Material.RED_CONCRETE, "&cTake back confirmation",
                    "&7You can still change your offer.",
                    "", "&eClick to un-confirm");
        }
        return Items.of(Material.LIME_CONCRETE, "&aConfirm trade",
                otherConfirmed ? "&7They are waiting for you." : "&7Both sides have to confirm.",
                "", "&eClick to confirm");
    }

    private ItemStack otherConfirmItem(boolean otherConfirmed) {
        return otherConfirmed
                ? Items.of(Material.LIME_CONCRETE, "&aThey confirmed", "&7Waiting for you.")
                : Items.of(Material.RED_CONCRETE, "&cThey have not confirmed yet");
    }

    /* ------------------------------------------------------------ actions --- */

    /** Called when a player changes their offer; both confirmations are cleared. */
    public void offerChanged(TradeSession session) {
        if (session.isFinished()) {
            return;
        }
        session.resetConfirmations();
        render(session);
    }

    /** Sets the money one side offers. */
    public void setMoney(Player player, double amount) {
        TradeSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isFinished()) {
            return;
        }
        session.setMoney(player.getUniqueId(), amount);
        offerChanged(session);
        Msg.send(player, "&7You now offer &f" + plugin.economy().format(amount) + "&7.");
    }

    /** Confirms or un-confirms and, when both sides are ready, completes the trade. */
    public void toggleConfirm(Player player) {
        TradeSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isFinished()) {
            return;
        }
        boolean now = !session.confirmed(player.getUniqueId());
        session.setConfirmed(player.getUniqueId(), now);
        if (!now) {
            render(session);
            Msg.send(player, "&7Confirmation taken back.");
            return;
        }
        UUID partner = session.other(player.getUniqueId());
        Player other = partner == null ? null : plugin.getServer().getPlayer(partner);
        if (other != null) {
            other.playSound(other.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.6f);
            if (session.confirmed(partner)) {
                execute(session);
                return;
            }
            Msg.send(other, "&7" + player.getName() + " is ready - confirm to finish the trade.");
        }
        render(session);
        if (partner != null && session.confirmed(partner)) {
            execute(session);
        }
    }

    /** Cancels the trade of a player, returning everything they placed. */
    public void cancel(Player player, String reason) {
        TradeSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            cancel(session, reason);
        }
    }

    /** Cancels a session that is no longer driven by a specific player. */
    public void cancel(TradeSession session, String reason) {
        if (session.isFinished()) {
            return;
        }
        session.finish();
        sessions.remove(session.first());
        sessions.remove(session.second());
        returnItems(session);
        for (UUID uuid : List.of(session.first(), session.second())) {
            Player online = plugin.getServer().getPlayer(uuid);
            if (online != null) {
                online.closeInventory();
                if (reason != null) {
                    Msg.send(online, "&7Trade cancelled: " + reason);
                }
            }
        }
    }

    private void returnItems(TradeSession session) {
        for (UUID uuid : List.of(session.first(), session.second())) {
            Player online = plugin.getServer().getPlayer(uuid);
            ItemStack[] items = session.itemsOf(uuid);
            if (online == null) {
                // The trade is cancelled while both players are still online (a
                // quit fires before the player is removed), so this is only a
                // safety net. Log it rather than silently losing the items.
                plugin.getLogger().warning("Could not return " + items.length
                        + " offered stack(s) to an offline player during a trade.");
                continue;
            }
            for (ItemStack item : items) {
                if (item != null && !item.getType().isAir()) {
                    AuctionService.give(online, item.clone());
                }
            }
        }
    }

    private void execute(TradeSession session) {
        if (session.isFinished()) {
            return;
        }
        UUID first = session.first();
        UUID second = session.second();
        Player firstPlayer = plugin.getServer().getPlayer(first);
        Player secondPlayer = plugin.getServer().getPlayer(second);
        if (firstPlayer == null || secondPlayer == null) {
            cancel(session, "a player left");
            return;
        }

        double firstMoney = session.moneyOf(first);
        double secondMoney = session.moneyOf(second);
        if (plugin.economy().getBalance(first) < firstMoney
                || plugin.economy().getBalance(second) < secondMoney) {
            cancel(session, "not enough money");
            return;
        }

        ItemStack[] firstItems = session.itemsOf(first);
        ItemStack[] secondItems = session.itemsOf(second);
        if (session.isEmptyTrade() && firstMoney <= 0.0 && secondMoney <= 0.0) {
            cancel(session, "nothing was offered");
            return;
        }

        session.finish();
        sessions.remove(first);
        sessions.remove(second);
        session.clearOffers();

        if (firstMoney > 0.0) {
            plugin.economy().withdraw(first, firstMoney);
            plugin.economy().deposit(second, firstMoney);
        }
        if (secondMoney > 0.0) {
            plugin.economy().withdraw(second, secondMoney);
            plugin.economy().deposit(first, secondMoney);
        }
        plugin.economy().saveAsync();

        for (ItemStack item : secondItems) {
            if (item != null && !item.getType().isAir()) {
                AuctionService.give(firstPlayer, item.clone());
            }
        }
        for (ItemStack item : firstItems) {
            if (item != null && !item.getType().isAir()) {
                AuctionService.give(secondPlayer, item.clone());
            }
        }

        firstPlayer.closeInventory();
        secondPlayer.closeInventory();

        plugin.tradeLogs().record(first, session.firstName(), second, session.secondName(),
                firstMoney, secondMoney, firstItems, secondItems);

        Msg.success(firstPlayer, "Trade finished with " + secondPlayer.getName() + ".");
        Msg.success(secondPlayer, "Trade finished with " + firstPlayer.getName() + ".");
        firstPlayer.playSound(firstPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        secondPlayer.playSound(secondPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
    }

    /** Handles a player leaving; their partner is told and the trade is undone. */
    public void onQuit(Player player) {
        requests.remove(player.getUniqueId());
        TradeSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            UUID partner = session.other(player.getUniqueId());
            cancel(session, player.getName() + " left the server");
            Player other = partner == null ? null : plugin.getServer().getPlayer(partner);
            if (other != null) {
                Msg.send(other, "&c" + player.getName() + " left - the trade was cancelled.");
            }
        }
    }

    /** Number of trades currently open, shown by {@code /bac info}. */
    public int openCount() {
        return sessions.size() / 2;
    }
}
