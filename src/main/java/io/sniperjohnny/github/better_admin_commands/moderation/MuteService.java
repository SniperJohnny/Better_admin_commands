package io.sniperjohnny.github.better_admin_commands.moderation;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutes are stored in the players table so they survive restarts and restarts
 * of a network that shares the same database.
 */
public class MuteService {

    /** A stored mute. {@code until < 0} means permanent. */
    public record Mute(long until, String reason) {

        public boolean permanent() {
            return until < 0;
        }

        public boolean active() {
            return permanent() || until > System.currentTimeMillis();
        }

        public long remainingMillis() {
            return permanent() ? -1 : Math.max(0, until - System.currentTimeMillis());
        }
    }

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final LocalStore local;
    private final Map<UUID, Mute> mutes = new ConcurrentHashMap<>();

    public MuteService(Better_Admin_Commands plugin, Database database, LocalStore local) {
        this.plugin = plugin;
        this.database = database;
        this.local = local;
    }

    /**
     * Loads every active mute. Called once at start-up. Falls back to the local
     * safe file while MySQL is unavailable.
     */
    public void loadAll() throws SQLException {
        mutes.clear();
        if (database.isAvailable()) {
            try {
                loadFromDatabase();
                return;
            } catch (SQLException e) {
                database.markUnavailable();
                plugin.getLogger().warning("Could not read mutes from MySQL, using the local safe file: "
                        + e.getMessage());
            }
        }
        loadFromLocal();
    }

    private void loadFromDatabase() throws SQLException {
        Map<String, Map<String, Object>> localRows = new LinkedHashMap<>();
        database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `uuid`, `muted_until`, `mute_reason` FROM `" + database.table("players")
                            + "` WHERE `muted_until` <> 0");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(result.getString("uuid"));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    Mute mute = new Mute(result.getLong("muted_until"), result.getString("mute_reason"));
                    if (mute.active()) {
                        mutes.put(uuid, mute);
                    }
                    localRows.put(uuid.toString(), muteRow(uuid.toString(), mute.until(), mute.reason()));
                }
            }
            return null;
        });
        local.mergeAll(localRows);
    }

    private void loadFromLocal() {
        for (Map<String, Object> row : local.snapshot()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(LocalStore.string(row, "uuid"));
            } catch (IllegalArgumentException | NullPointerException e) {
                continue;
            }
            long until = LocalStore.longValue(row, "muted_until", 0L);
            if (until == 0L) {
                continue;
            }
            Mute mute = new Mute(until, LocalStore.string(row, "mute_reason"));
            if (mute.active()) {
                mutes.put(uuid, mute);
            }
        }
        plugin.getLogger().info("Loaded " + mutes.size() + " mute(s) from the local safe file.");
    }

    private static Map<String, Object> muteRow(String uuid, long until, String reason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("uuid", uuid);
        row.put("muted_until", until);
        row.put("mute_reason", reason);
        return row;
    }

    public Mute muteOf(UUID uuid) {
        Mute mute = mutes.get(uuid);
        if (mute == null) {
            return null;
        }
        if (!mute.active()) {
            mutes.remove(uuid);
            persistAsync(uuid, 0L, null);
            return null;
        }
        return mute;
    }

    public boolean isMuted(UUID uuid) {
        return muteOf(uuid) != null;
    }

    /** Applies a mute. {@code until} is an epoch millisecond timestamp, or -1 for permanent. */
    public void mute(UUID uuid, long until, String reason) {
        mutes.put(uuid, new Mute(until, reason));
        persistAsync(uuid, until, reason);
    }

    public void unmute(UUID uuid) {
        if (mutes.remove(uuid) == null) {
            return;
        }
        persistAsync(uuid, 0L, null);
    }

    private void persistAsync(UUID uuid, long until, String reason) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName() == null ? uuid.toString() : offline.getName();
            long now = System.currentTimeMillis();

            // Always mirror into the local safe file first.
            Map<String, Object> row = muteRow(uuid.toString(), until, reason);
            row.put("name", name);
            local.merge(uuid.toString(), row);

            if (!database.isAvailable()) {
                return;
            }
            String sql = "INSERT INTO `" + database.table("players") + "`"
                    + " (`uuid`, `name`, `balance`, `last_seen`, `muted_until`, `mute_reason`)"
                    + " VALUES (?, ?, 0, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE `muted_until` = VALUES(`muted_until`),"
                    + " `mute_reason` = VALUES(`mute_reason`)";
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, name);
                        statement.setLong(3, now);
                        statement.setLong(4, until);
                        statement.setString(5, reason);
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not store mute for " + uuid + ": " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /** Pushes the locally stored mute columns back into MySQL after a reconnect. */
    public void resyncToDatabase() {
        if (!database.isAvailable()) {
            return;
        }
        List<Map<String, Object>> rows = local.snapshot();
        String sql = "INSERT INTO `" + database.table("players") + "`"
                + " (`uuid`, `name`, `balance`, `last_seen`, `muted_until`, `mute_reason`)"
                + " VALUES (?, ?, 0, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE `muted_until` = VALUES(`muted_until`),"
                + " `mute_reason` = VALUES(`mute_reason`)";
        try {
            database.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (Map<String, Object> row : rows) {
                        String uuid = LocalStore.string(row, "uuid");
                        if (uuid == null || !row.containsKey("muted_until")) {
                            continue;
                        }
                        statement.setString(1, uuid);
                        statement.setString(2, LocalStore.string(row, "name"));
                        statement.setLong(3, LocalStore.longValue(row, "last_seen", 0L));
                        statement.setLong(4, LocalStore.longValue(row, "muted_until", 0L));
                        statement.setString(5, LocalStore.string(row, "mute_reason"));
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                return null;
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not sync mutes back to MySQL: " + e.getMessage());
            database.markUnavailable();
        }
    }
}
