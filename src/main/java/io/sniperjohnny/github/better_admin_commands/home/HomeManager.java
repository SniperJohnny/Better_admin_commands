package io.sniperjohnny.github.better_admin_commands.home;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player homes. Homes live in the {@code homes} table so they survive server
 * restarts and are available on every server sharing the database.
 */
public class HomeManager {

    private final Better_Admin_Commands plugin;
    private final Database database;
    private final LocalStore local;
    private final Map<UUID, Map<String, Location>> homes = new ConcurrentHashMap<>();

    public HomeManager(Better_Admin_Commands plugin, Database database, LocalStore local) {
        this.plugin = plugin;
        this.database = database;
        this.local = local;
    }

    /**
     * Reads every home into memory. Called once at start-up. Falls back to the
     * local safe file while MySQL is unavailable.
     */
    public void loadAll() throws SQLException {
        homes.clear();
        if (database.isAvailable()) {
            try {
                loadFromDatabase();
                return;
            } catch (SQLException e) {
                database.markUnavailable();
                plugin.getLogger().warning("Could not read homes from MySQL, using the local safe file: "
                        + e.getMessage());
            }
        }
        loadFromLocal();
    }

    private void loadFromDatabase() throws SQLException {
        Map<String, Map<String, Object>> localRows = new LinkedHashMap<>();
        database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `uuid`, `home`, `world`, `x`, `y`, `z`, `yaw`, `pitch` FROM `"
                            + database.table("homes") + "`");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(result.getString("uuid"));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    String name = result.getString("home");
                    World world = Bukkit.getWorld(result.getString("world"));
                    if (world == null) {
                        continue;
                    }
                    Location location = new Location(world,
                            result.getDouble("x"), result.getDouble("y"), result.getDouble("z"),
                            result.getFloat("yaw"), result.getFloat("pitch"));
                    homes.computeIfAbsent(uuid, key -> new ConcurrentHashMap<>())
                            .put(name.toLowerCase(Locale.ROOT), location);
                    localRows.put(LocalStore.composite(uuid, name), homeRow(uuid, name, location));
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
            String name = LocalStore.string(row, "home");
            World world = Bukkit.getWorld(String.valueOf(LocalStore.string(row, "world")));
            if (name == null || world == null) {
                continue;
            }
            Location location = new Location(world,
                    LocalStore.doubleValue(row, "x", 0.0),
                    LocalStore.doubleValue(row, "y", 0.0),
                    LocalStore.doubleValue(row, "z", 0.0),
                    (float) LocalStore.doubleValue(row, "yaw", 0.0),
                    (float) LocalStore.doubleValue(row, "pitch", 0.0));
            homes.computeIfAbsent(uuid, key -> new ConcurrentHashMap<>())
                    .put(name.toLowerCase(Locale.ROOT), location);
        }
        plugin.getLogger().info("Loaded " + homes.size() + " player home list(s) from the local safe file.");
    }

    private static Map<String, Object> homeRow(UUID uuid, String name, Location location) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("uuid", uuid.toString());
        row.put("home", name);
        row.put("world", location.getWorld().getName());
        row.put("x", location.getX());
        row.put("y", location.getY());
        row.put("z", location.getZ());
        row.put("yaw", location.getYaw());
        row.put("pitch", location.getPitch());
        return row;
    }

    /** All homes of a player, sorted alphabetically. Never {@code null}. */
    public Map<String, Location> homesOf(UUID uuid) {
        Map<String, Location> stored = homes.get(uuid);
        if (stored == null || stored.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new TreeMap<>(stored));
    }

    public Location get(UUID uuid, String name) {
        Map<String, Location> stored = homes.get(uuid);
        if (stored == null) {
            return null;
        }
        Location location = stored.get(name.toLowerCase(Locale.ROOT));
        return location == null ? null : location.clone();
    }

    public boolean exists(UUID uuid, String name) {
        Map<String, Location> stored = homes.get(uuid);
        return stored != null && stored.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /** Maximum number of homes a player may own, based on permissions then config. */
    public int limitFor(Player player) {
        int limit = plugin.getConfig().getInt("homes.max", 3);
        for (int candidate = 100; candidate >= 1; candidate--) {
            if (player.hasPermission("betteradmincommands.homes.limit." + candidate)) {
                return Math.max(limit, candidate);
            }
        }
        return limit;
    }

    /**
     * Stores a home. Returns {@code false} when the player already reached
     * their home limit and the home does not exist yet.
     */
    public boolean set(Player player, String name, Location location) {
        UUID uuid = player.getUniqueId();
        String key = name.toLowerCase(Locale.ROOT);
        Map<String, Location> stored = homes.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>());
        boolean existing = stored.containsKey(key);
        if (!existing && stored.size() >= limitFor(player)) {
            return false;
        }
        stored.put(key, location.clone());
        saveAsync(uuid, key, location);
        return true;
    }

    public boolean delete(UUID uuid, String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Map<String, Location> stored = homes.get(uuid);
        if (stored == null || stored.remove(key) == null) {
            return false;
        }
        deleteAsync(uuid, key);
        return true;
    }

    private void saveAsync(UUID uuid, String key, Location location) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Always mirror into the local safe file first.
            local.merge(LocalStore.composite(uuid, key), homeRow(uuid, key, location));
            if (!database.isAvailable()) {
                return;
            }
            String sql = "INSERT INTO `" + database.table("homes") + "`"
                    + " (`uuid`, `home`, `world`, `x`, `y`, `z`, `yaw`, `pitch`)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE `world` = VALUES(`world`), `x` = VALUES(`x`),"
                    + " `y` = VALUES(`y`), `z` = VALUES(`z`), `yaw` = VALUES(`yaw`), `pitch` = VALUES(`pitch`)";
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, key);
                        statement.setString(3, location.getWorld().getName());
                        statement.setDouble(4, location.getX());
                        statement.setDouble(5, location.getY());
                        statement.setDouble(6, location.getZ());
                        statement.setFloat(7, location.getYaw());
                        statement.setFloat(8, location.getPitch());
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not save home '" + key + "': " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    private void deleteAsync(UUID uuid, String key) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Always mirror the deletion into the local safe file first.
            local.delete(LocalStore.composite(uuid, key));
            if (!database.isAvailable()) {
                return;
            }
            try {
                database.withConnection(connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM `" + database.table("homes") + "` WHERE `uuid` = ? AND `home` = ?")) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, key);
                        statement.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not delete home '" + key + "': " + e.getMessage());
                database.markUnavailable();
            }
        });
    }

    /**
     * Replaces the homes table with the local safe file after a reconnect. The
     * safe file is a complete mirror, so this also propagates deletions made
     * while the database was down.
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
                            "DELETE FROM `" + database.table("homes") + "` WHERE `uuid` = ?")) {
                        for (String owner : owners) {
                            delete.setString(1, owner);
                            delete.addBatch();
                        }
                        delete.executeBatch();
                    }
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO `" + database.table("homes") + "`"
                                    + " (`uuid`, `home`, `world`, `x`, `y`, `z`, `yaw`, `pitch`)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        for (Map<String, Object> row : rows) {
                            String uuid = LocalStore.string(row, "uuid");
                            String name = LocalStore.string(row, "home");
                            if (uuid == null || name == null) {
                                continue;
                            }
                            insert.setString(1, uuid);
                            insert.setString(2, name);
                            insert.setString(3, LocalStore.string(row, "world"));
                            insert.setDouble(4, LocalStore.doubleValue(row, "x", 0.0));
                            insert.setDouble(5, LocalStore.doubleValue(row, "y", 0.0));
                            insert.setDouble(6, LocalStore.doubleValue(row, "z", 0.0));
                            insert.setFloat(7, (float) LocalStore.doubleValue(row, "yaw", 0.0));
                            insert.setFloat(8, (float) LocalStore.doubleValue(row, "pitch", 0.0));
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
            plugin.getLogger().severe("Could not sync homes back to MySQL: " + e.getMessage());
            database.markUnavailable();
        }
    }

    /** Raw, unmodifiable view used for debug output. */
    public Map<UUID, Map<String, Location>> all() {
        Map<UUID, Map<String, Location>> copy = new LinkedHashMap<>();
        homes.forEach((uuid, map) -> copy.put(uuid, Collections.unmodifiableMap(new TreeMap<>(map))));
        return Collections.unmodifiableMap(copy);
    }
}
