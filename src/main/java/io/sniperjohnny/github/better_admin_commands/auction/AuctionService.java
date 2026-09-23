package io.sniperjohnny.github.better_admin_commands.auction;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The auction house store.
 *
 * <p>Listings are kept in memory the whole time the server runs (like balances),
 * mirrored into {@code data/auction.yml} on every change and pushed to MySQL in
 * the background, so neither buying nor selling ever blocks the server thread on
 * the database.</p>
 *
 * <p>Each listing keeps its own serialized {@link ItemStack}; when it expires or
 * is cancelled the item waits in the seller's claims instead of being dropped.</p>
 */
public class AuctionService {

    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String CANCELLED = "CANCELLED";

    /** One auction listing. {@code expiresAt == 0} means it never expires. */
    public record Listing(String id, UUID sellerUuid, String sellerName, ItemStack item, double price,
                          long createdAt, long expiresAt, String state, boolean claimed, String buyerName) {

        public boolean isActive() {
            return ACTIVE.equals(state);
        }
    }

    public enum ListResult { SUCCESS, NO_ITEM, TOO_CHEAP, TOO_EXPENSIVE, TOO_MANY, CANNOT_AFFORD_FEE }

    public enum BuyResult { SUCCESS, NOT_FOUND, OWN_LISTING, TOO_POOR, NO_SPACE }

    /** The orders the browse menu offers. */
    public enum Sort {
        NEWEST("Newest first"),
        OLDEST("Oldest first"),
        CHEAPEST("Cheapest first"),
        MOST_EXPENSIVE("Most expensive first"),
        NAME("Item name A-Z");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** The next order, so one button can cycle through all of them. */
        public Sort next() {
            Sort[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public Comparator<Listing> comparator() {
            return switch (this) {
                case NEWEST -> Comparator.comparingLong(Listing::createdAt).reversed();
                case OLDEST -> Comparator.comparingLong(Listing::createdAt);
                case CHEAPEST -> Comparator.comparingDouble(Listing::price);
                case MOST_EXPENSIVE -> Comparator.comparingDouble(Listing::price).reversed();
                case NAME -> Comparator.comparing(listing -> itemName(listing.item()),
                        String.CASE_INSENSITIVE_ORDER);
            };
        }
    }

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final LocalStore local;
    private final Map<String, Listing> listings = new ConcurrentHashMap<>();

    public AuctionService(Better_Admin_Commands plugin, Database database, LocalStore local) {
        this.plugin = plugin;
        this.database = database;
        this.local = local;
    }

    /* ------------------------------------------------------------ config --- */

    public boolean enabled() {
        return plugin.getConfig().getBoolean("auction.enabled", true);
    }

    public int guiRows() {
        return Math.max(3, Math.min(6, plugin.getConfig().getInt("auction.gui-rows", 6)));
    }

    /** How long an active listing stays up, in hours; {@code 0} means never. */
    public long listingHours() {
        return Math.max(0L, plugin.getConfig().getLong("auction.listing-hours", 48L));
    }

    /** How many active listings one player may have at once. */
    public int maxListings() {
        return Math.max(1, plugin.getConfig().getInt("auction.max-listings-per-player", 10));
    }

    /** Share of a sale the server keeps, as a percentage. */
    public double taxPercent() {
        return percent("auction.tax-percent", 0.0);
    }

    /** Fee charged when listing, as a percentage of the price. */
    public double listingFeePercent() {
        return percent("auction.listing-fee-percent", 0.0);
    }

    private double percent(String path, double fallback) {
        double value = plugin.getConfig().getDouble(path, fallback);
        return Math.max(0.0, Math.min(100.0, value));
    }

    public double minPrice() {
        return Math.max(0.0, plugin.getConfig().getDouble("auction.min-price", 1.0));
    }

    public double maxPrice() {
        return Math.max(minPrice(), plugin.getConfig().getDouble("auction.max-price", 1_000_000_000.0));
    }

    /* -------------------------------------------------------------- load --- */

    /** Reads every listing into memory, preferring MySQL and falling back locally. */
    public void loadAll() {
        listings.clear();
        if (database.isAvailable()) {
            try {
                Map<String, Map<String, Object>> localRows = new LinkedHashMap<>();
                database.withConnection(connection -> {
                    try (Statement statement = connection.createStatement();
                         ResultSet result = statement.executeQuery(
                                 "SELECT * FROM `" + database.table("ah_listings") + "`")) {
                        while (result.next()) {
                            Listing listing = fromResult(result);
                            if (listing != null) {
                                listings.put(listing.id(), listing);
                                localRows.put(listing.id(), rowOf(listing));
                            }
                        }
                    }
                    return null;
                });
                local.mergeAll(localRows);
                return;
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not read the auction listings from MySQL, using the local safe file: "
                        + e.getMessage());
                database.markUnavailable();
            }
        }
        loadFromLocal();
    }

    private void loadFromLocal() {
        for (Map<String, Object> row : local.rawSnapshot()) {
            String id = LocalStore.string(row, LocalStore.KEY);
            Listing listing = listingFromRow(id, row);
            if (listing != null) {
                listings.put(listing.id(), listing);
            }
        }
    }

    private Listing fromResult(ResultSet result) throws SQLException {
        String id = result.getString("id");
        String owner = result.getString("seller_uuid");
        if (id == null || owner == null) {
            return null;
        }
        ItemStack item = decode(result.getString("item"));
        if (item == null) {
            return null;
        }
        try {
            return new Listing(id, UUID.fromString(owner), result.getString("seller_name"), item,
                    result.getDouble("price"), result.getLong("created_at"), result.getLong("expires_at"),
                    result.getString("state"), result.getInt("claimed") != 0, result.getString("buyer_name"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Listing listingFromRow(String id, Map<String, Object> row) {
        String owner = LocalStore.string(row, "seller_uuid");
        if (id == null || owner == null) {
            return null;
        }
        ItemStack item = decode(LocalStore.string(row, "item"));
        if (item == null) {
            return null;
        }
        try {
            return new Listing(id, UUID.fromString(owner),
                    LocalStore.string(row, "seller_name"), item,
                    LocalStore.doubleValue(row, "price", 0.0),
                    LocalStore.longValue(row, "created_at", 0L),
                    LocalStore.longValue(row, "expires_at", 0L),
                    LocalStore.string(row, "state") == null ? ACTIVE : LocalStore.string(row, "state"),
                    Boolean.parseBoolean(String.valueOf(row.get("claimed"))),
                    LocalStore.string(row, "buyer_name"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<String, Object> rowOf(Listing listing) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("seller_uuid", listing.sellerUuid().toString());
        row.put("seller_name", listing.sellerName());
        row.put("item", encode(listing.item()));
        row.put("price", listing.price());
        row.put("created_at", listing.createdAt());
        row.put("expires_at", listing.expiresAt());
        row.put("state", listing.state());
        row.put("claimed", listing.claimed());
        row.put("buyer_name", listing.buyerName());
        return row;
    }

    /* ------------------------------------------------------------ reads --- */

    public List<Listing> active() {
        return listings.values().stream()
                .filter(Listing::isActive)
                .sorted(Comparator.comparingLong(Listing::createdAt).reversed())
                .toList();
    }

    /**
     * The active listings a buyer sees: optionally filtered by a search term and
     * always in the chosen order.
     *
     * <p>The search term is matched against the item's type name, its custom
     * display name and the seller's name, all case-insensitively, so "diamond"
     * finds diamond listings and "sniper" finds listings by SniperJohnny.</p>
     */
    public List<Listing> browse(String query, Sort sort) {
        String needle = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        Sort order = sort == null ? Sort.NEWEST : sort;
        return listings.values().stream()
                .filter(Listing::isActive)
                .filter(listing -> needle.isEmpty() || matches(listing, needle))
                .sorted(order.comparator())
                .toList();
    }

    private static boolean matches(Listing listing, String needle) {
        if (itemName(listing.item()).toLowerCase(java.util.Locale.ROOT).contains(needle)) {
            return true;
        }
        return listing.sellerName() != null
                && listing.sellerName().toLowerCase(java.util.Locale.ROOT).contains(needle);
    }

    /** The name a listing is searched and sorted by: its custom name, else its material. */
    private static String itemName(ItemStack item) {
        if (item == null) {
            return "";
        }
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName());
        }
        return item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    public List<Listing> mine(UUID uuid) {
        return listings.values().stream()
                .filter(listing -> listing.isActive() && listing.sellerUuid().equals(uuid))
                .sorted(Comparator.comparingLong(Listing::createdAt).reversed())
                .toList();
    }

    /** Items waiting for a seller, from cancelled or expired listings. */
    public List<Listing> claims(UUID uuid) {
        return listings.values().stream()
                .filter(listing -> !listing.claimed() && listing.sellerUuid().equals(uuid)
                        && (EXPIRED.equals(listing.state()) || CANCELLED.equals(listing.state())))
                .sorted(Comparator.comparingLong(Listing::createdAt).reversed())
                .toList();
    }

    public int claimCount(UUID uuid) {
        return claims(uuid).size();
    }

    public Listing find(String id) {
        return id == null ? null : listings.get(id);
    }

    /* ----------------------------------------------------------- writes --- */

    /** Creates a listing. The item is copied, so the caller still owns their stack. */
    public ListResult list(Player seller, ItemStack item, double price) {
        if (!enabled()) {
            return ListResult.NO_ITEM;
        }
        if (item == null || item.getType().isAir()) {
            return ListResult.NO_ITEM;
        }
        if (price < minPrice()) {
            return ListResult.TOO_CHEAP;
        }
        if (price > maxPrice()) {
            return ListResult.TOO_EXPENSIVE;
        }
        if (mine(seller.getUniqueId()).size() >= maxListings()) {
            return ListResult.TOO_MANY;
        }
        double fee = price * percent("auction.listing-fee-percent", 0.0) / 100.0;
        if (fee > 0.0) {
            if (plugin.economy().getBalance(seller.getUniqueId()) < fee) {
                return ListResult.CANNOT_AFFORD_FEE;
            }
            plugin.economy().withdraw(seller.getUniqueId(), fee);
        }

        long now = System.currentTimeMillis();
        long hours = listingHours();
        String id = UUID.randomUUID().toString();
        Listing listing = new Listing(id, seller.getUniqueId(), seller.getName(), item.clone(), price,
                now, hours <= 0L ? 0L : now + hours * 3_600_000L, ACTIVE, false, null);
        listings.put(id, listing);
        local.merge(id, rowOf(listing));
        persist(listing);
        return ListResult.SUCCESS;
    }

    /** Buys a listing and moves the money and the item. Runs on the server thread. */
    public BuyResult buy(Player buyer, String id) {
        Listing listing = listings.get(id);
        if (listing == null || !listing.isActive()) {
            return BuyResult.NOT_FOUND;
        }
        if (listing.sellerUuid().equals(buyer.getUniqueId())) {
            return BuyResult.OWN_LISTING;
        }
        if (plugin.economy().getBalance(buyer.getUniqueId()) < listing.price()) {
            return BuyResult.TOO_POOR;
        }
        if (!hasRoom(buyer.getInventory(), listing.item())) {
            return BuyResult.NO_SPACE;
        }

        plugin.economy().withdraw(buyer.getUniqueId(), listing.price());
        double tax = listing.price() * percent("auction.tax-percent", 0.0) / 100.0;
        plugin.economy().deposit(listing.sellerUuid(), listing.price() - tax);

        give(buyer, listing.item().clone());
        listings.remove(id);
        local.delete(id);
        deleteRow(id);

        Player seller = Bukkit.getPlayer(listing.sellerUuid());
        if (seller != null) {
            Msg.send(seller, "&7Your listing of &f" + listing.item().getAmount() + "x "
                    + listing.item().getType().name().toLowerCase(java.util.Locale.ROOT)
                    + " &7sold to &f" + buyer.getName() + " &7for &f"
                    + plugin.economy().format(listing.price()) + "&7.");
        }
        return BuyResult.SUCCESS;
    }

    /** Cancels an own listing; the item waits in the seller's claims. */
    public boolean cancel(Player player, String id) {
        Listing listing = listings.get(id);
        if (listing == null || !listing.isActive() || !listing.sellerUuid().equals(player.getUniqueId())) {
            return false;
        }
        update(listing, CANCELLED, false, listing.buyerName());
        return true;
    }

    /** Hands a waiting item back. Returns the item, or {@code null} when there is nothing to claim. */
    public ItemStack claim(Player player, String id) {
        Listing listing = listings.get(id);
        if (listing == null || listing.claimed() || !listing.sellerUuid().equals(player.getUniqueId())) {
            return null;
        }
        ItemStack item = listing.item().clone();
        listings.remove(id);
        local.delete(id);
        deleteRow(id);
        return item;
    }

    /** Marks overdue listings as expired and tells sellers who are online. */
    public void expire() {
        long now = System.currentTimeMillis();
        List<Listing> expired = new ArrayList<>();
        for (Listing listing : listings.values()) {
            if (listing.isActive() && listing.expiresAt() > 0L && listing.expiresAt() <= now) {
                update(listing, EXPIRED, false, listing.buyerName());
                expired.add(listing);
            }
        }
        for (Listing listing : expired) {
            Player seller = Bukkit.getPlayer(listing.sellerUuid());
            if (seller != null) {
                Msg.send(seller, "&7Your auction listing expired - claim the item back in &f/ah&7.");
            }
        }
    }

    private void update(Listing listing, String state, boolean claimed, String buyerName) {
        Listing updated = new Listing(listing.id(), listing.sellerUuid(), listing.sellerName(), listing.item(),
                listing.price(), listing.createdAt(), listing.expiresAt(), state, claimed, buyerName);
        listings.put(updated.id(), updated);
        local.merge(updated.id(), rowOf(updated));
        persist(updated);
    }

    /* --------------------------------------------------------- persistence - */

    private void persist(Listing listing) {
        String sql = "INSERT INTO `" + database.table("ah_listings")
                + "` (`id`, `seller_uuid`, `seller_name`, `item`, `price`, `created_at`, `expires_at`,"
                + " `state`, `claimed`, `buyer_name`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE `state` = VALUES(`state`), `claimed` = VALUES(`claimed`),"
                + " `buyer_name` = VALUES(`buyer_name`)";
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!database.isAvailable()) {
                return;
            }
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, listing.id());
                        statement.setString(2, listing.sellerUuid().toString());
                        statement.setString(3, listing.sellerName());
                        statement.setString(4, encode(listing.item()));
                        statement.setDouble(5, listing.price());
                        statement.setLong(6, listing.createdAt());
                        statement.setLong(7, listing.expiresAt());
                        statement.setString(8, listing.state());
                        statement.setInt(9, listing.claimed() ? 1 : 0);
                        statement.setString(10, listing.buyerName());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not store an auction listing: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    private void deleteRow(String id) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!database.isAvailable()) {
                return;
            }
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM `" + database.table("ah_listings") + "` WHERE `id` = ?")) {
                        statement.setString(1, id);
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not delete an auction listing: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /* ------------------------------------------------------------ helpers -- */

    /** Whether the item still fits into the inventory without being dropped. */
    public static boolean hasRoom(PlayerInventory inventory, ItemStack item) {
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                return true;
            }
            if (slot.isSimilar(item) && slot.getAmount() + item.getAmount() <= slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** Adds an item, dropping whatever does not fit at the player's feet. */
    public static void give(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }

    private static String encode(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private static ItemStack decode(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
