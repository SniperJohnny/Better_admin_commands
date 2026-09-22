package io.sniperjohnny.github.better_admin_commands.shop;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.auction.AuctionService;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server shop.
 *
 * <p>Shops are normally not written by hand - they are imported from
 * EconomyShopGUI by {@link EconomyShopGuiImporter}, which reads whatever YAML it
 * finds and pulls out every entry that names a material. After that the shop is
 * this plugin's own: it lives in MySQL and is mirrored into
 * {@code data/shops.yml} + {@code data/shop_items.yml}, exactly like the auction
 * house.</p>
 *
 * <p>Prices are per unit. A unit is the amount stored on the display item, so an
 * entry with {@code amount: 16} sells bundles of sixteen.</p>
 */
public class ShopService {

    /** A single purchasable entry. {@code buyPrice} and {@code sellPrice} are -1 when not offered. */
    public record ShopItem(String shop, String key, Material material, ItemStack display,
                           double buyPrice, double sellPrice, int slot, int page, String search) {

        /** How many items one purchase of this entry gives. */
        public int unit() {
            return Math.max(1, display.getAmount());
        }

        public boolean buyable() {
            return buyPrice >= 0.0;
        }

        public boolean sellable() {
            return sellPrice >= 0.0;
        }
    }

    /** A shop, i.e. one imported file. */
    public record Shop(String id, String display, ItemStack icon, int rows) {
    }

    public enum TradeResult { SUCCESS, NO_MONEY, NO_SPACE, NOT_BUYABLE, NOT_SELLABLE, NOTHING_TO_SELL, NO_ACCESS }

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final LocalStore localShops;
    private final LocalStore localItems;

    private final Map<String, Shop> shops = new ConcurrentHashMap<>();
    private final Map<String, List<ShopItem>> items = new ConcurrentHashMap<>();
    /** page -> slot -> item, rebuilt lazily whenever the contents change. */
    private final Map<String, Map<Integer, Map<Integer, ShopItem>>> layouts = new ConcurrentHashMap<>();

    public ShopService(Better_Admin_Commands plugin, Database database,
                       LocalStore localShops, LocalStore localItems) {
        this.plugin = plugin;
        this.database = database;
        this.localShops = localShops;
        this.localItems = localItems;
    }

    /* ------------------------------------------------------------ config --- */

    public boolean enabled() {
        return plugin.getConfig().getBoolean("shop.enabled", true);
    }

    public int guiRows() {
        return Math.max(3, Math.min(6, plugin.getConfig().getInt("shop.gui-rows", 6)));
    }

    public boolean sellingEnabled() {
        return plugin.getConfig().getBoolean("shop.selling-enabled", true);
    }

    public boolean searchEnabled() {
        return plugin.getConfig().getBoolean("shop.search-enabled", true);
    }

    /* ------------------------------------------------------------ access --- */

    /**
     * The permission a player needs for a shop, or {@code null} when the shop is
     * open to everyone.
     *
     * <p>{@code shop.access.permissions.<shop-id>} wins over everything else,
     * then {@code shop.access.free} (a shop id or {@code *}), and only when
     * neither applies does the shop fall back to
     * {@code shop.access.permission-prefix}. With the shipped defaults (an empty
     * prefix) every shop stays open, exactly as before.</p>
     */
    public String permissionFor(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            return null;
        }
        String id = shopId.toLowerCase(Locale.ROOT);

        ConfigurationSection overrides = plugin.getConfig()
                .getConfigurationSection("shop.access.permissions");
        if (overrides != null) {
            // A shop id may contain a dot ("mob.drops") and a hand-written key
            // may use one where the id has a slash ("blocks.mob_drops"), so both
            // sides are compared in the same normalised form. getValues(true) also
            // lets a dotted key match even though YAML parsed it as nested.
            String wanted = accessKey(id);
            for (Map.Entry<String, Object> entry : overrides.getValues(true).entrySet()) {
                if (entry.getValue() instanceof ConfigurationSection) {
                    continue;
                }
                if (!accessKey(entry.getKey()).equals(wanted)) {
                    continue;
                }
                String value = String.valueOf(entry.getValue());
                return value.isBlank() ? null : value.trim();
            }
        }

        for (String entry : plugin.getConfig().getStringList("shop.access.free")) {
            if (entry != null && (entry.equals("*") || entry.equalsIgnoreCase(id))) {
                return null;
            }
        }

        String prefix = plugin.getConfig().getString("shop.access.permission-prefix", "");
        if (prefix == null || prefix.isBlank()) {
            return null;
        }
        return prefix.trim() + nodeOf(id);
    }

    /** A shop id turned into something usable as a permission node. */
    public static String nodeOf(String shopId) {
        return shopId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", ".");
    }

    /**
     * The form shop ids and permission keys are compared in. Both dots and
     * slashes collapse to an underscore, so {@code blocks/mob_drops} and
     * {@code blocks.mob_drops} mean the same shop.
     */
    public static String accessKey(String value) {
        return value == null ? ""
                : value.toLowerCase(Locale.ROOT).replace('/', '_').replace('\\', '_').replace('.', '_');
    }

    /**
     * Sets (or clears) the permission that unlocks one shop, by writing it into
     * {@code shop.access.permissions}. Used by the in-game shop editor.
     *
     * @param permission the permission node, or {@code null}/blank to unlock the shop
     */
    public void setShopPermission(String shopId, String permission) {
        if (shopId == null || shopId.isBlank()) {
            return;
        }
        // The key is normalised, so it can never be read back as a nested section.
        String path = "shop.access.permissions." + accessKey(shopId);
        plugin.getConfig().set(path, permission == null || permission.isBlank() ? null : permission.trim());
        plugin.saveConfig();
    }

    /* ------------------------------------------------------------ editing -- */

    /** Replaces one entry and stores it, used by the in-game shop editor. */
    public void updateItem(ShopItem item) {
        if (item == null) {
            return;
        }
        List<ShopItem> stored = items.computeIfAbsent(item.shop(), ignored -> new ArrayList<>());
        stored.removeIf(existing -> existing.key().equals(item.key()));
        stored.add(item);
        layouts.remove(item.shop());
        localItems.merge(LocalStore.composite(item.shop(), item.key()), itemRow(item));
        upsertItem(item);
    }

    /** Removes one entry and deletes it, used by the in-game shop editor. */
    public boolean deleteItem(String shopId, String key) {
        if (shopId == null || key == null) {
            return false;
        }
        List<ShopItem> stored = items.get(shopId.toLowerCase(Locale.ROOT));
        if (stored == null || !stored.removeIf(existing -> existing.key().equals(key))) {
            return false;
        }
        layouts.remove(shopId.toLowerCase(Locale.ROOT));
        localItems.delete(LocalStore.composite(shopId.toLowerCase(Locale.ROOT), key));
        deleteItemRow(shopId.toLowerCase(Locale.ROOT), key);
        return true;
    }

    /** A new ShopItem with a different buy price; a negative price removes it from sale. */
    public static ShopItem withBuyPrice(ShopItem item, double price) {
        return new ShopItem(item.shop(), item.key(), item.material(), item.display(), price,
                item.sellPrice(), item.slot(), item.page(), item.search());
    }

    /** A new ShopItem with a different sell price; a negative price stops buying it back. */
    public static ShopItem withSellPrice(ShopItem item, double price) {
        return new ShopItem(item.shop(), item.key(), item.material(), item.display(), item.buyPrice(),
                price, item.slot(), item.page(), item.search());
    }

    /** A new ShopItem whose purchase hands out a different amount. */
    public static ShopItem withAmount(ShopItem item, int amount) {
        ItemStack display = item.display().clone();
        display.setAmount(Math.max(1, Math.min(display.getMaxStackSize(), amount)));
        return new ShopItem(item.shop(), item.key(), item.material(), display, item.buyPrice(),
                item.sellPrice(), item.slot(), item.page(), item.search());
    }

    private void upsertItem(ShopItem item) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!database.isAvailable()) {
                return;
            }
            String sql = "INSERT INTO `" + database.table("shop_items") + "`"
                    + " (`shop`, `item_key`, `material`, `search`, `item`, `buy_price`, `sell_price`,"
                    + " `slot`, `page`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE `material` = VALUES(`material`), `search` = VALUES(`search`),"
                    + " `item` = VALUES(`item`), `buy_price` = VALUES(`buy_price`),"
                    + " `sell_price` = VALUES(`sell_price`), `slot` = VALUES(`slot`), `page` = VALUES(`page`)";
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, item.shop());
                        statement.setString(2, item.key());
                        statement.setString(3, item.material().name());
                        statement.setString(4, item.search());
                        statement.setString(5, encode(item.display()));
                        statement.setDouble(6, item.buyPrice());
                        statement.setDouble(7, item.sellPrice());
                        statement.setInt(8, item.slot());
                        statement.setInt(9, item.page());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not store a shop entry: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    private void deleteItemRow(String shopId, String key) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!database.isAvailable()) {
                return;
            }
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM `" + database.table("shop_items") + "`"
                                    + " WHERE `shop` = ? AND `item_key` = ?")) {
                        statement.setString(1, shopId);
                        statement.setString(2, key);
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not delete a shop entry: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /**
     * Whether a player may use a shop. Anyone holding
     * {@code betteradmincommands.shop.admin} may use every shop, which is also
     * how staff preview a shop before granting it out.
     */
    public boolean canAccess(Player player, String shopId) {
        if (player.hasPermission("betteradmincommands.shop.admin")) {
            return true;
        }
        String permission = permissionFor(shopId);
        return permission == null || player.hasPermission(permission);
    }

    /** Hide the shops a player cannot use, instead of showing them greyed out. */
    public boolean hideLocked() {
        return plugin.getConfig().getBoolean("shop.access.hide-locked", false);
    }

    /** The shops a player may see: everything, or only what they can use. */
    public List<Shop> visibleShops(Player player) {
        List<Shop> all = shops();
        if (!hideLocked()) {
            return all;
        }
        List<Shop> visible = new ArrayList<>(all.size());
        for (Shop shop : all) {
            if (canAccess(player, shop.id())) {
                visible.add(shop);
            }
        }
        return visible;
    }

    public boolean importOnStartup() {
        return plugin.getConfig().getBoolean("shop.import.enabled", true);
    }

    public boolean importForced() {
        return plugin.getConfig().getBoolean("shop.import.force", false);
    }

    /* ------------------------------------------------------------- reads --- */

    public boolean isEmpty() {
        return shops.isEmpty();
    }

    public int shopCount() {
        return shops.size();
    }

    public int itemCount() {
        return items.values().stream().mapToInt(List::size).sum();
    }

    /** Every shop, sorted by display name. */
    public List<Shop> shops() {
        return shops.values().stream()
                .sorted(Comparator.comparing(Shop::display, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Shop shop(String id) {
        return id == null ? null : shops.get(id.toLowerCase(Locale.ROOT));
    }

    public List<ShopItem> itemsOf(String shopId) {
        return items.getOrDefault(shopId == null ? "" : shopId.toLowerCase(Locale.ROOT), List.of());
    }

    public ShopItem findItem(String shopId, String key) {
        for (ShopItem item : itemsOf(shopId)) {
            if (item.key().equals(key)) {
                return item;
            }
        }
        return null;
    }

    /** Finds one item anywhere, by its unique key, so a search result can be opened. */
    public ShopItem findItem(String key) {
        if (key == null) {
            return null;
        }
        for (List<ShopItem> list : items.values()) {
            for (ShopItem item : list) {
                if (item.key().equals(key)) {
                    return item;
                }
            }
        }
        return null;
    }

    /** The number of pages a shop needs. */
    public int pages(String shopId) {
        Map<Integer, Map<Integer, ShopItem>> layout = layout(shopId);
        return Math.max(1, layout.keySet().stream().mapToInt(Integer::intValue).max().orElse(0));
    }

    /** The items of one page, keyed by the slot they should be drawn in. */
    public Map<Integer, ShopItem> page(String shopId, int page) {
        return layout(shopId).getOrDefault(Math.max(1, page), Map.of());
    }

    /**
     * Full text search across every shop. An empty query returns everything.
     * Sorted by shop and then by the order the items were imported in.
     */
    public List<ShopItem> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<ShopItem> results = new ArrayList<>();
        for (ShopItem item : allItems()) {
            if (needle.isEmpty() || item.search().contains(needle)) {
                results.add(item);
            }
        }
        results.sort(Comparator.comparing(ShopItem::shop, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(ShopItem::page)
                .thenComparingInt(ShopItem::slot));
        return results;
    }

    public List<ShopItem> allItems() {
        List<ShopItem> all = new ArrayList<>();
        for (List<ShopItem> list : items.values()) {
            all.addAll(list);
        }
        return all;
    }

    /* ------------------------------------------------------------ layout --- */

    /**
     * Places every item of a shop into a page/slot grid, honouring the slot and
     * page an item was imported with where they fit and moving it to the first
     * free slot otherwise. Nothing is ever dropped, so an import with odd slot
     * numbers still shows every item.
     */
    private Map<Integer, Map<Integer, ShopItem>> layout(String shopId) {
        String key = shopId == null ? "" : shopId.toLowerCase(Locale.ROOT);
        return layouts.computeIfAbsent(key, ignored -> {
            Shop shop = shops.get(key);
            int rows = shop == null || shop.rows() <= 0 ? guiRows() : shop.rows();
            int pageSize = Math.max(1, (Math.max(2, Math.min(6, rows)) - 1) * 9);

            Map<Integer, Map<Integer, ShopItem>> layout = new TreeMap<>();
            List<ShopItem> sorted = new ArrayList<>(items.getOrDefault(key, List.of()));
            sorted.sort(Comparator.comparingInt(ShopItem::page).thenComparingInt(ShopItem::slot));

            for (ShopItem item : sorted) {
                int page = Math.max(1, item.page());
                int slot = item.slot();
                Map<Integer, ShopItem> pageMap = layout.computeIfAbsent(page, ignored2 -> new LinkedHashMap<>());
                if (slot < 0 || slot >= pageSize || pageMap.containsKey(slot)) {
                    int free = freeSlot(pageMap, pageSize);
                    if (free < 0) {
                        page++;
                        pageMap = layout.computeIfAbsent(page, ignored2 -> new LinkedHashMap<>());
                        free = freeSlot(pageMap, pageSize);
                    }
                    slot = Math.max(0, free);
                }
                pageMap.put(slot, item);
            }
            return layout;
        });
    }

    private static int freeSlot(Map<Integer, ShopItem> pageMap, int pageSize) {
        for (int slot = 0; slot < pageSize; slot++) {
            if (!pageMap.containsKey(slot)) {
                return slot;
            }
        }
        return -1;
    }

    /* ------------------------------------------------------------- load --- */

    /** Reads the shops into memory, preferring MySQL and falling back locally. */
    public void loadAll() {
        shops.clear();
        items.clear();
        layouts.clear();
        if (database.isAvailable()) {
            try {
                boolean loaded = loadFromDatabase();
                if (loaded) {
                    return;
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not read the shop from MySQL, using the local safe files: "
                        + e.getMessage());
                database.markUnavailable();
            }
        }
        loadFromLocal();
    }

    /** @return whether MySQL answered at all (an empty table is still an answer) */
    private boolean loadFromDatabase() throws SQLException {
        Map<String, Map<String, Object>> shopRows = new LinkedHashMap<>();
        Map<String, Map<String, Object>> itemRows = new LinkedHashMap<>();

        database.withConnection(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT * FROM `" + database.table("shops") + "`")) {
                while (result.next()) {
                    Shop shop = shopFromResult(result);
                    if (shop != null) {
                        shops.put(shop.id(), shop);
                        shopRows.put(shop.id(), shopRow(shop));
                    }
                }
            }
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT * FROM `" + database.table("shop_items") + "`")) {
                while (result.next()) {
                    ShopItem item = itemFromResult(result);
                    if (item != null) {
                        items.computeIfAbsent(item.shop(), ignored -> new ArrayList<>()).add(item);
                        itemRows.put(LocalStore.composite(item.shop(), item.key()), itemRow(item));
                    }
                }
            }
            return null;
        });

        localShops.mergeAll(shopRows);
        localItems.mergeAll(itemRows);
        return true;
    }

    private void loadFromLocal() {
        for (Map<String, Object> row : localShops.rawSnapshot()) {
            String id = LocalStore.string(row, LocalStore.KEY);
            Shop shop = shopFromRow(id, row);
            if (shop != null) {
                shops.put(shop.id(), shop);
            }
        }
        for (Map<String, Object> row : localItems.rawSnapshot()) {
            ShopItem item = itemFromRow(row);
            if (item != null) {
                items.computeIfAbsent(item.shop(), ignored -> new ArrayList<>()).add(item);
            }
        }
        if (!shops.isEmpty()) {
            plugin.getLogger().info("Loaded " + shopCount() + " shop(s) with "
                    + itemCount() + " item(s) from the local safe files.");
        }
    }

    private Shop shopFromResult(ResultSet result) throws SQLException {
        String id = result.getString("id");
        if (id == null || id.isBlank()) {
            return null;
        }
        ItemStack icon = decode(result.getString("icon"));
        return new Shop(id.toLowerCase(Locale.ROOT), displayOr(result.getString("display"), id),
                icon == null ? new ItemStack(Material.CHEST) : icon, result.getInt("rows"));
    }

    private Shop shopFromRow(String id, Map<String, Object> row) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ItemStack icon = decode(LocalStore.string(row, "icon"));
        return new Shop(id.toLowerCase(Locale.ROOT), displayOr(LocalStore.string(row, "display"), id),
                icon == null ? new ItemStack(Material.CHEST) : icon,
                (int) LocalStore.longValue(row, "rows", 0L));
    }

    /** A shop always needs a name to show; an empty one falls back to its id. */
    private static String displayOr(String display, String id) {
        return display == null || display.isBlank() ? id : display;
    }

    private ShopItem itemFromResult(ResultSet result) throws SQLException {
        String shop = result.getString("shop");
        String key = result.getString("item_key");
        if (shop == null || key == null) {
            return null;
        }
        ItemStack display = decode(result.getString("item"));
        if (display == null) {
            return null;
        }
        Material material = display.getType();
        return new ShopItem(shop.toLowerCase(Locale.ROOT), key, material, display,
                result.getDouble("buy_price"), result.getDouble("sell_price"),
                result.getInt("slot"), result.getInt("page"),
                searchOr(result.getString("search"), material));
    }

    private ShopItem itemFromRow(Map<String, Object> row) {
        String shop = LocalStore.string(row, "shop");
        String key = LocalStore.string(row, "item_key");
        if (shop == null || key == null) {
            return null;
        }
        ItemStack display = decode(LocalStore.string(row, "item"));
        if (display == null) {
            return null;
        }
        return new ShopItem(shop.toLowerCase(Locale.ROOT), key, display.getType(), display,
                LocalStore.doubleValue(row, "buy_price", -1.0),
                LocalStore.doubleValue(row, "sell_price", -1.0),
                (int) LocalStore.longValue(row, "slot", 0L),
                (int) LocalStore.longValue(row, "page", 1L),
                searchOr(LocalStore.string(row, "search"), display.getType()));
    }

    /** The lowercased text the search box matches against; an empty one is rebuilt. */
    private static String searchOr(String search, Material material) {
        if (search != null && !search.isBlank()) {
            return search;
        }
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static Map<String, Object> shopRow(Shop shop) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", shop.id());
        row.put("display", shop.display());
        row.put("icon", encode(shop.icon()));
        row.put("rows", shop.rows());
        return row;
    }

    private static Map<String, Object> itemRow(ShopItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("shop", item.shop());
        row.put("item_key", item.key());
        row.put("material", item.material().name());
        row.put("search", item.search());
        row.put("item", encode(item.display()));
        row.put("buy_price", item.buyPrice());
        row.put("sell_price", item.sellPrice());
        row.put("slot", item.slot());
        row.put("page", item.page());
        return row;
    }

    /* ----------------------------------------------------------- replace --- */

    /**
     * Replaces the whole shop with the given contents. Used by the importer, so
     * a re-import never leaves half of an old file behind.
     */
    public void replaceAll(List<Shop> newShops, Map<String, List<ShopItem>> newItems) {
        shops.clear();
        items.clear();
        layouts.clear();
        for (Shop shop : newShops) {
            shops.put(shop.id(), shop);
        }
        for (Map.Entry<String, List<ShopItem>> entry : newItems.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                items.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        saveLocally();
        persistAll();
    }

    private void saveLocally() {
        Map<String, Map<String, Object>> shopRows = new LinkedHashMap<>();
        for (Shop shop : shops.values()) {
            shopRows.put(shop.id(), shopRow(shop));
        }
        Map<String, Map<String, Object>> itemRows = new LinkedHashMap<>();
        for (List<ShopItem> list : items.values()) {
            for (ShopItem item : list) {
                itemRows.put(LocalStore.composite(item.shop(), item.key()), itemRow(item));
            }
        }
        localShops.replaceAll(rowsWithKeys(shopRows));
        localItems.replaceAll(rowsWithKeys(itemRows));
    }

    private static List<Map<String, Object>> rowsWithKeys(Map<String, Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        rows.forEach((key, row) -> {
            Map<String, Object> withKey = new LinkedHashMap<>(row);
            withKey.put(LocalStore.KEY, key);
            out.add(withKey);
        });
        return out;
    }

    /** Pushes the whole shop back into the database, e.g. after a reconnect. */
    public void resyncToDatabase() {
        if (shops.isEmpty() || !database.isAvailable()) {
            return;
        }
        persistAll();
    }

    private void persistAll() {
        List<Shop> snapshotShops = new ArrayList<>(shops.values());
        List<ShopItem> snapshotItems = allItems();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!database.isAvailable()) {
                return;
            }
            try {
                database.withConnection(connection -> {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("DELETE FROM `" + database.table("shops") + "`");
                        statement.executeUpdate("DELETE FROM `" + database.table("shop_items") + "`");
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO `" + database.table("shops") + "`"
                                    + " (`id`, `display`, `icon`, `rows`) VALUES (?, ?, ?, ?)")) {
                        for (Shop shop : snapshotShops) {
                            statement.setString(1, shop.id());
                            statement.setString(2, shop.display());
                            statement.setString(3, encode(shop.icon()));
                            statement.setInt(4, shop.rows());
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO `" + database.table("shop_items") + "`"
                                    + " (`shop`, `item_key`, `material`, `search`, `item`, `buy_price`,"
                                    + " `sell_price`, `slot`, `page`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        for (ShopItem item : snapshotItems) {
                            statement.setString(1, item.shop());
                            statement.setString(2, item.key());
                            statement.setString(3, item.material().name());
                            statement.setString(4, item.search());
                            statement.setString(5, encode(item.display()));
                            statement.setDouble(6, item.buyPrice());
                            statement.setDouble(7, item.sellPrice());
                            statement.setInt(8, item.slot());
                            statement.setInt(9, item.page());
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not store the shop: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /* ------------------------------------------------------------- trade --- */

    /** Buys {@code quantity} units of an item. Runs on the server thread. */
    public TradeResult buy(Player player, ShopItem item, int quantity) {
        if (item == null || !item.buyable()) {
            return TradeResult.NOT_BUYABLE;
        }
        if (!canAccess(player, item.shop())) {
            return TradeResult.NO_ACCESS;
        }
        int amount = Math.max(1, quantity);
        double total = item.buyPrice() * amount;
        if (plugin.economy().getBalance(player.getUniqueId()) < total) {
            return TradeResult.NO_MONEY;
        }
        ItemStack prototype = stockOf(item);
        int pieces = prototype.getAmount() * amount;
        if (!canFit(player, prototype, pieces)) {
            return TradeResult.NO_SPACE;
        }
        plugin.economy().withdraw(player.getUniqueId(), total);
        for (ItemStack stack : split(prototype, pieces)) {
            AuctionService.give(player, stack);
        }
        return TradeResult.SUCCESS;
    }

    /**
     * Sells {@code quantity} units of an item out of the player's inventory, or
     * as many complete units as the player carries when that is fewer.
     */
    public TradeResult sell(Player player, ShopItem item, int quantity) {
        if (item == null || !item.sellable() || !sellingEnabled()) {
            return TradeResult.NOT_SELLABLE;
        }
        if (!canAccess(player, item.shop())) {
            return TradeResult.NO_ACCESS;
        }
        int units = sellableUnits(player, item, Math.max(1, quantity));
        if (units <= 0) {
            return TradeResult.NOTHING_TO_SELL;
        }
        payForUnits(player, item, units);
        return TradeResult.SUCCESS;
    }

    /** Sells everything of that material the player carries. */
    public int sellEverything(Player player, ShopItem item) {
        if (item == null || !item.sellable() || !sellingEnabled()) {
            return 0;
        }
        if (!canAccess(player, item.shop())) {
            return 0;
        }
        int units = sellableUnits(player, item, Integer.MAX_VALUE);
        if (units <= 0) {
            return 0;
        }
        payForUnits(player, item, units);
        return units;
    }

    /**
     * How many whole units of an entry the player can sell. Only complete bundles
     * count, so a half-full bundle is never paid for as a full one.
     */
    private int sellableUnits(Player player, ShopItem item, int wanted) {
        int unit = Math.max(1, item.unit());
        int bundles = countMaterial(player, item.material()) / unit;
        return Math.max(0, Math.min(bundles, wanted));
    }

    private void payForUnits(Player player, ShopItem item, int units) {
        removeMaterial(player, item.material(), units * item.unit());
        plugin.economy().deposit(player.getUniqueId(), item.sellPrice() * units);
    }

    /** The stack one purchase of this entry hands out. */
    public static ItemStack stockOf(ShopItem item) {
        return item.display().clone();
    }

    private static int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private static void removeMaterial(Player player, Material material, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int index = 0; index < contents.length && left > 0; index++) {
            ItemStack stack = contents[index];
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int take = Math.min(left, stack.getAmount());
            left -= take;
            if (take >= stack.getAmount()) {
                contents[index] = null;
            } else {
                stack.setAmount(stack.getAmount() - take);
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    /** Whether the given number of pieces of a prototype still fits in the inventory. */
    public static boolean canFit(Player player, ItemStack prototype, int pieces) {
        return fits(player.getInventory().getStorageContents(), prototype, pieces);
    }

    private static boolean fits(ItemStack[] contents, ItemStack prototype, int pieces) {
        int capacity = 0;
        int max = prototype.getMaxStackSize();
        for (ItemStack slot : contents) {
            if (slot == null || slot.getType().isAir()) {
                capacity += max;
            } else if (slot.isSimilar(prototype)) {
                capacity += Math.max(0, max - slot.getAmount());
            }
            if (capacity >= pieces) {
                return true;
            }
        }
        return false;
    }

    /** Splits a total number of pieces into stacks of the prototype's max size. */
    public static List<ItemStack> split(ItemStack prototype, int pieces) {
        List<ItemStack> stacks = new ArrayList<>();
        int max = Math.max(1, prototype.getMaxStackSize());
        int left = pieces;
        while (left > 0) {
            int take = Math.min(max, left);
            ItemStack stack = prototype.clone();
            stack.setAmount(take);
            stacks.add(stack);
            left -= take;
        }
        return stacks;
    }

    /* ----------------------------------------------------------- helpers -- */

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
