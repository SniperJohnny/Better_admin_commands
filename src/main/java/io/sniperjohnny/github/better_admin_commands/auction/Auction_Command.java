package io.sniperjohnny.github.better_admin_commands.auction;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The auction house. One command, {@code /ah}, and everything else is a button:
 * browsing and buying, selling the held item, managing your own listings and
 * claiming items back from cancelled or expired listings.
 *
 * <p>Prices are collected as a chat line, because a chest window alone cannot
 * ask for a number.</p>
 */
public class Auction_Command implements TabExecutor {

    /** What one player is currently looking at: their search term and sort order. */
    private record BrowseState(String query, AuctionService.Sort sort) {
    }

    private final Better_Admin_Commands plugin;
    private final Map<UUID, BrowseState> browseStates = new ConcurrentHashMap<>();

    public Auction_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    private BrowseState stateOf(Player player) {
        return browseStates.getOrDefault(player.getUniqueId(),
                new BrowseState("", AuctionService.Sort.NEWEST));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (!plugin.auctions().enabled()) {
            Msg.error(player, "The auction house is disabled.");
            return true;
        }
        openMain(player);
        return true;
    }

    /* -------------------------------------------------------- main menu --- */

    private void openMain(Player player) {
        int rows = plugin.auctions().guiRows();
        int size = rows * 9;
        Menu menu = new Menu("&8Auction House", rows);
        menu.fillEmpty(Items.filler());

        menu.button(size / 2 - 2, Items.of(Material.CHEST, "&6Browse listings",
                        "&7There are &f" + plugin.auctions().active().size() + " &7listing(s).",
                        "", "&eClick to open"),
                event -> openBrowse(player, 0));
        menu.button(size / 2, Items.of(Material.GOLD_INGOT, "&aSell held item",
                        "&7Hold an item and list it for sale.",
                        "", "&eClick to list it"),
                event -> startSell(player));
        menu.button(size / 2 + 2, Items.of(Material.PLAYER_HEAD, "&bMy listings",
                        "&7Manage and cancel what you listed.",
                        "", "&eClick to open"),
                event -> openMine(player, 0));

        int claims = plugin.auctions().claimCount(player.getUniqueId());
        if (claims > 0) {
            menu.button(size - 5, Items.of(Material.HOPPER, "&eClaims &7(" + claims + ")",
                            "&7Items from cancelled or expired listings.",
                            "", "&eClick to collect"),
                    event -> openClaims(player));
        }

        menu.button(size / 2 - 4, Items.of(Material.SUNFLOWER, "&6Your balance",
                "&7You have &f" + plugin.economy().format(plugin.economy().getBalance(player.getUniqueId())) + "&7."));

        menu.button(size - 1, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        menu.open(player);
    }

    /* ----------------------------------------------------------- browse --- */

    private void openBrowse(Player player, int page) {
        BrowseState state = stateOf(player);
        List<AuctionService.Listing> listings = plugin.auctions().browse(state.query(), state.sort());
        int rows = plugin.auctions().guiRows();
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        String title = state.query().isEmpty()
                ? "&8Auction House &8» &fBrowse"
                : "&8Auction House &8» &f\"" + state.query() + "\"";
        Menu menu = new Menu(title, rows);
        menu.fillEmpty(Items.filler());

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < listings.size(); index++) {
            AuctionService.Listing listing = listings.get(start + index);
            menu.button(index, describe(listing, "&eClick to buy"), event -> openConfirm(player, listing.id()));
        }
        if (listings.isEmpty()) {
            menu.button((rows - 1) / 2 * 9 + 4, Items.of(Material.GRAY_DYE,
                    state.query().isEmpty() ? "&7No listings yet" : "&7Nothing found",
                    state.query().isEmpty()
                            ? "&7Be the first to sell something."
                            : "&7No listing matches &f" + state.query() + "&7.",
                    "&7Try &f/ah&7, or clear the search."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openBrowse(player, current - 1));
        }
        menu.button(nav + 1, Items.of(Material.HOPPER, "&bSort: &f" + state.sort().label(),
                        "&7Listings are shown in this order.",
                        "", "&eClick to change"),
                event -> {
                    browseStates.put(player.getUniqueId(),
                            new BrowseState(state.query(), state.sort().next()));
                    openBrowse(player, 0);
                });
        menu.button(nav + 2, Items.of(Material.GOLD_INGOT, "&aSell held item"),
                event -> startSell(player));
        menu.button(nav + 3, Items.of(Material.COMPASS, "&bSearch",
                        state.query().isEmpty()
                                ? "&7Filter the listings by item or seller."
                                : "&7Filtering by: &f" + state.query(),
                        "", "&eClick to search"),
                event -> promptSearch(player));
        menu.button(nav + 4, Items.of(Material.BARRIER, "&cBack", "&7Return to the auction menu."),
                event -> openMain(player));
        if (!state.query().isEmpty()) {
            menu.button(nav + 5, Items.of(Material.FIRE_CHARGE, "&cClear search",
                            "&7Showing everything again.",
                            "", "&eClick to clear"),
                    event -> {
                        browseStates.put(player.getUniqueId(), new BrowseState("", state.sort()));
                        openBrowse(player, 0);
                    });
        }
        menu.button(nav + 6, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages,
                "&7Listings: &f" + listings.size()));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openBrowse(player, current + 1));
        }
        menu.open(player);
    }

    /** Asks for a search term and reopens the browse menu with it. */
    private void promptSearch(Player player) {
        plugin.chatPrompts().request(player,
                "&7Search the auction house for an item or a seller &8(type &fclear &8for everything)",
                answer -> {
                    BrowseState state = stateOf(player);
                    if (answer.equalsIgnoreCase("cancel")) {
                        Msg.send(player, "&7Search cancelled.");
                        openBrowse(player, 0);
                        return;
                    }
                    String query = answer.equalsIgnoreCase("clear") ? "" : answer;
                    browseStates.put(player.getUniqueId(), new BrowseState(query, state.sort()));
                    openBrowse(player, 0);
                });
    }

    private void openConfirm(Player player, String id) {
        AuctionService.Listing listing = plugin.auctions().find(id);
        if (listing == null || !listing.isActive()) {
            Msg.error(player, "That listing is no longer available.");
            openBrowse(player, 0);
            return;
        }
        Menu menu = new Menu("&8Confirm purchase", 3);
        menu.fillEmpty(Items.filler());
        menu.button(13, describe(listing, ""));
        menu.button(11, Items.of(Material.LIME_CONCRETE, "&aBuy for &f" + plugin.economy().format(listing.price()),
                        "&7Seller: &f" + listing.sellerName(),
                        "", "&eClick to confirm"),
                event -> {
                    AuctionService.BuyResult result = plugin.auctions().buy(player, id);
                    switch (result) {
                        case SUCCESS -> {
                            Msg.success(player, "You bought " + listing.item().getAmount() + "x "
                                    + pretty(listing.item()) + " &7for &a"
                                    + plugin.economy().format(listing.price()) + "&7.");
                            player.getWorld().playSound(player.getLocation(),
                                    org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
                        }
                        case NOT_FOUND -> Msg.error(player, "That listing is no longer available.");
                        case OWN_LISTING -> Msg.error(player, "You cannot buy your own listing.");
                        case TOO_POOR -> Msg.error(player, "You cannot afford that.");
                        case NO_SPACE -> Msg.error(player, "Make room in your inventory first.");
                    }
                    openBrowse(player, 0);
                });
        menu.button(15, Items.of(Material.RED_CONCRETE, "&cCancel"), event -> openBrowse(player, 0));
        menu.open(player);
    }

    /* ------------------------------------------------------ my listings --- */

    private void openMine(Player player, int page) {
        List<AuctionService.Listing> listings = plugin.auctions().mine(player.getUniqueId());
        int rows = plugin.auctions().guiRows();
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Auction House &8» &fMy listings", rows);
        menu.fillEmpty(Items.filler());

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < listings.size(); index++) {
            AuctionService.Listing listing = listings.get(start + index);
            menu.button(index, describe(listing, "&eClick to cancel"), event -> openCancelConfirm(player, listing.id()));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openMine(player, current - 1));
        }
        menu.button(nav + 4, Items.of(Material.BARRIER, "&cBack", "&7Return to the auction menu."),
                event -> openMain(player));
        menu.button(nav + 6, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openMine(player, current + 1));
        }
        menu.open(player);
    }

    private void openCancelConfirm(Player player, String id) {
        AuctionService.Listing listing = plugin.auctions().find(id);
        if (listing == null || !listing.isActive()) {
            Msg.error(player, "That listing is gone.");
            openMine(player, 0);
            return;
        }
        Menu menu = new Menu("&8Cancel listing", 3);
        menu.fillEmpty(Items.filler());
        menu.button(13, describe(listing, ""));
        menu.button(11, Items.of(Material.RED_CONCRETE, "&cCancel listing",
                        "&7The item goes to your &fClaims&7.",
                        "", "&eClick to confirm"),
                event -> {
                    if (plugin.auctions().cancel(player, id)) {
                        Msg.success(player, "Listing cancelled - collect the item from Claims.");
                    } else {
                        Msg.error(player, "That listing is gone.");
                    }
                    openMine(player, 0);
                });
        menu.button(15, Items.of(Material.LIME_CONCRETE, "&aKeep it listed"), event -> openMine(player, 0));
        menu.open(player);
    }

    /* ------------------------------------------------------------ claims --- */

    private void openClaims(Player player) {
        List<AuctionService.Listing> claims = plugin.auctions().claims(player.getUniqueId());
        int rows = plugin.auctions().guiRows();
        int pageSize = (rows - 1) * 9;

        Menu menu = new Menu("&8Auction House &8» &fClaims", rows);
        menu.fillEmpty(Items.filler());
        for (int index = 0; index < pageSize && index < claims.size(); index++) {
            AuctionService.Listing listing = claims.get(index);
            menu.button(index, describe(listing, "&eClick to collect"), event -> {
                ItemStack item = plugin.auctions().claim(player, listing.id());
                if (item == null) {
                    Msg.error(player, "That item was already collected.");
                } else {
                    AuctionService.give(player, item);
                    Msg.success(player, "Collected " + item.getAmount() + "x " + pretty(item) + "&a.");
                }
                openClaims(player);
            });
        }
        int nav = (rows - 1) * 9;
        menu.button(nav + 4, Items.of(Material.BARRIER, "&cBack", "&7Return to the auction menu."),
                event -> openMain(player));
        menu.open(player);
    }

    /* -------------------------------------------------------------- sell --- */

    private void startSell(Player player) {
        if (!player.hasPermission("betteradmincommands.auction.sell")) {
            Msg.noPermission(player);
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            Msg.error(player, "Hold the item you want to sell first.");
            openMain(player);
            return;
        }
        ItemStack selling = held.clone();
        double min = plugin.auctions().minPrice();
        plugin.chatPrompts().request(player,
                "&7Enter the price for &f" + selling.getAmount() + "x " + pretty(selling)
                        + " &7in chat &8(min " + plugin.economy().format(min) + "&8).",
                answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
                        Msg.send(player, "&7Listing cancelled.");
                        openMain(player);
                        return;
                    }
                    Double price = Targets.parseDouble(answer);
                    if (price == null || price <= 0.0) {
                        Msg.error(player, "That is not a valid price.");
                        openMain(player);
                        return;
                    }
                    ItemStack current = player.getInventory().getItemInMainHand();
                    if (current.getType().isAir() || !current.isSimilar(selling)
                            || current.getAmount() != selling.getAmount()) {
                        Msg.error(player, "The item in your hand changed - nothing was listed.");
                        openMain(player);
                        return;
                    }
                    AuctionService.ListResult result = plugin.auctions().list(player, current.clone(), price);
                    switch (result) {
                        case SUCCESS -> {
                            player.getInventory().setItemInMainHand(null);
                            Msg.success(player, "You listed " + selling.getAmount() + "x " + pretty(selling)
                                    + " &afor " + plugin.economy().format(price) + "&a.");
                        }
                        case NO_ITEM -> Msg.error(player, "Hold the item you want to sell first.");
                        case TOO_CHEAP -> Msg.error(player, "The price is below the minimum of "
                                + plugin.economy().format(plugin.auctions().minPrice()) + ".");
                        case TOO_EXPENSIVE -> Msg.error(player, "The price is above the maximum of "
                                + plugin.economy().format(plugin.auctions().maxPrice()) + ".");
                        case TOO_MANY -> Msg.error(player, "You already have the maximum number of active listings.");
                        case CANNOT_AFFORD_FEE -> Msg.error(player, "You cannot afford the listing fee.");
                    }
                    openMain(player);
                });
    }

    /* ----------------------------------------------------------- helpers --- */

    /** The listing's own item with sale details appended to the lore. */
    private ItemStack describe(AuctionService.Listing listing, String footer) {
        ItemStack display = listing.item().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(Msg.component("&7Seller: &f" + listing.sellerName()));
            lore.add(Msg.component("&7Price: &6" + plugin.economy().format(listing.price())));
            lore.add(Msg.component("&7Expires: &f" + expiresText(listing)));
            if (!footer.isBlank()) {
                lore.add(Component.empty());
                lore.add(Msg.component(footer));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private String expiresText(AuctionService.Listing listing) {
        if (listing.expiresAt() <= 0L) {
            return "never";
        }
        long left = listing.expiresAt() - System.currentTimeMillis();
        if (left <= 0L) {
            return "expired";
        }
        return Targets.formatDuration(left / 1000L);
    }

    /** A friendly name for an item, preferring its custom name. */
    private static String pretty(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        String[] parts = item.getType().name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return name.toString();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
