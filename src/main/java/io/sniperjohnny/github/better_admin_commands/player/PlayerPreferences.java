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

public class PlayerPreferences {

    public static final String NICKNAME = "nickname";
    public static final String NICK_GROUP = "nick_group";

    public static final String PREFIX_NONE = "-";

    private static final String NAME_TAG_TEAM_PREFIX = "bacn";

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

    public String nickname(UUID uuid) {
        return settings(uuid).get(NICKNAME);
    }

    public String plainNickname(UUID uuid) {
        String nickname = nickname(uuid);
        return nickname == null ? null : stripColor(nickname);
    }

    public static String stripColor(String value) {
        return value == null ? null : value.replaceAll("(?i)[&\u00A7][0-9a-fk-or]", "");
    }

    public String nicknameGroup(UUID uuid) {
        return settings(uuid).get(NICK_GROUP);
    }

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

    public String nicknameDisplay(UUID uuid) {
        String nickname = nickname(uuid);
        return nickname == null ? null : nicknamePrefix(uuid) + nickname;
    }

    public String displayName(UUID uuid, String realName) {
        String nickname = nickname(uuid);
        String base = nickname == null ? (realName == null ? "" : realName) : nickname;
        return nicknamePrefix(uuid) + base;
    }

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

    public Component tabName(UUID uuid, String realName) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(displayName(uuid, realName));
    }

    public boolean hideNameTags() {
        return plugin.getConfig().getBoolean("nick.hide-nametag", true);
    }

    public boolean setNameTagsHidden(boolean hidden) {
        plugin.getConfig().set("nick.hide-nametag", hidden);
        plugin.saveConfig();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            applyNickname(online);
        }
        return hidden;
    }

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

    public void refreshNameTags(Player joiner) {
        ScoreboardManager manager = plugin.getServer().getScoreboardManager();
        if (manager == null || joiner.getScoreboard() == manager.getMainScoreboard()) {
            return; // everyone shares the main scoreboard, its teams are in place already
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            updateNameTag(online);
        }
    }

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

    public void removeNameTagTeams() {
        for (Scoreboard board : scoreboards()) {
            for (Team team : new ArrayList<>(board.getTeams())) {
                if (team.getName().startsWith(NAME_TAG_TEAM_PREFIX)) {
                    team.unregister();
                }
            }
        }
    }

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

    private static String nameTagTeam(UUID uuid) {
        return NAME_TAG_TEAM_PREFIX + uuid.toString().replace("-", "").substring(0, 12);
    }

    private Component vanishCue() {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(vanishCueText());
    }

    private String vanishCueText() {
        return plugin.getConfig().getString("moderation.vanish-tab-cue", "&7[&8V&7] &r");
    }

    public void setNickname(Player player, String nickname) {
        setNickname(player, nickname, nicknameGroup(player.getUniqueId()));
    }

    public void setNickname(Player player, String nickname, String group) {
        set(player.getUniqueId(), NICKNAME, nickname);
        set(player.getUniqueId(), NICK_GROUP, group);
        applyNickname(player);
    }

    public boolean notificationEnabled(UUID uuid, String category, boolean fallback) {
        String value = settings(uuid).get("notify." + category);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public void setNotification(UUID uuid, String category, Boolean value) {
        set(uuid, "notify." + category, value == null ? null : Boolean.toString(value));
    }

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

    public Map<String, String> allEntries(UUID uuid) {
        return Map.copyOf(settings(uuid));
    }

    public Map<UUID, Map<String, String>> all() {
        Map<UUID, Map<String, String>> copy = new LinkedHashMap<>();
        cache.forEach((uuid, map) -> copy.put(uuid, Map.copyOf(map)));
        return copy;
    }

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
