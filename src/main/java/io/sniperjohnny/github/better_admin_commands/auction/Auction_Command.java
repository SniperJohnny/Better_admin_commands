package io.sniperjohnny.github.better_admin_commands.auction;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.gui.Theme;
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
 * The auction house.
 *
 * <p>{@code /ah} opens the whole thing as a menu: browse, sell, my listings and
 * claims. Everything is reachable through buttons, and the same actions are also
 * available as text subcommands for players who prefer typing:</p>
 *
 * <pre>
 *   /ah                       open the menu
 *   /ah browse [page]         the listings, optionally a page
 *   /ah search &lt;text&gt;         filter by item or seller
 *   /ah sort &lt;order&gt;          newest, oldest, cheapest, expensive, name
 *   /ah sell [price]          list the held item (asks for the price when none)
 *   /ah mine                  your own listings
 *   /ah claims                collect items back
 *   /ah help                  this list
 * </pre>
 */
public class Auction_Command implements TabExecutor {

    /** Slots the browse grid uses: four inner rows, seven columns. */
    private static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    /** Centre of the grid, used for the "nothing here" message. */
    private static final int EMPTY = 22;

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

    private void setState(Player player, String query, AuctionService.Sort sort) {
        browseStates.put(player.getUniqueId(), new BrowseState(query, sort));
    }

    /* ------------------------------------------------------------ command --- */

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
        if (args.length == 0) {
            openMain(player);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "browse", "b", "list" -> openBrowse(player, pageFrom(args, 1));
            case "search", "find" -> {
                if (args.length < 2) {
                    promptSearch(player);
                    return true;
                }
                StringBuilder query = new StringBuilder();
                for (int index = 1; index < args.length; index++) {
                    if (query.length() > 0) {
                        query.append(' ');
                    }
                    query.append(args[index]);
                }
                setState(player, query.toString(), stateOf(player).sort());
                openBrowse(player, 0);
            }
            case "sort", "order" -> {
                AuctionService.Sort sort = parseSort(args.length >= 2 ? args[1] : null);
                if (sort == null) {
                    Msg.error(player, "Unknown sort order. Use newest, oldest, cheapest, "
                            + "expensive or name.");
                    return true;
                }
                setState(player, stateOf(player).query(), sort);
                openBrowse(player, 0);
            }
            case "sell", "list-item" -> {
                if (args.length >= 2) {
                    Double price = Targets.parseDouble(args[1]);
                    if (price == null || price <= 0.0) {
                        Msg.error(player, "That is not a valid price.");
                        return true;
                    }
                    listHeld(player, price);
                } else {
                    startSell(player);
                }
            }
            case "mine", "my", "listings" -> openMine(player, 0);
            case "claims", "claim" -> openClaims(player);
            case "help", "?", "commands" -> help(player, label);
            default -> {
                // A bare number doubles as a page, anything else is a search term.
                Integer page = Targets.parseInt(args[0]);
                if (page != null) {
                    openBrowse(player, Math.max(0, page - 1));
                    return true;
                }
                setState(player, String.join(" ", args), stateOf(player).sort());
                openBrowse(player, 0);
            }
        }
        return true;
    }

    private int pageFrom(String[] args, int index) {
        if (args.length <= index) {
            return 0;
        }
        Integer page = Targets.parseInt(args[index]);
        return page == null ? 0 : Math.max(0, page - 1);
    }

    private AuctionService.Sort parseSort(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "newest", "new", "recent" -> AuctionService.Sort.NEWEST;
            case "oldest", "old" -> AuctionService.Sort.OLDEST;
            case "cheapest", "cheap", "low" -> AuctionService.Sort.CHEAPEST;
            case "expensive", "priciest", "high" -> AuctionService.Sort.MOST_EXPENSIVE;
            case "name", "az", "a-z" -> AuctionService.Sort.NAME;
            default -> null;
        };
    }

    private void help(CommandSender sender, String label) {
        Msg.raw(sender, "&6Auction House");
        Msg.raw(sender, " &f/" + label + " &7- open the menu");
        Msg.raw(sender, " &f/" + label + " browse [page] &7- the listings");
        Msg.raw(sender, " &f/" + label + " search <item|seller> &7- filter the listings");
        Msg.raw(sender, " &f/" + label + " sort <newest|oldest|cheapest|expensive|name>");
        Msg.raw(sender, " &f/" + label + " sell [price] &7- list the item in your hand");
        Msg.raw(sender, " &f/" + label + " mine &7- manage your own listings");
        Msg.raw(sender, " &f/" + label + " claims &7- collect items back");
    }

    /* -------------------------------------------------------- main menu --- */

    private void openMain(Player player) {
        int rows = plugin.auctions().guiRows();
        Menu menu = new Menu(Theme.heading("Auction House"), rows);
        menu.frame();

        UUID uuid = player.getUniqueId();
        int active = plugin.auctions().active().size();
        int mine = plugin.auctions().mine(uuid).size();
        int claims = plugin.auctions().claimCount(uuid);
        double balance = plugin.economy().getBalance(uuid);

        menu.button(13, Theme.info(Material.SUNFLOWER, "&6Your balance",
                "&7You have &f" + plugin.economy().format(balance) + "&7.",
                "",
                "&7Use &f/money &7to manage it."));

        menu.button(20, Items.of(Material.CHEST, "&6Browse listings",
                        "&7There are &f" + active + " &7listing(s) right now.",
                        "",
                        "&eClick to open",
                        "&7or type &f/ah browse"),
                event -> openBrowse(player, 0));

        menu.button(22, Items.of(Material.GOLD_INGOT, "&aSell held item",
                        "&7Hold an item, then set a price.",
                        "",
                        "&eClick to list it",
                        "&7or type &f/ah sell <price>"),
                event -> startSell(player));

        menu.button(24, Items.of(Material.PLAYER_HEAD, "&bMy listings &7(" + mine + ")",
                        "&7Manage and cancel what you listed.",
                        "",
                        "&eClick to open",
                        "&7or type &f/ah mine"),
                event -> openMine(player, 0));

        menu.button(29, claims > 0
                        ? Items.of(Material.HOPPER, "&eClaims &7(" + claims + ")",
                                "&7Items from cancelled or expired listings.",
                                "",
                                "&eClick to collect")
                        : Items.of(Material.HOPPER, "&7Claims",
                                "&7Nothing is waiting for you."),
                event -> {
                    if (plugin.auctions().claimCount(uuid) > 0) {
                        openClaims(player);
                    } else {
                        Msg.send(player, "&7You have nothing to collect.");
                    }
                });

        menu.button(31, Items.of(Material.BOOK, "&bHow it works",
                "&7Listings run for &f" + plugin.auctions().listingHours() + "h&7,",
                "&7the server keeps &f" + plugin.auctions().taxPercent() + "% &7of a sale.",
                "&7Price range: &f" + plugin.economy().format(plugin.auctions().minPrice())
                        + " &7to &f" + plugin.economy().format(plugin.auctions().maxPrice()),
                "&7You may keep &f" + plugin.auctions().maxListings() + " &7listings."));

        menu.button(33, Items.of(Material.COMPASS, "&bSearch",
                        "&7Filter the listings by item or seller.",
                        "",
                        "&eClick to type a search",
                        "&7or type &f/ah search <text>"),
                event -> promptSearch(player));

        menu.button(40, Items.of(Material.KNOWLEDGE_BOOK, "&eCommands",
                "&7Every button has a text form:",
                "&f/ah browse &7, &f/ah sell &7, &f/ah mine &7, &f/ah claims",
                "",
                "&eClick to see them all"),
                event -> help(player, "ah"));

        menu.button(49, Theme.close(), event -> player.closeInventory());
        menu.open(player);
    }

    /* ----------------------------------------------------------- browse --- */

    private void openBrowse(Player player, int page) {
        BrowseState state = stateOf(player);
        List<AuctionService.Listing> listings = plugin.auctions().browse(state.query(), state.sort());
        int rows = plugin.auctions().guiRows();
        int pageSize = GRID.length;
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        String title = state.query().isEmpty()
                ? Theme.heading("Auction House") + " &8\u00bb &fBrowse"
                : Theme.heading("Auction House") + " &8\u00bb &f\"" + state.query() + "\"";
        Menu menu = new Menu(title, rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < GRID.length && start + index < listings.size(); index++) {
            AuctionService.Listing listing = listings.get(start + index);
            menu.button(GRID[index], describe(listing, "&eClick to buy"),
                    event -> openConfirm(player, listing.id()));
        }
        if (listings.isEmpty()) {
            menu.button(EMPTY, Theme.info(Material.GRAY_DYE,
                    state.query().isEmpty() ? "&7No listings yet" : "&7Nothing found",
                    state.query().isEmpty()
                            ? "&7Be the first to sell something with &f/ah sell&7."
                            : "&7No listing matches &f" + state.query() + "&7.",
                    "&7Try &f/ah&7, or clear the search."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openBrowse(player, current - 1));
        }
        menu.button(nav + 1, Items.of(Material.HOPPER, "&bSort: &f" + state.sort().label(),
                        "&7Listings are shown in this order.",
                        "",
                        "&eClick to change",
                        "&7or type &f/ah sort <order>"),
                event -> {
                    setState(player, state.query(), state.sort().next());
                    openBrowse(player, 0);
                });
        menu.button(nav + 2, Items.of(Material.GOLD_INGOT, "&aSell held item",
                        "&7List the item in your hand."),
                event -> startSell(player));
        menu.button(nav + 3, Items.of(Material.COMPASS, "&bSearch",
                        state.query().isEmpty()
                                ? "&7Filter the listings by item or seller."
                                : "&7Filtering by: &f" + state.query(),
                        "",
                        "&eClick to search"),
                event -> promptSearch(player));
        menu.button(nav + 4, Theme.back(), event -> openMain(player));
        if (!state.query().isEmpty()) {
            menu.button(nav + 5, Items.of(Material.FIRE_CHARGE, "&cClear search",
                            "&7Showing everything again.",
                            "",
                            "&eClick to clear"),
                    event -> {
                        setState(player, "", state.sort());
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
                    setState(player, query, state.sort());
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
        Menu menu = new Menu(Theme.heading("Confirm purchase"), 3);
        menu.frame();
        menu.button(13, describe(listing, ""));
        menu.button(11, Items.of(Material.LIME_CONCRETE, "&aBuy for &f" + plugin.economy().format(listing.price()),
                        "&7Seller: &f" + listing.sellerName(),
                        "",
                        "&eClick to confirm"),
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
        int pageSize = GRID.length;
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu(Theme.heading("Auction House") + " &8\u00bb &fMy listings", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < GRID.length && start + index < listings.size(); index++) {
            AuctionService.Listing listing = listings.get(start + index);
            menu.button(GRID[index], describe(listing, "&eClick to cancel"),
                    event -> openCancelConfirm(player, listing.id()));
        }
        if (listings.isEmpty()) {
            menu.button(EMPTY, Theme.info(Material.GRAY_DYE, "&7No listings",
                    "&7You have nothing up for sale.",
                    "&7Hold an item and press &fSell&7."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openMine(player, current - 1));
        }
        menu.button(nav + 4, Theme.back(), event -> openMain(player));
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
        Menu menu = new Menu(Theme.heading("Cancel listing"), 3);
        menu.frame();
        menu.button(13, describe(listing, ""));
        menu.button(11, Items.of(Material.RED_CONCRETE, "&cCancel listing",
                        "&7The item goes to your &fClaims&7.",
                        "",
                        "&eClick to confirm"),
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

        Menu menu = new Menu(Theme.heading("Auction House") + " &8\u00bb &fClaims", rows);
        menu.frame();
        for (int index = 0; index < GRID.length && index < claims.size(); index++) {
            AuctionService.Listing listing = claims.get(index);
            menu.button(GRID[index], describe(listing, "&eClick to collect"), event -> {
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
        if (claims.isEmpty()) {
            menu.button(EMPTY, Theme.info(Material.HOPPER, "&7Nothing to collect",
                    "&7Items from cancelled or expired",
                    "&7listings show up here."));
        }
        int nav = (rows - 1) * 9;
        menu.button(nav + 4, Theme.back(), event -> openMain(player));
        menu.open(player);
    }

    /* -------------------------------------------------------------- sell --- */

    private void startSell(Player player) {
        if (!plugin.permissions().has(player, "betteradmincommands.auction.sell")) {
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
                    listHeld(player, price);
                });
    }

    /** Lists whatever the player holds at the given price, after re-checking it. */
    private void listHeld(Player player, double price) {
        if (!plugin.permissions().has(player, "betteradmincommands.auction.sell")) {
            Msg.noPermission(player);
            return;
        }
        ItemStack current = player.getInventory().getItemInMainHand();
        if (current.getType().isAir()) {
            Msg.error(player, "Hold the item you want to sell first.");
            openMain(player);
            return;
        }
        ItemStack selling = current.clone();
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
    }

    /* ----------------------------------------------------------- helpers --- */

    /** The listing's own item with sale details appended to the lore. */
    private ItemStack describe(AuctionService.Listing listing, String footer) {
        ItemStack display = listing.item().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(Msg.component("&8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
            lore.add(Msg.component("&7Seller: &f" + listing.sellerName()));
            lore.add(Msg.component("&7Price: &6" + plugin.economy().format(listing.price())));
            lore.add(Msg.component("&7Expires in: &f" + expiresText(listing)));
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
        if (args.length == 1) {
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT),
                    "browse", "search", "sort", "sell", "mine", "claims", "help");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sort")) {
            return Targets.completeFrom(args[1].toLowerCase(Locale.ROOT),
                    "newest", "oldest", "cheapest", "expensive", "name");
        }
        return Collections.emptyList();
    }
}
