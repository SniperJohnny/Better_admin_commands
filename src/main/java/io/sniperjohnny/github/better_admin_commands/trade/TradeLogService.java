package io.sniperjohnny.github.better_admin_commands.trade;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.notify.NotificationService;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores every completed trade, so staff can look back at what was exchanged.
 *
 * <p>A record lives in memory, is mirrored into {@code data/trades.yml} and is
 * written to MySQL in the background - the same pattern the rest of the plugin
 * uses. Records older than {@code trade.log.retention-hours} (48 hours by
 * default) are dropped from all three places.</p>
 *
 * <p>To keep the memory footprint small, the items are held as their encoded form
 * plus a ready-made one-line summary instead of as {@link ItemStack} lists, and
 * only the newest {@code trade.log.max-cached} records are kept in memory - the
 * rest stay on disk and in the database until their time is up.</p>
 */
public class TradeLogService {

    /** One completed trade. Items stay encoded; the summary is pre-rendered. */
    public record TradeRecord(String id, UUID firstUuid, String firstName, UUID secondUuid, String secondName,
                              double firstMoney, double secondMoney, String firstItems, String secondItems,
                              String summary, long createdAt) {

        public boolean involves(UUID uuid) {
            return firstUuid.equals(uuid) || secondUuid.equals(uuid);
        }
    }

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final LocalStore local;
    private final Map<String, TradeRecord> records = new ConcurrentHashMap<>();

    public TradeLogService(Better_Admin_Commands plugin, Database database, LocalStore local) {
        this.plugin = plugin;
        this.database = database;
        this.local = local;
    }

    /* ------------------------------------------------------------ config --- */

    public boolean enabled() {
        return plugin.getConfig().getBoolean("trade.log.enabled", true);
    }

    public long retentionMillis() {
        long hours = Math.max(1L, plugin.getConfig().getLong("trade.log.retention-hours", 48L));
        return hours * 3_600_000L;
    }

    /** How many records are held in memory; older ones stay on disk only. */
    private int maxCached() {
        return Math.max(100, plugin.getConfig().getInt("trade.log.max-cached", 2000));
    }

    /** The permission that receives trade notifications. */
    public String notifyPermission() {
        return plugin.getConfig().getString("trade.log.notify-permission",
                "betteradmincommands.trade.notify");
    }

    /* -------------------------------------------------------------- load --- */

    /** Reads the stored history, preferring MySQL and falling back locally. */
    public void loadAll() {
        records.clear();
        if (database.isAvailable()) {
            try {
                Map<String, Map<String, Object>> localRows = new LinkedHashMap<>();
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT * FROM `" + database.table("trades")
                                    + "` ORDER BY `created_at` DESC LIMIT ?")) {
                        statement.setInt(1, maxCached());
                        try (ResultSet result = statement.executeQuery()) {
                            while (result.next()) {
                                TradeRecord record = fromResult(result);
                                if (record != null) {
                                    records.put(record.id(), record);
                                    localRows.put(record.id(), rowOf(record));
                                }
                            }
                        }
                    }
                    return null;
                });
                local.mergeAll(localRows);
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not read the trade history from MySQL, using the local "
                        + "safe file: " + e.getMessage());
                database.markUnavailable();
                loadFromLocal();
            }
        } else {
            loadFromLocal();
        }
        purge();
    }

    private void loadFromLocal() {
        List<TradeRecord> loaded = new ArrayList<>();
        for (Map<String, Object> row : local.rawSnapshot()) {
            TradeRecord record = recordFromRow(LocalStore.string(row, LocalStore.KEY), row);
            if (record != null) {
                loaded.add(record);
            }
        }
        loaded.sort(Comparator.comparingLong(TradeRecord::createdAt).reversed());
        for (int index = 0; index < Math.min(loaded.size(), maxCached()); index++) {
            TradeRecord record = loaded.get(index);
            records.put(record.id(), record);
        }
    }

    /* ------------------------------------------------------------ record --- */

    /** Stores a completed trade and tells the staff about it. */
    public void record(UUID firstUuid, String firstName, UUID secondUuid, String secondName,
                       double firstMoney, double secondMoney,
                       ItemStack[] firstItems, ItemStack[] secondItems) {
        if (!enabled()) {
            return;
        }
        String encodedFirst = encode(firstItems);
        String encodedSecond = encode(secondItems);
        String summary = buildSummary(firstName, firstItems, firstMoney,
                secondName, secondItems, secondMoney);
        TradeRecord record = new TradeRecord(UUID.randomUUID().toString(), firstUuid, firstName,
                secondUuid, secondName, firstMoney, secondMoney, encodedFirst, encodedSecond,
                summary, System.currentTimeMillis());
        records.put(record.id(), record);
        local.merge(record.id(), rowOf(record));
        persist(record);
        trim();
        notifyStaff(record);
    }

    /** Drops the oldest cached records once the cache grew past its cap. */
    private void trim() {
        int max = maxCached();
        if (records.size() <= max) {
            return;
        }
        List<TradeRecord> all = new ArrayList<>(records.values());
        all.sort(Comparator.comparingLong(TradeRecord::createdAt).reversed());
        for (int index = max; index < all.size(); index++) {
            records.remove(all.get(index).id());
        }
    }

    /** Tells every online staff member what was traded, respecting their /notify toggle. */
    private void notifyStaff(TradeRecord record) {
        NotificationService.Category category = plugin.notifications() == null
                ? null : plugin.notifications().category("trade");
        String permission = notifyPermission();
        for (Player online : Bukkit.getOnlinePlayers()) {
            // The two traders already know; do not tell them their own trade.
            if (record.involves(online.getUniqueId())) {
                continue;
            }
            boolean allowed;
            if (permission != null && !permission.isBlank()) {
                allowed = plugin.permissions().has(online, permission);
            } else {
                allowed = category == null || category.permission().isBlank()
                        || plugin.permissions().has(online, category.permission());
            }
            if (!allowed) {
                continue;
            }
            if (category != null && !plugin.notifications().toggleEnabled(online, category)) {
                continue;
            }
            Msg.send(online, "&7Trade &f" + record.firstName() + " &7\u2194 &f" + record.secondName()
                    + "&7: " + record.summary());
        }
        plugin.getLogger().info("Trade " + record.firstName() + " <-> " + record.secondName()
                + ": " + PlainTextComponentSerializer.plainText().serialize(Msg.component(record.summary())));
    }

    /**
     * A one-line description of a trade, with colour codes when {@code plain} is
     * {@code false} (they are translated for the console).
     */
    public String summaryOf(TradeRecord record, boolean plain) {
        return plain ? Msg.color(record.summary()) : record.summary();
    }

    private String buildSummary(String firstName, ItemStack[] firstItems, double firstMoney,
                                String secondName, ItemStack[] secondItems, double secondMoney) {
        StringBuilder text = new StringBuilder();
        appendSide(text, firstName, firstItems, firstMoney);
        text.append(" &7\u2194 ");
        appendSide(text, secondName, secondItems, secondMoney);
        return text.toString();
    }

    private void appendSide(StringBuilder text, String name, ItemStack[] items, double money) {
        text.append("&f").append(name).append(" &7gave &f");
        boolean any = false;
        if (money > 0.0) {
            text.append(plugin.economy().format(money));
            any = true;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (any) {
                text.append("&7, &f");
            }
            text.append(item.getAmount()).append("x ").append(pretty(item));
            any = true;
        }
        if (!any) {
            text.append("nothing");
        }
    }

    /* ------------------------------------------------------------- reads --- */

    /** The newest trades first, at most {@code limit} rows. */
    public List<TradeRecord> recent(int limit) {
        List<TradeRecord> list = new ArrayList<>(records.values());
        list.sort(Comparator.comparingLong(TradeRecord::createdAt).reversed());
        return list.size() <= limit ? list : list.subList(0, limit);
    }

    /** The newest trades one player took part in. */
    public List<TradeRecord> recentFor(UUID uuid, int limit) {
        List<TradeRecord> list = new ArrayList<>();
        for (TradeRecord record : records.values()) {
            if (record.involves(uuid)) {
                list.add(record);
            }
        }
        list.sort(Comparator.comparingLong(TradeRecord::createdAt).reversed());
        return list.size() <= limit ? list : list.subList(0, limit);
    }

    public int size() {
        return records.size();
    }

    /* -------------------------------------------------------------- purge --- */

    /** Drops every record older than the configured retention window. */
    public int purge() {
        long cutoff = System.currentTimeMillis() - retentionMillis();
        List<String> expired = new ArrayList<>();
        for (TradeRecord record : records.values()) {
            if (record.createdAt() < cutoff) {
                expired.add(record.id());
            }
        }
        for (String id : expired) {
            records.remove(id);
            local.delete(id);
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> purgeRows(cutoff));
        return expired.size();
    }

    private void purgeRows(long cutoff) {
        if (!database.isAvailable()) {
            return;
        }
        try {
            database.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM `" + database.table("trades") + "` WHERE `created_at` < ?")) {
                    statement.setLong(1, cutoff);
                    statement.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not purge the trade history: " + e.getMessage());
            database.markUnavailable();
        }
    }

    /* -------------------------------------------------------- persistence --- */

    private void persist(TradeRecord record) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!database.isAvailable()) {
                return;
            }
            String sql = "INSERT INTO `" + database.table("trades")
                    + "` (`id`, `first_uuid`, `first_name`, `second_uuid`, `second_name`,"
                    + " `first_money`, `second_money`, `first_items`, `second_items`, `created_at`)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE `created_at` = VALUES(`created_at`)";
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, record.id());
                        statement.setString(2, record.firstUuid().toString());
                        statement.setString(3, record.firstName());
                        statement.setString(4, record.secondUuid().toString());
                        statement.setString(5, record.secondName());
                        statement.setDouble(6, record.firstMoney());
                        statement.setDouble(7, record.secondMoney());
                        statement.setString(8, record.firstItems());
                        statement.setString(9, record.secondItems());
                        statement.setLong(10, record.createdAt());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not store the trade history: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    private TradeRecord fromResult(ResultSet result) throws SQLException {
        String id = result.getString("id");
        String firstUuid = result.getString("first_uuid");
        String secondUuid = result.getString("second_uuid");
        if (id == null || firstUuid == null || secondUuid == null) {
            return null;
        }
        try {
            String firstItems = result.getString("first_items");
            String secondItems = result.getString("second_items");
            String firstName = result.getString("first_name");
            String secondName = result.getString("second_name");
            double firstMoney = result.getDouble("first_money");
            double secondMoney = result.getDouble("second_money");
            return new TradeRecord(id, UUID.fromString(firstUuid), firstName,
                    UUID.fromString(secondUuid), secondName, firstMoney, secondMoney,
                    firstItems, secondItems,
                    buildSummary(firstName, decode(firstItems), firstMoney,
                            secondName, decode(secondItems), secondMoney),
                    result.getLong("created_at"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TradeRecord recordFromRow(String id, Map<String, Object> row) {
        String firstUuid = LocalStore.string(row, "first_uuid");
        String secondUuid = LocalStore.string(row, "second_uuid");
        if (id == null || firstUuid == null || secondUuid == null) {
            return null;
        }
        try {
            String firstName = LocalStore.string(row, "first_name");
            String secondName = LocalStore.string(row, "second_name");
            String firstItems = LocalStore.string(row, "first_items");
            String secondItems = LocalStore.string(row, "second_items");
            double firstMoney = LocalStore.doubleValue(row, "first_money", 0.0);
            double secondMoney = LocalStore.doubleValue(row, "second_money", 0.0);
            return new TradeRecord(id, UUID.fromString(firstUuid), firstName,
                    UUID.fromString(secondUuid), secondName, firstMoney, secondMoney,
                    firstItems, secondItems,
                    buildSummary(firstName, decode(firstItems), firstMoney,
                            secondName, decode(secondItems), secondMoney),
                    LocalStore.longValue(row, "created_at", 0L));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<String, Object> rowOf(TradeRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("first_uuid", record.firstUuid().toString());
        row.put("first_name", record.firstName());
        row.put("second_uuid", record.secondUuid().toString());
        row.put("second_name", record.secondName());
        row.put("first_money", record.firstMoney());
        row.put("second_money", record.secondMoney());
        row.put("first_items", record.firstItems());
        row.put("second_items", record.secondItems());
        row.put("created_at", record.createdAt());
        return row;
    }

    /** Pushes the local history back into MySQL after a reconnect. */
    public void resyncToDatabase() {
        if (!database.isAvailable()) {
            return;
        }
        for (TradeRecord record : new ArrayList<>(records.values())) {
            persist(record);
        }
    }

    /* ------------------------------------------------------------ helpers --- */

    private static String encode(ItemStack[] items) {
        StringBuilder text = new StringBuilder();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
        }
        return text.toString();
    }

    private static ItemStack[] decode(String data) {
        if (data == null || data.isBlank()) {
            return new ItemStack[0];
        }
        List<ItemStack> items = new ArrayList<>();
        for (String part : data.split("\n")) {
            if (part.isBlank()) {
                continue;
            }
            try {
                ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(part.trim()));
                if (item.getType() != Material.AIR) {
                    items.add(item);
                }
            } catch (RuntimeException ignored) {
                // A corrupt entry is skipped rather than failing the whole load.
            }
        }
        return items.toArray(new ItemStack[0]);
    }

    private static String pretty(ItemStack item) {
        var meta = item.getItemMeta();
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
}
