package io.sniperjohnny.github.better_admin_commands.report;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Report tickets and the message thread behind them.
 *
 * <p>A ticket has one creator (the player) and any number of staff who answer it,
 * so several staff can work on the same ticket. Everyone who took part is a
 * "participant": that is what drives the unread counts and who gets notified when
 * a new message arrives.</p>
 *
 * <p>Everything is cached in memory and written to MySQL in the background. This
 * feature is the one part of the plugin that needs the database to be reachable
 * - unlike balances or mail there is no local fallback for it.</p>
 */
public class ReportService {

    public static final String BASE_PERMISSION = "betteradmincommands.report";
    public static final String STAFF_PERMISSION = "betteradmincommands.report.staff";

    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";

    /** A ticket. */
    public record Report(String id, UUID reporterUuid, String reporterName, String targetName,
                         String category, String status, long createdAt, long updatedAt) {
        public boolean isOpen() {
            return OPEN.equals(status);
        }
    }

    /** One message in a ticket's thread. */
    public record Message(String id, String reportId, UUID authorUuid, String authorName,
                          boolean staff, String message, long createdAt) {
    }

    /** Someone involved in a ticket: the creator, or any staff who answered it. */
    public record Participant(String reportId, UUID uuid, String name, boolean staff, long lastRead) {
    }

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final Map<String, Report> reports = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> messages = new ConcurrentHashMap<>();
    private final Map<String, List<Participant>> participants = new ConcurrentHashMap<>();

    public ReportService(Better_Admin_Commands plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /** Whether reports can be used at all (they need MySQL). */
    public boolean available() {
        return database.isAvailable();
    }

    /* -------------------------------------------------------------- load --- */

    public void loadAll() {
        reports.clear();
        messages.clear();
        participants.clear();
        if (!database.isAvailable()) {
            plugin.getLogger().warning("Reports need the MySQL database, which is unavailable - "
                    + "the report feature stays empty until the connection is back.");
            return;
        }
        try {
            database.withConnection(connection -> {
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(
                             "SELECT * FROM `" + database.table("reports") + "`")) {
                    while (result.next()) {
                        Report report = reportFrom(result);
                        if (report != null) {
                            reports.put(report.id(), report);
                        }
                    }
                }
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(
                             "SELECT * FROM `" + database.table("report_messages") + "` ORDER BY `created_at` ASC")) {
                    while (result.next()) {
                        Message message = messageFrom(result);
                        if (message != null) {
                            messages.computeIfAbsent(message.reportId(), ignored -> new CopyOnWriteArrayList<>())
                                    .add(message);
                        }
                    }
                }
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(
                             "SELECT * FROM `" + database.table("report_participants") + "`")) {
                    while (result.next()) {
                        Participant participant = participantFrom(result);
                        if (participant != null) {
                            participants.computeIfAbsent(participant.reportId(), ignored -> new CopyOnWriteArrayList<>())
                                    .add(participant);
                        }
                    }
                }
                return null;
            });
            plugin.getLogger().info("Loaded " + reports.size() + " report ticket(s).");
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load the report tickets: " + e.getMessage());
            database.markUnavailable();
        }
    }

    private Report reportFrom(ResultSet result) throws SQLException {
        try {
            return new Report(result.getString("id"), UUID.fromString(result.getString("reporter_uuid")),
                    result.getString("reporter_name"), result.getString("target_name"),
                    result.getString("category"), result.getString("status"),
                    result.getLong("created_at"), result.getLong("updated_at"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Message messageFrom(ResultSet result) throws SQLException {
        try {
            return new Message(result.getString("id"), result.getString("report_id"),
                    UUID.fromString(result.getString("author_uuid")), result.getString("author_name"),
                    result.getInt("staff") != 0, result.getString("message"), result.getLong("created_at"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Participant participantFrom(ResultSet result) throws SQLException {
        try {
            return new Participant(result.getString("report_id"), UUID.fromString(result.getString("uuid")),
                    result.getString("name"), result.getInt("staff") != 0, result.getLong("last_read"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /* ------------------------------------------------------------- reads --- */

    public Report find(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Report exact = reports.get(id);
        if (exact != null) {
            return exact;
        }
        if (id.length() < 6) {
            return null;
        }
        return reports.values().stream()
                .filter(report -> report.id().startsWith(id))
                .findFirst().orElse(null);
    }

    /** Open first, then most recently updated. */
    private static Comparator<Report> order() {
        return Comparator.comparing((Report report) -> !report.isOpen())
                .thenComparing(Comparator.comparingLong(Report::updatedAt).reversed());
    }

    public List<Report> all(boolean openOnly) {
        return reports.values().stream()
                .filter(report -> !openOnly || report.isOpen())
                .sorted(order())
                .toList();
    }

    public List<Report> mine(UUID uuid) {
        return reports.values().stream()
                .filter(report -> report.reporterUuid().equals(uuid))
                .sorted(order())
                .toList();
    }

    public List<Message> messages(String reportId) {
        return List.copyOf(messages.getOrDefault(reportId, List.of()));
    }

    public Message lastMessage(String reportId) {
        List<Message> list = messages.get(reportId);
        return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
    }

    public List<Participant> participants(String reportId) {
        return List.copyOf(participants.getOrDefault(reportId, List.of()));
    }

    public int messageCount(String reportId) {
        return messages.getOrDefault(reportId, List.of()).size();
    }

    /** Whether a player already took part in a ticket. */
    public boolean isParticipant(UUID uuid, String reportId) {
        return participants.getOrDefault(reportId, List.of()).stream()
                .anyMatch(participant -> participant.uuid().equals(uuid));
    }

    public int unread(UUID uuid, String reportId) {
        // Only people who actually took part in a ticket track unread messages;
        // every other staff member is told about a new ticket separately.
        Participant participant = participants.getOrDefault(reportId, List.of()).stream()
                .filter(entry -> entry.uuid().equals(uuid))
                .findFirst().orElse(null);
        if (participant == null) {
            return 0;
        }
        long since = participant.lastRead();
        int count = 0;
        for (Message message : messages.getOrDefault(reportId, List.of())) {
            if (message.createdAt() > since && !message.authorUuid().equals(uuid)) {
                count++;
            }
        }
        return count;
    }

    public int totalUnread(UUID uuid) {
        int total = 0;
        for (Report report : reports.values()) {
            total += unread(uuid, report.id());
        }
        return total;
    }

    /* ------------------------------------------------------------ writes --- */

    /** Creates a ticket and its first message, then tells the staff who are online. */
    public Report create(Player reporter, String category, String targetName, String description) {
        long now = System.currentTimeMillis();
        String id = UUID.randomUUID().toString();
        Report report = new Report(id, reporter.getUniqueId(), reporter.getName(),
                targetName == null || targetName.isBlank() ? null : targetName,
                category, OPEN, now, now);
        reports.put(id, report);
        messages.put(id, new CopyOnWriteArrayList<>());

        Participant participant = new Participant(id, reporter.getUniqueId(), reporter.getName(), false, now);
        participants.computeIfAbsent(id, ignored -> new CopyOnWriteArrayList<>()).add(participant);

        Message message = new Message(UUID.randomUUID().toString(), id, reporter.getUniqueId(),
                reporter.getName(), false, description, now);
        messages.get(id).add(message);

        saveReport(report);
        saveParticipant(participant);
        saveMessage(message);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(reporter) && online.hasPermission(STAFF_PERMISSION)) {
                sendNotification(online, report, "&7New &f" + category + " &7ticket from &f" + reporter.getName() + "&7.");
            }
        }
        return report;
    }

    /** Adds a reply and notifies everybody else involved. */
    public Message reply(Player author, String reportId, String text) {
        Report report = reports.get(reportId);
        if (report == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        boolean staff = author.hasPermission(STAFF_PERMISSION);
        Message message = new Message(UUID.randomUUID().toString(), reportId, author.getUniqueId(),
                author.getName(), staff, text, now);
        messages.computeIfAbsent(reportId, ignored -> new CopyOnWriteArrayList<>()).add(message);

        Report updated = new Report(report.id(), report.reporterUuid(), report.reporterName(),
                report.targetName(), report.category(), report.status(), report.createdAt(), now);
        reports.put(reportId, updated);
        saveReport(updated);
        saveMessage(message);

        // The author has obviously read everything up to their own message.
        touch(author, reportId);

        String prefix = staff ? "&7Staff &f" : "&7";
        for (Participant participant : participants(reportId)) {
            if (participant.uuid().equals(author.getUniqueId())) {
                continue;
            }
            Player online = Bukkit.getPlayer(participant.uuid());
            if (online != null) {
                sendNotification(online, updated, prefix + author.getName() + " &7replied.");
            }
        }
        return message;
    }

    /** Marks a ticket as open or closed. */
    public void setStatus(String reportId, String status) {
        Report report = reports.get(reportId);
        if (report == null) {
            return;
        }
        Report updated = new Report(report.id(), report.reporterUuid(), report.reporterName(),
                report.targetName(), report.category(), status, report.createdAt(), System.currentTimeMillis());
        reports.put(reportId, updated);
        saveReport(updated);
    }

    /** Adds a player to the ticket and marks them as caught up. */
    public void touch(Player player, String reportId) {
        Report report = reports.get(reportId);
        if (report == null) {
            return;
        }
        List<Participant> list = participants.computeIfAbsent(reportId, ignored -> new CopyOnWriteArrayList<>());
        Participant existing = list.stream()
                .filter(entry -> entry.uuid().equals(player.getUniqueId()))
                .findFirst().orElse(null);
        Participant updated = new Participant(reportId, player.getUniqueId(), player.getName(),
                player.hasPermission(STAFF_PERMISSION), System.currentTimeMillis());
        if (existing != null) {
            list.remove(existing);
        }
        list.add(updated);
        saveParticipant(updated);
    }

    /** Tells a player about unread tickets when they join. */
    public void notifyOnJoin(Player player) {
        int unread = totalUnread(player.getUniqueId());
        if (unread <= 0) {
            return;
        }
        Component line = Msg.component("&8[&6Reports&8] &7You have &f" + unread
                        + " &7unread report message(s). ")
                .append(Msg.button("&a[open]", "/report", "&7Open your report tickets"));
        player.sendMessage(line);
    }

    private void sendNotification(Player player, Report report, String text) {
        // Respect the /notify switch: staff may turn ticket updates off.
        if (plugin.notifications() != null && !plugin.notifications().enabled(player, "report")) {
            return;
        }
        Component line = Msg.component("&8[&6Reports&8] " + text + " ")
                .append(Msg.button("&a[open]", "/report view " + report.id(),
                        "&7Open ticket &f" + shortId(report.id())));
        player.sendMessage(line);
    }

    public static String shortId(String id) {
        return id == null ? "" : id.substring(0, Math.min(8, id.length()));
    }

    /* --------------------------------------------------------- persistence - */

    private void saveReport(Report report) {
        String sql = "INSERT INTO `" + database.table("reports")
                + "` (`id`, `reporter_uuid`, `reporter_name`, `target_name`, `category`, `status`,"
                + " `created_at`, `updated_at`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE `target_name` = VALUES(`target_name`),"
                + " `category` = VALUES(`category`), `status` = VALUES(`status`),"
                + " `updated_at` = VALUES(`updated_at`)";
        runAsync(sql, statement -> {
            statement.setString(1, report.id());
            statement.setString(2, report.reporterUuid().toString());
            statement.setString(3, report.reporterName());
            statement.setString(4, report.targetName());
            statement.setString(5, report.category());
            statement.setString(6, report.status());
            statement.setLong(7, report.createdAt());
            statement.setLong(8, report.updatedAt());
        });
    }

    private void saveMessage(Message message) {
        String sql = "INSERT INTO `" + database.table("report_messages")
                + "` (`id`, `report_id`, `author_uuid`, `author_name`, `staff`, `message`, `created_at`)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `message` = VALUES(`message`)";
        runAsync(sql, statement -> {
            statement.setString(1, message.id());
            statement.setString(2, message.reportId());
            statement.setString(3, message.authorUuid().toString());
            statement.setString(4, message.authorName());
            statement.setInt(5, message.staff() ? 1 : 0);
            statement.setString(6, message.message());
            statement.setLong(7, message.createdAt());
        });
    }

    private void saveParticipant(Participant participant) {
        String sql = "INSERT INTO `" + database.table("report_participants")
                + "` (`report_id`, `uuid`, `name`, `staff`, `last_read`) VALUES (?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `staff` = VALUES(`staff`),"
                + " `last_read` = VALUES(`last_read`)";
        runAsync(sql, statement -> {
            statement.setString(1, participant.reportId());
            statement.setString(2, participant.uuid().toString());
            statement.setString(3, participant.name());
            statement.setInt(4, participant.staff() ? 1 : 0);
            statement.setLong(5, participant.lastRead());
        });
    }

    @FunctionalInterface
    private interface Bind {
        void apply(PreparedStatement statement) throws SQLException;
    }

    private void runAsync(String sql, Bind bind) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!database.isAvailable()) {
                return;
            }
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        bind.apply(statement);
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not save a report ticket: " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /** Category ids currently used, newest first, for the staff overview. */
    public Set<String> categoriesInUse() {
        Set<String> categories = new HashSet<>();
        for (Report report : reports.values()) {
            categories.add(report.category().toLowerCase(Locale.ROOT));
        }
        return categories;
    }

    /** Counts per status, used by the menu labels. */
    public Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(OPEN, 0);
        counts.put(CLOSED, 0);
        for (Report report : reports.values()) {
            counts.merge(report.status(), 1, Integer::sum);
        }
        return counts;
    }

    /** Free-text search across reporter, target and category, used by the staff menu. */
    public List<Report> search(String query, boolean openOnly) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<Report> matches = new ArrayList<>();
        for (Report report : all(openOnly)) {
            if (report.reporterName().toLowerCase(Locale.ROOT).contains(needle)
                    || (report.targetName() != null && report.targetName().toLowerCase(Locale.ROOT).contains(needle))
                    || report.category().toLowerCase(Locale.ROOT).contains(needle)
                    || shortId(report.id()).contains(needle)) {
                matches.add(report);
            }
        }
        return matches;
    }
}
