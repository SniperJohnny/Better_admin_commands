package io.sniperjohnny.github.better_admin_commands.home;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
    private final Map<UUID, Map<String, Location>> homes = new ConcurrentHashMap<>();

    public HomeManager(Better_Admin_Commands plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /** Reads every home into memory. Called once at start-up. */
    public void loadAll() throws SQLException {
        homes.clear();
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
                    World world = Bukkit.getWorld(result.getString("world"));
                    if (world == null) {
                        continue;
                    }
                    Location location = new Location(world,
                            result.getDouble("x"), result.getDouble("y"), result.getDouble("z"),
                            result.getFloat("yaw"), result.getFloat("pitch"));
                    homes.computeIfAbsent(uuid, key -> new ConcurrentHashMap<>())
                            .put(result.getString("home").toLowerCase(Locale.ROOT), location);
                }
            }
            return null;
        });
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
            }
        });
    }

    private void deleteAsync(UUID uuid, String key) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
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
            }
        });
    }

    /** Raw, unmodifiable view used for debug output. */
    public Map<UUID, Map<String, Location>> all() {
        Map<UUID, Map<String, Location>> copy = new LinkedHashMap<>();
        homes.forEach((uuid, map) -> copy.put(uuid, Collections.unmodifiableMap(new TreeMap<>(map))));
        return Collections.unmodifiableMap(copy);
    }
}
