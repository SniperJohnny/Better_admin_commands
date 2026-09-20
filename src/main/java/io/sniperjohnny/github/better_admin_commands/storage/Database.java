package io.sniperjohnny.github.better_admin_commands.storage;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A deliberately small JDBC connection pool around the MySQL/MariaDB driver.
 * The pool is enough for a Minecraft server where all queries are short.
 */
public class Database {

    /** Functional interface for work that needs a pooled connection. */
    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    private final Better_Admin_Commands plugin;
    private final String url;
    private final String user;
    private final String password;
    private final int poolSize;
    private final String tablePrefix;

    private final Deque<Connection> idle = new ArrayDeque<>();
    private final Object lock = new Object();
    private int openConnections = 0;
    private volatile boolean closed = false;

    public Database(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig().getConfigurationSection("database");
        if (config == null) {
            throw new IllegalStateException("The 'database' section is missing from config.yml");
        }

        String host = config.getString("host", "127.0.0.1");
        int port = config.getInt("port", 3306);
        String database = config.getString("name", "better_admin_commands");
        this.user = config.getString("user", "root");
        this.password = config.getString("password", "");
        this.poolSize = Math.max(1, config.getInt("pool-size", 4));
        this.tablePrefix = config.getString("table-prefix", "bac_");
        boolean useSsl = config.getBoolean("use-ssl", false);
        String extra = config.getString("connection-parameters", "");

        StringBuilder urlBuilder = new StringBuilder("jdbc:mysql://")
                .append(host).append(':').append(port).append('/').append(database)
                .append("?useSSL=").append(useSsl)
                .append("&allowPublicKeyRetrieval=true")
                .append("&autoReconnect=true")
                .append("&connectTimeout=10000")
                .append("&socketTimeout=30000");
        if (extra != null && !extra.isBlank()) {
            urlBuilder.append('&').append(extra);
        }
        this.url = urlBuilder.toString();
    }

    /** Opens one connection to verify the credentials and then creates the tables. */
    public void connect() throws SQLException {
        // Validate credentials eagerly so configuration mistakes show up at start-up.
        Connection connection = openRawConnection();
        closeQuietly(connection);
        createTables();
    }

    /** Creates every table the plugin needs, if it does not exist yet. */
    public void createTables() throws SQLException {
        withConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("players") + "` ("
                        + "`uuid` CHAR(36) NOT NULL,"
                        + "`name` VARCHAR(16) NOT NULL,"
                        + "`balance` DOUBLE NOT NULL DEFAULT 0,"
                        + "`last_seen` BIGINT NOT NULL DEFAULT 0,"
                        + "`muted_until` BIGINT NOT NULL DEFAULT 0,"
                        + "`mute_reason` VARCHAR(255) NULL,"
                        + "PRIMARY KEY (`uuid`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("homes") + "` ("
                        + "`uuid` CHAR(36) NOT NULL,"
                        + "`home` VARCHAR(32) NOT NULL,"
                        + "`world` VARCHAR(64) NOT NULL,"
                        + "`x` DOUBLE NOT NULL,"
                        + "`y` DOUBLE NOT NULL,"
                        + "`z` DOUBLE NOT NULL,"
                        + "`yaw` FLOAT NOT NULL DEFAULT 0,"
                        + "`pitch` FLOAT NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (`uuid`, `home`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                // Generic key/value store for per-player settings such as the
                // nickname, social spy, ignore list, tptoggle and afk state.
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("player_settings") + "` ("
                        + "`uuid` CHAR(36) NOT NULL,"
                        + "`setting_key` VARCHAR(48) NOT NULL,"
                        + "`setting_value` TEXT NULL,"
                        + "PRIMARY KEY (`uuid`, `setting_key`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("mail") + "` ("
                        + "`id` BIGINT NOT NULL AUTO_INCREMENT,"
                        + "`sender_uuid` CHAR(36) NOT NULL,"
                        + "`sender_name` VARCHAR(16) NOT NULL,"
                        + "`target_uuid` CHAR(36) NOT NULL,"
                        + "`message` TEXT NOT NULL,"
                        + "`sent_at` BIGINT NOT NULL,"
                        + "`is_read` TINYINT NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (`id`),"
                        + "KEY `idx_mail_target` (`target_uuid`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            }
            return null;
        });
    }

    /** Prefixes a table name with the configured prefix. */
    public String table(String name) {
        return tablePrefix + name;
    }

    /** Last time a player was stored, or {@code null} when they are unknown. */
    public Long lastSeen(java.util.UUID uuid) {
        try {
            return withConnection(connection -> {
                try (java.sql.PreparedStatement statement = connection.prepareStatement(
                        "SELECT `last_seen` FROM `" + table("players") + "` WHERE `uuid` = ?")) {
                    statement.setString(1, uuid.toString());
                    try (java.sql.ResultSet result = statement.executeQuery()) {
                        return result.next() ? result.getLong("last_seen") : null;
                    }
                }
            });
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Runs the given work on a pooled connection and returns its result.
     * The connection is always released back into the pool afterwards.
     */
    public <T> T withConnection(SqlFunction<T> work) throws SQLException {
        Connection connection = getConnection();
        try {
            return work.apply(connection);
        } finally {
            release(connection);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection cached;
        synchronized (lock) {
            while (true) {
                if (closed) {
                    throw new SQLException("The database connection pool is closed");
                }
                cached = idle.pollFirst();
                if (cached != null) {
                    break;
                }
                if (openConnections < poolSize) {
                    openConnections++;
                    cached = null;
                    break;
                }
                try {
                    lock.wait(15_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for a database connection");
                }
            }
        }

        if (cached != null) {
            try {
                if (!cached.isClosed() && cached.isValid(2)) {
                    return cached;
                }
            } catch (SQLException ignored) {
                // fall through and replace the broken connection
            }
            closeQuietly(cached);
            synchronized (lock) {
                openConnections--;
                lock.notify();
            }
            return getConnection();
        }

        try {
            return openRawConnection();
        } catch (SQLException e) {
            synchronized (lock) {
                openConnections--;
                lock.notify();
            }
            throw e;
        }
    }

    public void release(Connection connection) {
        if (connection == null) {
            return;
        }
        synchronized (lock) {
            if (closed) {
                closeQuietly(connection);
                openConnections--;
                return;
            }
            idle.addFirst(connection);
            lock.notify();
        }
    }

    public void close() {
        synchronized (lock) {
            closed = true;
            for (Connection connection : idle) {
                closeQuietly(connection);
            }
            idle.clear();
            openConnections = 0;
            lock.notifyAll();
        }
    }

    private Connection openRawConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // nothing sensible to do here
        }
    }
}
