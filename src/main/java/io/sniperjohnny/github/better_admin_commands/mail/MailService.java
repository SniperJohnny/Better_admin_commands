package io.sniperjohnny.github.better_admin_commands.mail;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Offline messages (mail) stored in the {@code mail} table.
 */
public class MailService {

    /** A single stored mail message. */
    public record Mail(long id, UUID senderUuid, String senderName, UUID targetUuid,
                       String message, long sentAt, boolean read) {
    }

    private final Better_Admin_Commands plugin;
    private final Database database;

    public MailService(Better_Admin_Commands plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /** Stores a new message. Runs asynchronously. */
    public void send(Player sender, UUID target, String message) {
        String sql = "INSERT INTO `" + database.table("mail")
                + "` (`sender_uuid`, `sender_name`, `target_uuid`, `message`, `sent_at`, `is_read`)"
                + " VALUES (?, ?, ?, ?, ?, 0)";
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, sender.getUniqueId().toString());
                        statement.setString(2, sender.getName());
                        statement.setString(3, target.toString());
                        statement.setString(4, message);
                        statement.setLong(5, System.currentTimeMillis());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not store mail: " + e.getMessage());
            }
        });
    }

    /** All messages of a player, oldest first. */
    public List<Mail> inbox(UUID uuid, boolean onlyUnread) throws SQLException {
        String sql = "SELECT * FROM `" + database.table("mail") + "` WHERE `target_uuid` = ?"
                + (onlyUnread ? " AND `is_read` = 0" : "") + " ORDER BY `sent_at` ASC";
        return database.withConnection(connection -> {
            List<Mail> messages = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        messages.add(new Mail(
                                result.getLong("id"),
                                UUID.fromString(result.getString("sender_uuid")),
                                result.getString("sender_name"),
                                UUID.fromString(result.getString("target_uuid")),
                                result.getString("message"),
                                result.getLong("sent_at"),
                                result.getInt("is_read") != 0));
                    }
                }
            }
            return messages;
        });
    }

    /** Number of unread messages. */
    public int unreadCount(UUID uuid) {
        try {
            return database.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM `" + database.table("mail")
                                + "` WHERE `target_uuid` = ? AND `is_read` = 0")) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? result.getInt(1) : 0;
                    }
                }
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not count mail: " + e.getMessage());
            return 0;
        }
    }

    /** Marks every message of a player as read. */
    public void markAllRead(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE `" + database.table("mail") + "` SET `is_read` = 1 WHERE `target_uuid` = ?")) {
                        statement.setString(1, uuid.toString());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not mark mail as read: " + e.getMessage());
            }
        });
    }

    /** Deletes every message of a player. */
    public void clear(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM `" + database.table("mail") + "` WHERE `target_uuid` = ?")) {
                        statement.setString(1, uuid.toString());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not clear mail: " + e.getMessage());
            }
        });
    }

    /** Raw rows used by the backup command. */
    public List<Map<String, Object>> readAll() throws SQLException {
        return database.withConnection(connection -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT * FROM `" + database.table("mail") + "`")) {
                int columns = result.getMetaData().getColumnCount();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= columns; index++) {
                        row.put(result.getMetaData().getColumnLabel(index), result.getObject(index));
                    }
                    rows.add(row);
                }
            }
            return rows;
        });
    }
}
