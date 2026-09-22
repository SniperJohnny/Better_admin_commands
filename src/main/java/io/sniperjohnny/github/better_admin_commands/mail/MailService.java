package io.sniperjohnny.github.better_admin_commands.mail;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
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
import java.util.concurrent.CompletableFuture;

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
    private final LocalStore local;

    public MailService(Better_Admin_Commands plugin, Database database, LocalStore local) {
        this.plugin = plugin;
        this.database = database;
        this.local = local;
    }

    /**
     * Runs one piece of mail work off the main thread and reports when it is
     * done, so a menu can redraw only after a write has actually landed instead
     * of racing it.
     */
    private CompletableFuture<Void> runAsync(Runnable work) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                work.run();
            } finally {
                done.complete(null);
            }
        });
        return done;
    }

    /**
     * Stores a new message. Runs asynchronously. Always mirrored locally.
     *
     * @return completed once the message is stored (or locally mirrored)
     */
    public CompletableFuture<Void> send(Player sender, UUID target, String message) {
        long sentAt = System.currentTimeMillis();
        String key = UUID.randomUUID().toString();
        return runAsync(() -> {
            local.merge(key, mailRow(sender.getUniqueId().toString(), sender.getName(),
                    target.toString(), message, sentAt, false));
            if (!database.isAvailable()) {
                return;
            }
            String sql = "INSERT INTO `" + database.table("mail")
                    + "` (`sender_uuid`, `sender_name`, `target_uuid`, `message`, `sent_at`, `is_read`)"
                    + " VALUES (?, ?, ?, ?, ?, 0)";
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, sender.getUniqueId().toString());
                        statement.setString(2, sender.getName());
                        statement.setString(3, target.toString());
                        statement.setString(4, message);
                        statement.setLong(5, sentAt);
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not store mail: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /** All messages of a player, oldest first. */
    public List<Mail> inbox(UUID uuid, boolean onlyUnread) throws SQLException {
        if (database.isAvailable()) {
            String sql = "SELECT * FROM `" + database.table("mail") + "` WHERE `target_uuid` = ?"
                    + (onlyUnread ? " AND `is_read` = 0" : "") + " ORDER BY `sent_at` ASC";
            try {
                return database.withConnection(connection -> {
                    List<Mail> messages = new ArrayList<>();
                    Map<String, Map<String, Object>> localRows = new LinkedHashMap<>();
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, uuid.toString());
                        try (ResultSet result = statement.executeQuery()) {
                            while (result.next()) {
                                Mail mail = new Mail(
                                        result.getLong("id"),
                                        UUID.fromString(result.getString("sender_uuid")),
                                        result.getString("sender_name"),
                                        UUID.fromString(result.getString("target_uuid")),
                                        result.getString("message"),
                                        result.getLong("sent_at"),
                                        result.getInt("is_read") != 0);
                                messages.add(mail);
                                localRows.put(LocalStore.composite(mail.senderUuid(), mail.targetUuid(), mail.sentAt()),
                                        mailRow(mail.senderUuid().toString(), mail.senderName(),
                                                mail.targetUuid().toString(), mail.message(), mail.sentAt(),
                                                mail.read()));
                            }
                        }
                    }
                    local.mergeAll(localRows);
                    return messages;
                });
            } catch (SQLException e) {
                database.markUnavailable();
            }
        }
        return localInbox(uuid, onlyUnread);
    }

    private List<Mail> localInbox(UUID uuid, boolean onlyUnread) {
        List<Mail> messages = new ArrayList<>();
        for (Map<String, Object> row : local.snapshot()) {
            if (!uuid.toString().equals(LocalStore.string(row, "target_uuid"))) {
                continue;
            }
            boolean read = Boolean.parseBoolean(String.valueOf(row.get("is_read")));
            if (onlyUnread && read) {
                continue;
            }
            Mail mail = mailFromRow(row, read);
            if (mail != null) {
                messages.add(mail);
            }
        }
        messages.sort(java.util.Comparator.comparingLong(Mail::sentAt));
        return messages;
    }

    private static Mail mailFromRow(Map<String, Object> row, boolean read) {
        try {
            return new Mail(
                    0L,
                    UUID.fromString(LocalStore.string(row, "sender_uuid")),
                    LocalStore.string(row, "sender_name"),
                    UUID.fromString(LocalStore.string(row, "target_uuid")),
                    LocalStore.string(row, "message"),
                    LocalStore.longValue(row, "sent_at", 0L),
                    read);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /** Number of unread messages. */
    public int unreadCount(UUID uuid) {
        if (database.isAvailable()) {
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
                database.markUnavailable();
            }
        }
        return localInbox(uuid, true).size();
    }

    /** Marks every message of a player as read. */
    public CompletableFuture<Void> markAllRead(UUID uuid) {
        return runAsync(() -> {
            local.updateWhere(row -> uuid.toString().equals(LocalStore.string(row, "target_uuid")),
                    java.util.Map.of("is_read", true));
            if (!database.isAvailable()) {
                return;
            }
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
                database.markUnavailable();
            }
        });
    }

    /**
     * Marks one message as read. Matched by target and send time, so it works
     * both against the database and against the local safe file - the local rows
     * carry no auto-increment id.
     */
    public CompletableFuture<Void> markRead(UUID target, Mail mail) {
        if (target == null || mail == null) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(() -> {
            local.updateWhere(row -> matches(target, mail, row), Map.of("is_read", true));
            if (!database.isAvailable()) {
                return;
            }
            String sql = "UPDATE `" + database.table("mail")
                    + "` SET `is_read` = 1 WHERE `target_uuid` = ? AND `sent_at` = ?";
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, target.toString());
                        statement.setLong(2, mail.sentAt());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not mark mail as read: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /** Deletes a single message of a player. */
    public CompletableFuture<Void> delete(UUID target, Mail mail) {
        if (target == null || mail == null) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(() -> {
            local.deleteWhere(row -> matches(target, mail, row));
            if (!database.isAvailable()) {
                return;
            }
            String sql = "DELETE FROM `" + database.table("mail")
                    + "` WHERE `target_uuid` = ? AND `sent_at` = ?";
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, target.toString());
                        statement.setLong(2, mail.sentAt());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not delete mail: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /** Whether a safe-file row is the given message of the given player. */
    private static boolean matches(UUID target, Mail mail, Map<String, Object> row) {
        return target.toString().equals(LocalStore.string(row, "target_uuid"))
                && LocalStore.longValue(row, "sent_at", -1L) == mail.sentAt();
    }

    /** Deletes every message of a player. */
    public CompletableFuture<Void> clear(UUID uuid) {
        return runAsync(() -> {
            local.deleteWhere(row -> uuid.toString().equals(LocalStore.string(row, "target_uuid")));
            if (!database.isAvailable()) {
                return;
            }
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
                database.markUnavailable();
            }
        });
    }

    /** Raw rows used by the backup command. */
    public List<Map<String, Object>> readAll() throws SQLException {
        if (!database.isAvailable()) {
            return local.snapshot();
        }
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

    /**
     * Appends every locally stored message that the database does not know yet.
     * Messages are matched on sender, target and timestamp to avoid duplicates.
     */
    public void resyncToDatabase() {
        if (!database.isAvailable()) {
            return;
        }
        List<Map<String, Object>> localRows = local.snapshot();
        if (localRows.isEmpty()) {
            return;
        }
        try {
            database.withConnection(connection -> {
                java.util.Set<String> existing = new java.util.HashSet<>();
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(
                             "SELECT `sender_uuid`, `target_uuid`, `sent_at` FROM `"
                                     + database.table("mail") + "`")) {
                    while (result.next()) {
                        existing.add(LocalStore.composite(result.getString("sender_uuid"),
                                result.getString("target_uuid"), result.getLong("sent_at")));
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO `" + database.table("mail")
                                + "` (`sender_uuid`, `sender_name`, `target_uuid`, `message`, `sent_at`, `is_read`)"
                                + " VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (Map<String, Object> row : localRows) {
                        String senderUuid = LocalStore.string(row, "sender_uuid");
                        String targetUuid = LocalStore.string(row, "target_uuid");
                        long sentAt = LocalStore.longValue(row, "sent_at", 0L);
                        if (senderUuid == null || targetUuid == null
                                || !existing.add(LocalStore.composite(senderUuid, targetUuid, sentAt))) {
                            continue;
                        }
                        insert.setString(1, senderUuid);
                        insert.setString(2, LocalStore.string(row, "sender_name"));
                        insert.setString(3, targetUuid);
                        insert.setString(4, LocalStore.string(row, "message"));
                        insert.setLong(5, sentAt);
                        insert.setBoolean(6, Boolean.parseBoolean(String.valueOf(row.get("is_read"))));
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                return null;
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not sync mail back to MySQL: " + e.getMessage());
            database.markUnavailable();
        }
    }

    private static Map<String, Object> mailRow(String senderUuid, String senderName, String targetUuid,
                                               String message, long sentAt, boolean read) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sender_uuid", senderUuid);
        row.put("sender_name", senderName);
        row.put("target_uuid", targetUuid);
        row.put("message", message);
        row.put("sent_at", sentAt);
        row.put("is_read", read);
        return row;
    }
}
