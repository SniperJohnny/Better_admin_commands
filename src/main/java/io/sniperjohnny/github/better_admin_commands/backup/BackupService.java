package io.sniperjohnny.github.better_admin_commands.backup;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Dumps every table the plugin owns into human readable YAML files inside
 * {@code plugins/Better_Admin_Commands/backups/<timestamp>/}.
 *
 * <p>The dump is generic: it reads the column labels from the result set, so a
 * table that gains a column is backed up correctly without code changes.</p>
 */
public class BackupService {

    /** Result of a backup run. */
    public record BackupResult(File folder, Map<String, Integer> rowCounts) {

        public int totalRows() {
            return rowCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final LocalStore localPlayers;
    private final LocalStore localHomes;
    private final LocalStore localSettings;
    private final LocalStore localMail;

    public BackupService(Better_Admin_Commands plugin, Database database, LocalStore localPlayers,
                         LocalStore localHomes, LocalStore localSettings, LocalStore localMail) {
        this.plugin = plugin;
        this.database = database;
        this.localPlayers = localPlayers;
        this.localHomes = localHomes;
        this.localSettings = localSettings;
        this.localMail = localMail;
    }

    /** Runs a full backup. Blocking - call from an async task. */
    public BackupResult backup() throws SQLException, IOException {
        File root = new File(plugin.getDataFolder(), "backups");
        File folder = new File(root, LocalDateTime.now().format(FOLDER_FORMAT));
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create the backup folder " + folder.getAbsolutePath());
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("players", dumpTable("players", new File(folder, "players.yml")));
        counts.put("homes", dumpTable("homes", new File(folder, "homes.yml")));
        counts.put("player_settings", dumpTable("player_settings", new File(folder, "player_settings.yml")));
        counts.put("mail", dumpTable("mail", new File(folder, "mail.yml")));

        YamlConfiguration meta = new YamlConfiguration();
        meta.set("created-at", System.currentTimeMillis());
        meta.set("created-at-readable", LocalDateTime.now().toString());
        meta.set("server", plugin.getServer().getName() + " " + plugin.getServer().getVersion());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            meta.set("tables." + entry.getKey(), entry.getValue());
        }
        meta.save(new File(folder, "meta.yml"));

        return new BackupResult(folder, counts);
    }

    /**
     * Reads a whole table into a YAML file and returns the row count. Falls
     * back to the local safe file when the database is unavailable.
     */
    public int dumpTable(String shortName, File target) throws SQLException, IOException {
        String table = database.table(shortName);
        List<Map<String, Object>> rows;
        if (database.isAvailable()) {
            try {
                rows = database.withConnection(connection -> {
                    List<Map<String, Object>> result = new ArrayList<>();
                    try (Statement statement = connection.createStatement();
                         ResultSet resultSet = statement.executeQuery("SELECT * FROM `" + table + "`")) {
                        int columns = resultSet.getMetaData().getColumnCount();
                        while (resultSet.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int index = 1; index <= columns; index++) {
                                Object value = resultSet.getObject(index);
                                row.put(resultSet.getMetaData().getColumnLabel(index),
                                        value instanceof byte[] bytes ? new String(bytes) : value);
                            }
                            result.add(row);
                        }
                    }
                    return result;
                });
            } catch (SQLException e) {
                database.markUnavailable();
                rows = localRows(shortName).get();
            }
        } else {
            rows = localRows(shortName).get();
        }

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("table", table);
        yaml.set("row-count", rows.size());
        yaml.set("rows", rows);

        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        yaml.save(target);
        return rows.size();
    }

    private Supplier<List<Map<String, Object>>> localRows(String shortName) {
        return switch (shortName) {
            case "players" -> localPlayers::snapshot;
            case "homes" -> localHomes::snapshot;
            case "player_settings" -> localSettings::snapshot;
            case "mail" -> localMail::snapshot;
            default -> List::of;
        };
    }
}
