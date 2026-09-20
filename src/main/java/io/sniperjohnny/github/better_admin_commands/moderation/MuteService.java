package io.sniperjohnny.github.better_admin_commands.moderation;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    private final Map<UUID, Mute> mutes = new ConcurrentHashMap<>();

    public MuteService(Better_Admin_Commands plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /** Loads every active mute. Called once at start-up. */
    public void loadAll() throws SQLException {
        mutes.clear();
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
                }
            }
            return null;
        });
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
                        statement.setLong(3, System.currentTimeMillis());
                        statement.setLong(4, until);
                        statement.setString(5, reason);
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not store mute for " + uuid + ": " + e.getMessage());
            }
        });
    }
}
