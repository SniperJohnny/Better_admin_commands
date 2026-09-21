package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per player settings that do not deserve their own column: nickname,
 * social spy, teleport requests toggle, ignore list and the afk flag.
 *
 * They live in the {@code player_settings} key/value table so new settings can
 * be added without a database migration.
 */
public class PlayerPreferences {

    public static final String NICKNAME = "nickname";
    public static final String NICK_GROUP = "nick_group";
    /** Packed {@code value|signature|source} of a skin borrowed with /skinchange. */
    public static final String SKIN = "skin";
    public static final String SOCIAL_SPY = "socialspy";
    public static final String TELEPORT_TOGGLE = "tptoggle";
    public static final String IGNORE = "ignore";
    public static final String AFK = "afk";

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final LocalStore local;
    private final Map<UUID, Map<String, String>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastReplyTarget = new ConcurrentHashMap<>();

    public PlayerPreferences(Better_Admin_Commands plugin, Database database, LocalStore local) {
        this.plugin = plugin;
        this.database = database;
        this.local = local;
    }

    /**
     * Loads all settings of a player. Called synchronously when they join. Uses
     * the local safe file while MySQL is unavailable and refreshes the safe file
     * when the database is the source.
     */
    public void load(Player player) {
        Map<String, String> settings = new ConcurrentHashMap<>();
        if (database.isAvailable()) {
            Map<String, Map<String, Object>> localRows = new LinkedHashMap<>();
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT `setting_key`, `setting_value` FROM `" + database.table("player_settings")
                                    + "` WHERE `uuid` = ?")) {
                        statement.setString(1, player.getUniqueId().toString());
                        try (ResultSet result = statement.executeQuery()) {
                            while (result.next()) {
                                String key = result.getString("setting_key");
                                String value = result.getString("setting_value");
                                settings.put(key, value);
                                localRows.put(LocalStore.composite(player.getUniqueId(), key),
                                        settingRow(player.getUniqueId(), key, value));
                            }
                        }
                    }
                    return null;
                });
                local.mergeAll(localRows);
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not load settings for " + player.getName() + ": "
                        + e.getMessage());
                database.markUnavailable();
                loadFromLocal(player.getUniqueId(), settings);
            }
        } else {
            loadFromLocal(player.getUniqueId(), settings);
        }
        cache.put(player.getUniqueId(), settings);
    }

    private void loadFromLocal(UUID uuid, Map<String, String> settings) {
        for (Map<String, Object> row : local.snapshot()) {
            String owner = LocalStore.string(row, "uuid");
            if (!uuid.toString().equals(owner)) {
                continue;
            }
            String key = LocalStore.string(row, "setting_key");
            if (key != null) {
                settings.put(key, LocalStore.string(row, "setting_value"));
            }
        }
    }

    private static Map<String, Object> settingRow(UUID uuid, String key, String value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("uuid", uuid.toString());
        row.put("setting_key", key);
        row.put("setting_value", value);
        return row;
    }

    /** Removes the cached settings when a player leaves. */
    public void unload(UUID uuid) {
        cache.remove(uuid);
        lastReplyTarget.remove(uuid);
    }

    private Map<String, String> settings(UUID uuid) {
        return cache.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>());
    }

    public String get(UUID uuid, String key, String fallback) {
        String value = settings(uuid).get(key);
        return value == null ? fallback : value;
    }

    public boolean getBoolean(UUID uuid, String key, boolean fallback) {
        String value = settings(uuid).get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public void set(UUID uuid, String key, String value) {
        if (value == null) {
            settings(uuid).remove(key);
            persistAsync(uuid, key, null);
        } else {
            settings(uuid).put(key, value);
            persistAsync(uuid, key, value);
        }
    }

    public void setBoolean(UUID uuid, String key, boolean value) {
        set(uuid, key, Boolean.toString(value));
    }

    /* --------------------------------------------------------- nickname --- */

    public String nickname(UUID uuid) {
        return settings(uuid).get(NICKNAME);
    }

    /** The LuckPerms group whose prefix is shown in front of the nickname. */
    public String nicknameGroup(UUID uuid) {
        return settings(uuid).get(NICK_GROUP);
    }

    /**
     * Applies the stored nickname and its group prefix. The result is used for
     * the tab list, the name tag above the player and - through the display
     * name - for chat messages on a vanilla/Paper chat.
     *
     * <p>A vanished player additionally gets the configured cue in the tab list,
     * so the staff who are allowed to see them can tell them apart.</p>
     */
    public void applyNickname(Player player) {
        String nickname = nickname(player.getUniqueId());
        Component name = nickname == null
                ? Component.text(player.getName())
                : LegacyComponentSerializer.legacyAmpersand().deserialize(nickname);
        Component shown = prefixOf(nicknameGroup(player.getUniqueId())).append(name);
        player.displayName(shown);
        player.playerListName(plugin.vanish().isVanished(player) ? vanishCue().append(shown) : shown);
    }

    /** The tab list marker for vanished players, configured under moderation. */
    private Component vanishCue() {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getConfig().getString("moderation.vanish-tab-cue", "&7[&8V&7] &r"));
    }

    /** The borrowed LuckPerms group prefix, never {@code null}. */
    private Component prefixOf(String group) {
        if (group == null) {
            return Component.empty();
        }
        String prefix = plugin.nicks().prefix(group);
        return prefix == null
                ? Component.empty()
                : LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
    }

    /** Changes the nickname, keeping the group prefix that is already set. */
    public void setNickname(Player player, String nickname) {
        setNickname(player, nickname, nicknameGroup(player.getUniqueId()));
    }

    /**
     * Sets the nickname and the LuckPerms group whose prefix is shown with it.
     * A {@code null} nickname or group removes that part.
     */
    public void setNickname(Player player, String nickname, String group) {
        set(player.getUniqueId(), NICKNAME, nickname);
        set(player.getUniqueId(), NICK_GROUP, group);
        applyNickname(player);
    }

    /* ----------------------------------------------------------- ignore --- */

    public Set<String> ignored(UUID uuid) {
        String raw = settings(uuid).get(IGNORE);
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> names = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            if (!part.isBlank()) {
                names.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    public boolean isIgnoring(UUID uuid, String name) {
        return ignored(uuid).contains(name.toLowerCase(Locale.ROOT));
    }

    /** Toggles a name on the ignore list, returning the new state. */
    public boolean toggleIgnore(UUID uuid, String name) {
        Set<String> names = new LinkedHashSet<>(ignored(uuid));
        String key = name.toLowerCase(Locale.ROOT);
        boolean nowIgnored;
        if (names.contains(key)) {
            names.remove(key);
            nowIgnored = false;
        } else {
            names.add(key);
            nowIgnored = true;
        }
        set(uuid, IGNORE, names.isEmpty() ? null : String.join(",", names));
        return nowIgnored;
    }

    /* -------------------------------------------------------- last reply --- */

    public void setLastReplyTarget(UUID uuid, UUID target) {
        if (target == null) {
            lastReplyTarget.remove(uuid);
        } else {
            lastReplyTarget.put(uuid, target.toString());
        }
    }

    public UUID lastReplyTarget(UUID uuid) {
        String raw = lastReplyTarget.get(uuid);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /* ------------------------------------------------------------ backup --- */

    /** Every key of a single player, used by features with dynamic keys. */
    public Map<String, String> allEntries(UUID uuid) {
        return Map.copyOf(settings(uuid));
    }

    /** Every loaded setting, used by the backup command. */
    public Map<UUID, Map<String, String>> all() {
        Map<UUID, Map<String, String>> copy = new LinkedHashMap<>();
        cache.forEach((uuid, map) -> copy.put(uuid, Map.copyOf(map)));
        return copy;
    }

    /** Settings of players who are not online, read from the database or the local safe file. */
    public List<Map<String, Object>> readAllFromDatabase() throws SQLException {
        if (!database.isAvailable()) {
            return local.snapshot();
        }
        return database.withConnection(connection -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM `" + database.table("player_settings") + "`");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("uuid", result.getString("uuid"));
                    row.put("setting_key", result.getString("setting_key"));
                    row.put("setting_value", result.getString("setting_value"));
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /**
     * Replaces the settings table with the local safe file after a reconnect.
     * The safe file is a complete mirror, so deletions propagate too.
     */
    public void resyncToDatabase() {
        if (!database.isAvailable()) {
            return;
        }
        List<Map<String, Object>> rows = local.snapshot();
        Set<String> owners = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String uuid = LocalStore.string(row, "uuid");
            if (uuid != null) {
                owners.add(uuid);
            }
        }
        try {
            database.withConnection(connection -> {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM `" + database.table("player_settings") + "` WHERE `uuid` = ?")) {
                        for (String owner : owners) {
                            delete.setString(1, owner);
                            delete.addBatch();
                        }
                        delete.executeBatch();
                    }
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO `" + database.table("player_settings")
                                    + "` (`uuid`, `setting_key`, `setting_value`) VALUES (?, ?, ?)")) {
                        for (Map<String, Object> row : rows) {
                            String uuid = LocalStore.string(row, "uuid");
                            String key = LocalStore.string(row, "setting_key");
                            if (uuid == null || key == null) {
                                continue;
                            }
                            insert.setString(1, uuid);
                            insert.setString(2, key);
                            insert.setString(3, LocalStore.string(row, "setting_value"));
                            insert.addBatch();
                        }
                        insert.executeBatch();
                    }
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(autoCommit);
                }
                return null;
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not sync settings back to MySQL: " + e.getMessage());
            database.markUnavailable();
        }
    }

    private void persistAsync(UUID uuid, String key, String value) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Always mirror into the local safe file first.
            String composite = LocalStore.composite(uuid, key);
            if (value == null) {
                local.delete(composite);
            } else {
                local.merge(composite, settingRow(uuid, key, value));
            }
            if (!database.isAvailable()) {
                return;
            }
            try {
                if (value == null) {
                    database.withConnection(connection -> {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "DELETE FROM `" + database.table("player_settings")
                                        + "` WHERE `uuid` = ? AND `setting_key` = ?")) {
                            statement.setString(1, uuid.toString());
                            statement.setString(2, key);
                            statement.executeUpdate();
                        }
                        return null;
                    });
                    return;
                }
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO `" + database.table("player_settings")
                                    + "` (`uuid`, `setting_key`, `setting_value`) VALUES (?, ?, ?)"
                                    + " ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`)")) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, key);
                        statement.setString(3, value);
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not save setting '" + key + "': " + e.getMessage());
                database.markUnavailable();
            }
        });
    }
}
