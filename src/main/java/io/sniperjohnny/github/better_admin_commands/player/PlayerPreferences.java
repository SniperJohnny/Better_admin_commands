package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

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
    /** Stored as the nick prefix when a player asked for no prefix at all. */
    public static final String PREFIX_NONE = "-";
    /** Start of every scoreboard team created to hide a player's name tag. */
    private static final String NAME_TAG_TEAM_PREFIX = "bacn";
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
        // The player's name tag team is only needed while they are on the server.
        removeNameTagTeam(uuid);
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

    /**
     * The LuckPerms group whose prefix is shown in front of the nickname.
     * {@code null} means "use the player's own rank", {@link #PREFIX_NONE} means
     * "no prefix at all".
     */
    public String nicknameGroup(UUID uuid) {
        return settings(uuid).get(NICK_GROUP);
    }

    /**
     * The nickname prefix as a legacy string, never {@code null}.
     *
     * <p>When no group is stored the player's own rank prefix is used, so a worn
     * rank shows up with the nickname without anyone naming a group. A stored
     * {@link #PREFIX_NONE} suppresses the prefix entirely.</p>
     */
    public String nicknamePrefix(UUID uuid) {
        String group = nicknameGroup(uuid);
        if (PREFIX_NONE.equals(group)) {
            return "";
        }
        if (group == null || group.isBlank()) {
            String own = plugin.nicks().userPrefix(uuid);
            return own == null ? "" : own;
        }
        String prefix = plugin.nicks().prefix(group);
        return prefix == null ? "" : prefix;
    }

    /**
     * The nickname with its group prefix as a legacy string, or {@code null}
     * when no nickname is set. Used to hand the nickname to placeholders such as
     * the ones a tab list plugin reads.
     */
    public String nicknameDisplay(UUID uuid) {
        String nickname = nickname(uuid);
        return nickname == null ? null : nicknamePrefix(uuid) + nickname;
    }

    /**
     * The name a player is shown with everywhere in the plugin: the rank prefix
     * (the player's own rank, or the group borrowed with {@code /nick}) followed
     * by the nickname, or the real name when no nickname is set.
     *
     * <p>Returns a legacy string, so it can be dropped into the existing
     * {@code &}-code messages.</p>
     */
    public String displayName(UUID uuid, String realName) {
        String nickname = nickname(uuid);
        String base = nickname == null ? (realName == null ? "" : realName) : nickname;
        return nicknamePrefix(uuid) + base;
    }

    /**
     * Applies the stored nickname and its rank prefix.
     *
     * <p>With TAB installed the tab list name, the borrowed rank and the hidden
     * name tag are handed to TAB. Without it the display name and the tab list
     * entry are set directly, and the name tags are hidden with scoreboard
     * teams. A vanished player additionally gets the configured cue in the tab
     * list, so the staff who are allowed to see them can tell them apart.</p>
     */
    public void applyNickname(Player player) {
        Component shown = tabName(player.getUniqueId(), player.getName());
        player.displayName(shown);
        boolean vanished = plugin.vanish().isVanished(player);
        player.playerListName(vanished ? vanishCue().append(shown) : shown);
        // TAB owns the tab list and the name tags when it is installed, so hand
        // it the nickname, the rank prefix and the vanish cue as well instead of
        // fighting over the same packets.
        plugin.tabs().apply(player.getUniqueId(), nickname(player.getUniqueId()),
                nicknameGroup(player.getUniqueId()), vanished ? vanishCueText() : "");
        // The name tag above a head is hidden for every player - not only for a
        // nicked one - so the tab list is the only place a name shows up.
        updateNameTag(player);
    }

    /**
     * The name shown for a player in the tab list and above their head. It is the
     * rank prefix plus the nickname, or the rank prefix plus the real name when
     * no nickname is set - so a rank stays visible even without a nickname.
     */
    public Component tabName(UUID uuid, String realName) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(displayName(uuid, realName));
    }

    /** Whether the name tag above players' heads is hidden (config: {@code nick.hide-nametag}). */
    public boolean hideNameTags() {
        return plugin.getConfig().getBoolean("nick.hide-nametag", true);
    }

    /**
     * Hides the name tag above a player's head, by putting them into a scoreboard
     * team whose name tag visibility is off.
     *
     * <p>This is the fallback for a server without TAB, or with TAB's name tag
     * feature switched off - then TAB hides the tag itself. The tag is hidden for
     * <em>every</em> player, nick or not, so the tab list is the only place a
     * name shows. {@code nick.hide-nametag} (on by default) turns this off for
     * servers where another plugin relies on the scoreboard teams; the team this
     * plugin created is then removed again.</p>
     */
    private void updateNameTag(Player player) {
        boolean hide = hideNameTags() && !plugin.tabs().nameTagsAvailable();
        String teamName = nameTagTeam(player.getUniqueId());
        // The team has to exist on every scoreboard a player uses: a scoreboard
        // plugin may hand out a board per player, and a name tag is only hidden
        // for the viewers whose board knows the team.
        for (Scoreboard board : scoreboards()) {
            Team team = board.getTeam(teamName);
            if (!hide) {
                if (team != null) {
                    team.unregister();
                }
                continue;
            }
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
    }

    /**
     * Puts every player back into their hidden name tag team. Called when someone
     * joins, because a scoreboard plugin may have handed them a scoreboard of
     * their own that knows nothing about the teams yet.
     */
    public void refreshNameTags(Player joiner) {
        ScoreboardManager manager = plugin.getServer().getScoreboardManager();
        if (manager == null || joiner.getScoreboard() == manager.getMainScoreboard()) {
            return; // everyone shares the main scoreboard, its teams are in place already
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            updateNameTag(online);
        }
    }

    /** Every scoreboard a name tag team has to exist on. */
    private Set<Scoreboard> scoreboards() {
        Set<Scoreboard> boards = new LinkedHashSet<>();
        ScoreboardManager manager = plugin.getServer().getScoreboardManager();
        if (manager == null) {
            return boards;
        }
        boards.add(manager.getMainScoreboard());
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            boards.add(viewer.getScoreboard());
        }
        return boards;
    }

    /**
     * Removes every name tag team this plugin created, so the name tags come back
     * when the plugin is switched off or unloaded.
     */
    public void removeNameTagTeams() {
        for (Scoreboard board : scoreboards()) {
            for (Team team : new ArrayList<>(board.getTeams())) {
                if (team.getName().startsWith(NAME_TAG_TEAM_PREFIX)) {
                    team.unregister();
                }
            }
        }
    }

    /** Removes the name tag team of one player, used when they leave. */
    private void removeNameTagTeam(UUID uuid) {
        if (plugin.getServer().getScoreboardManager() == null) {
            return;
        }
        String teamName = nameTagTeam(uuid);
        for (Scoreboard board : scoreboards()) {
            Team team = board.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
    }

    /** A scoreboard team name that fits the 16 character limit. */
    private static String nameTagTeam(UUID uuid) {
        return NAME_TAG_TEAM_PREFIX + uuid.toString().replace("-", "").substring(0, 12);
    }

    /** The tab list marker for vanished players, configured under moderation. */
    private Component vanishCue() {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(vanishCueText());
    }

    /** The same marker as a legacy string, for TAB's string-based API. */
    private String vanishCueText() {
        return plugin.getConfig().getString("moderation.vanish-tab-cue", "&7[&8V&7] &r");
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

    /* ----------------------------------------------------- notifications --- */

    /**
     * Whether a player left a notification switched on. An unset value falls
     * back to the configured default, so a new notification is on until someone
     * turns it off.
     */
    public boolean notificationEnabled(UUID uuid, String category, boolean fallback) {
        String value = settings(uuid).get("notify." + category);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    /** Stores a notification toggle; {@code null} restores the configured default. */
    public void setNotification(UUID uuid, String category, Boolean value) {
        set(uuid, "notify." + category, value == null ? null : Boolean.toString(value));
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
