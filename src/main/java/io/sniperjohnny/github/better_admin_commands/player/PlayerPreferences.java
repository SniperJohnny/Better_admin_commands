package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
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
    public static final String SOCIAL_SPY = "socialspy";
    public static final String TELEPORT_TOGGLE = "tptoggle";
    public static final String IGNORE = "ignore";
    public static final String AFK = "afk";

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final Map<UUID, Map<String, String>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastReplyTarget = new ConcurrentHashMap<>();

    public PlayerPreferences(Better_Admin_Commands plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /** Loads all settings of a player. Called synchronously when they join. */
    public void load(Player player) {
        Map<String, String> settings = new ConcurrentHashMap<>();
        try {
            database.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT `setting_key`, `setting_value` FROM `" + database.table("player_settings")
                                + "` WHERE `uuid` = ?")) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            settings.put(result.getString("setting_key"), result.getString("setting_value"));
                        }
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load settings for " + player.getName() + ": " + e.getMessage());
        }
        cache.put(player.getUniqueId(), settings);
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

    /** Applies the stored nickname to the display name and the tab list. */
    public void applyNickname(Player player) {
        String nickname = nickname(player.getUniqueId());
        Component component = nickname == null
                ? Component.text(player.getName())
                : LegacyComponentSerializer.legacyAmpersand().deserialize(nickname);
        player.displayName(component);
        player.playerListName(component);
    }

    public void setNickname(Player player, String nickname) {
        if (nickname == null) {
            set(player.getUniqueId(), NICKNAME, null);
        } else {
            set(player.getUniqueId(), NICKNAME, nickname);
        }
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

    /** Settings of players who are not online, read straight from the database. */
    public List<Map<String, Object>> readAllFromDatabase() throws SQLException {
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

    private void persistAsync(UUID uuid, String key, String value) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
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
            }
        });
    }
}
