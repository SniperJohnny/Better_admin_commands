package io.sniperjohnny.github.better_admin_commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyService {

    public record BalanceEntry(UUID uuid, String name, double balance) {
    }

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final LocalStore local;

    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    private final double startingBalance;
    private final double maxBalance;
    private final DecimalFormat format;
    private final String currencySymbol;

    public EconomyService(Better_Admin_Commands plugin, Database database, LocalStore local) {
        this.plugin = plugin;
        this.database = database;
        this.local = local;
        this.startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 100.0);
        this.maxBalance = plugin.getConfig().getDouble("economy.max-balance", 1_000_000_000.0);
        this.currencySymbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        DecimalFormat parsed;
        try {
            parsed = new DecimalFormat(plugin.getConfig().getString("economy.format", "#,##0.00"));
        } catch (IllegalArgumentException e) {
            parsed = new DecimalFormat("#,##0.00");
        }
        this.format = parsed;
    }

    public String format(double amount) {
        return currencySymbol + format.format(amount);
    }

    public void loadAll() throws SQLException {
        balances.clear();
        names.clear();
        if (database.isAvailable()) {
            try {
                loadFromDatabase();
                return;
            } catch (SQLException e) {
                database.markUnavailable();
                plugin.getLogger().warning("Could not read balances from MySQL, using the local safe file: "
                        + e.getMessage());
            }
        }
        loadFromLocal();
    }

    private void loadFromDatabase() throws SQLException {
        Map<String, Map<String, Object>> localRows = new java.util.LinkedHashMap<>();
        database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `uuid`, `name`, `balance`, `last_seen` FROM `" + database.table("players") + "`");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID uuid = parseUuid(result.getString("uuid"));
                    if (uuid == null) {
                        continue;
                    }
                    double balance = result.getDouble("balance");
                    String name = result.getString("name");
                    long lastSeen = result.getLong("last_seen");
                    balances.put(uuid, balance);
                    names.put(uuid, name);
                    localRows.put(uuid.toString(), playerRow(uuid.toString(), name, balance, lastSeen));
                }
            }
            return null;
        });
        local.mergeAll(localRows);
    }

    private void loadFromLocal() {
        for (Map<String, Object> row : local.snapshot()) {
            UUID uuid = parseUuid(LocalStore.string(row, "uuid"));
            if (uuid == null) {
                continue;
            }
            balances.put(uuid, LocalStore.doubleValue(row, "balance", 0.0));
            String name = LocalStore.string(row, "name");
            if (name != null) {
                names.put(uuid, name);
            }
        }
        plugin.getLogger().info("Loaded " + balances.size() + " balance(s) from the local safe file.");
    }

    private static Map<String, Object> playerRow(String uuid, String name, double balance, long lastSeen) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("uuid", uuid);
        row.put("name", name);
        row.put("balance", balance);
        row.put("last_seen", lastSeen);
        return row;
    }

    public void ensureAccount(UUID uuid, String name) {
        names.put(uuid, name);
        if (balances.containsKey(uuid)) {
            dirty.add(uuid);
            return;
        }
        balances.put(uuid, startingBalance);
        dirty.add(uuid);
    }

    public boolean hasAccount(UUID uuid) {
        return balances.containsKey(uuid);
    }

    public boolean hasAccount(OfflinePlayer player) {
        return player != null && hasAccount(player.getUniqueId());
    }

    public double getBalance(UUID uuid) {
        Double balance = balances.get(uuid);
        if (balance == null) {
            // A UUID lookup does not hit the network; only a name lookup would.
            String name = names.get(uuid);
            if (name == null) {
                OfflinePlayer offline = plugin.getServer().getOfflinePlayer(uuid);
                name = offline.getName() == null ? uuid.toString() : offline.getName();
            }
            ensureAccount(uuid, name);
            balance = startingBalance;
        }
        return balance;
    }

    public double getBalance(OfflinePlayer player) {
        return player == null ? 0.0 : getBalance(player.getUniqueId());
    }

    public String nameOf(UUID uuid) {
        String name = names.get(uuid);
        if (name != null) {
            return name;
        }
        OfflinePlayer offline = plugin.getServer().getOfflinePlayer(uuid);
        name = offline.getName() == null ? uuid.toString() : offline.getName();
        names.put(uuid, name);
        return name;
    }

    public void setBalance(UUID uuid, double amount) {
        double clamped = Math.max(0.0, Math.min(maxBalance, amount));
        balances.put(uuid, clamped);
        dirty.add(uuid);
    }

    public double deposit(UUID uuid, double amount) {
        double updated = Math.max(0.0, Math.min(maxBalance, getBalance(uuid) + amount));
        balances.put(uuid, updated);
        dirty.add(uuid);
        return updated;
    }

    public boolean withdraw(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) {
            return false;
        }
        balances.put(uuid, Math.max(0.0, current - amount));
        dirty.add(uuid);
        return true;
    }

    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public enum TransferResult { SUCCESS, PAYMENTS_DISABLED, SELF, TOO_SMALL, TOO_POOR }

    public TransferResult transfer(Player from, UUID target, double amount) {
        if (!plugin.getConfig().getBoolean("economy.allow-payments", true)) {
            return TransferResult.PAYMENTS_DISABLED;
        }
        if (target == null || target.equals(from.getUniqueId())) {
            return TransferResult.SELF;
        }
        if (amount < plugin.getConfig().getDouble("economy.minimum-payment", 0.01)) {
            return TransferResult.TOO_SMALL;
        }
        if (!withdraw(from.getUniqueId(), amount)) {
            return TransferResult.TOO_POOR;
        }
        deposit(target, amount);
        saveAsync();
        return TransferResult.SUCCESS;
    }

    public double minimumPayment() {
        return plugin.getConfig().getDouble("economy.minimum-payment", 0.01);
    }

    public boolean paymentsAllowed() {
        return plugin.getConfig().getBoolean("economy.allow-payments", true);
    }

    public double startingBalance() {
        return startingBalance;
    }

    public java.util.Set<UUID> accountIds() {
        return new java.util.LinkedHashSet<>(balances.keySet());
    }

    public List<BalanceEntry> top(int limit) {
        List<BalanceEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            entries.add(new BalanceEntry(entry.getKey(), nameOf(entry.getKey()), entry.getValue()));
        }
        entries.sort(Comparator.comparingDouble(BalanceEntry::balance).reversed());
        return entries.size() <= limit ? entries : entries.subList(0, limit);
    }

    public double maxBalance() {
        return maxBalance;
    }

    public String currencySymbol() {
        return currencySymbol;
    }

    public void saveDirty() {
        if (dirty.isEmpty()) {
            return;
        }
        List<UUID> pending = new ArrayList<>(dirty);
        dirty.removeAll(pending);

        long now = System.currentTimeMillis();
        Map<String, Map<String, Object>> localRows = new java.util.LinkedHashMap<>();
        for (UUID uuid : pending) {
            Double balance = balances.get(uuid);
            if (balance == null) {
                continue;
            }
            localRows.put(uuid.toString(), playerRow(uuid.toString(), nameOf(uuid), balance, now));
        }
        local.mergeAll(localRows);

        if (!database.isAvailable()) {
            return;
        }

        String sql = "INSERT INTO `" + database.table("players") + "`"
                + " (`uuid`, `name`, `balance`, `last_seen`) VALUES (?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE `name` = VALUES(`name`),"
                + " `balance` = VALUES(`balance`), `last_seen` = VALUES(`last_seen`)";

        try {
            database.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (UUID uuid : pending) {
                        Double balance = balances.get(uuid);
                        if (balance == null) {
                            continue;
                        }
                        statement.setString(1, uuid.toString());
                        statement.setString(2, nameOf(uuid));
                        statement.setDouble(3, balance);
                        statement.setLong(4, now);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                return null;
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save balances to MySQL: " + e.getMessage());
            database.markUnavailable();
        }
    }

    public void resyncToDatabase() {
        if (!database.isAvailable()) {
            return;
        }
        List<Map<String, Object>> rows = local.snapshot();
        String sql = "INSERT INTO `" + database.table("players") + "`"
                + " (`uuid`, `name`, `balance`, `last_seen`) VALUES (?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE `name` = VALUES(`name`),"
                + " `balance` = VALUES(`balance`), `last_seen` = VALUES(`last_seen`)";
        try {
            database.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (Map<String, Object> row : rows) {
                        String uuid = LocalStore.string(row, "uuid");
                        if (uuid == null || !row.containsKey("balance")) {
                            continue;
                        }
                        statement.setString(1, uuid);
                        statement.setString(2, LocalStore.string(row, "name"));
                        statement.setDouble(3, LocalStore.doubleValue(row, "balance", 0.0));
                        statement.setLong(4, LocalStore.longValue(row, "last_seen", 0L));
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                return null;
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not sync balances back to MySQL: " + e.getMessage());
            database.markUnavailable();
        }
    }

    public void saveAsync() {
        if (dirty.isEmpty()) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveDirty);
    }

    public void saveAsync(UUID uuid) {
        dirty.add(uuid);
        saveAsync();
    }

    public void saveBlocking() {
        saveDirty();
    }

    public void touch(Player player) {
        ensureAccount(player.getUniqueId(), player.getName());
    }

    private static UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
