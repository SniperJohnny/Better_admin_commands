package io.sniperjohnny.github.better_admin_commands.storage;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

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

    /** One step of the connection check behind {@code /betteradmincommands database}. */
    public record Check(String label, boolean ok, String detail) {
    }

    private final Better_Admin_Commands plugin;
    private final String url;
    /** The same url without the database, used to create a missing database. */
    private final String serverUrl;
    private final String user;
    private final String password;
    private final int poolSize;
    private final String tablePrefix;

    private final DatabaseSettings.Engine engine;
    private final String host;
    private final int port;
    private final String databaseName;
    private final boolean createIfMissing;
    /** Where the settings came from, so a diagnosis can say so. */
    private final String source;

    private final Deque<Connection> idle = new ArrayDeque<>();
    private final Object lock = new Object();
    private int openConnections = 0;
    private volatile boolean closed = false;
    private volatile boolean available = false;

    public Database(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig().getConfigurationSection("database");
        if (config == null) {
            throw new IllegalStateException("The 'database' section is missing from config.yml");
        }

        // A connection string, when given, wins over the single fields - but the
        // fields still fill in whatever the string leaves out.
        DatabaseSettings.Connection parsed =
                DatabaseSettings.parse(config.getString("connection-string", ""));
        this.source = parsed != null ? "connection-string" : "host/port/name/user/password";
        this.engine = parsed != null ? parsed.engine() : DatabaseSettings.Engine.MYSQL;

        this.host = fill(parsed == null ? null : parsed.host(), config.getString("host", "127.0.0.1"));
        this.port = parsed != null && parsed.port() > 0
                ? parsed.port() : config.getInt("port", engine.defaultPort());
        this.databaseName = fill(parsed == null ? null : parsed.database(),
                config.getString("name", "better_admin_commands")).replace("`", "");
        this.user = fill(parsed == null ? null : parsed.user(), config.getString("user", "root"));
        this.password = fill(parsed == null ? null : parsed.password(), config.getString("password", ""));
        this.poolSize = Math.max(1, config.getInt("pool-size", 4));
        this.tablePrefix = config.getString("table-prefix", "bac_");
        this.createIfMissing = config.getBoolean("create-if-missing", true);

        boolean useSsl = config.getBoolean("use-ssl", false);
        String extra = fill(parsed == null ? null : parsed.parameters(),
                config.getString("connection-parameters", ""));
        this.serverUrl = buildUrl(host, port, "", useSsl, extra);
        this.url = buildUrl(host, port, databaseName, useSsl, extra);
    }

    /**
     * Opens one connection to verify the credentials, creates the database when
     * it does not exist yet, and then creates the tables.
     */
    public void connect() throws SQLException {
        if (!engine.supported()) {
            throw new SQLException("The connection details point at " + engine.label()
                    + ", which this plugin cannot talk to. Use a MySQL or MariaDB database, or remove "
                    + "database.connection-string and fill in the fields below it.");
        }
        // Validate credentials eagerly so configuration mistakes show up at start-up.
        try {
            verifyConnection();
        } catch (SQLException failure) {
            if (!tryCreateDatabase(failure)) {
                throw hint(failure);
            }
        }
        createTables();
        available = true;
    }

    /** Opens and closes one connection, to prove the credentials work. */
    private void verifyConnection() throws SQLException {
        closeQuietly(openRawConnection());
    }

    /**
     * Creates the configured database when the only problem is that it does not
     * exist yet.
     *
     * @return {@code true} when the database is usable afterwards
     */
    private boolean tryCreateDatabase(SQLException failure) throws SQLException {
        boolean missing = failure.getMessage() != null
                && failure.getMessage().toLowerCase(Locale.ROOT).contains("unknown database");
        if (!createIfMissing || databaseName.isBlank() || !missing) {
            return false;
        }
        plugin.getLogger().info("The database '" + databaseName + "' does not exist yet - creating it.");
        try (Connection connection = DriverManager.getConnection(serverUrl, user, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + databaseName + "` "
                    + "CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        } catch (SQLException createFailure) {
            throw new SQLException("Could not create the database '" + databaseName + "' ("
                    + createFailure.getMessage() + "). Create it yourself or ask your host to.", createFailure);
        }
        verifyConnection();
        plugin.getLogger().info("Created the database '" + databaseName + "'.");
        return true;
    }

    /** Adds a suggestion to the message of a failure that looks familiar. */
    private static SQLException hint(SQLException failure) {
        String hint = DatabaseSettings.hintFor(failure.getMessage());
        return hint == null ? failure : new SQLException(failure.getMessage() + " - " + hint, failure);
    }

    private static String buildUrl(String host, int port, String database, boolean useSsl, String extra) {
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(host).append(':').append(port).append('/').append(database)
                .append("?useSSL=").append(useSsl)
                .append("&allowPublicKeyRetrieval=true")
                .append("&autoReconnect=true")
                .append("&connectTimeout=10000")
                .append("&socketTimeout=30000");
        if (extra != null && !extra.isBlank()) {
            url.append('&').append(extra);
        }
        return url.toString();
    }

    /** Prefers the value from the connection string, else the config field. */
    private static String fill(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : (fallback == null ? "" : fallback);
    }

    /* ---------------------------------------------------------- diagnosis --- */

    /** What the plugin thinks it is connecting to, without the password. */
    public String describe() {
        StringBuilder text = new StringBuilder(engine.label()).append(' ');
        if (!user.isBlank()) {
            text.append(user).append('@');
        }
        text.append(host).append(':').append(port);
        if (!databaseName.isBlank()) {
            text.append('/').append(databaseName);
        }
        return text.append(" (from ").append(source).append(')').toString();
    }

    /**
     * Walks through the connection step by step. Blocking, so call it from an
     * async task - used by {@code /betteradmincommands database}.
     */
    public List<Check> diagnose() {
        List<Check> checks = new ArrayList<>();
        checks.add(new Check("Engine", engine.supported(), engine.label()
                + (engine.supported() ? "" : " - only MySQL and MariaDB are supported")));
        if (!engine.supported()) {
            return checks;
        }
        checks.add(new Check("Settings", true, describe()));
        checks.add(checkAddress());
        checks.add(checkPort());
        checks.add(checkLogin());
        checks.add(checkTables());
        return checks;
    }

    private Check checkAddress() {
        try {
            return new Check("Address", true,
                    host + " resolves to " + InetAddress.getByName(host).getHostAddress());
        } catch (UnknownHostException e) {
            return new Check("Address", false, "cannot resolve '" + host + "' - check database.host");
        }
    }

    private Check checkPort() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            return new Check("Port", true, "port " + port + " is open");
        } catch (IOException e) {
            return new Check("Port", false, "cannot reach port " + port
                    + " - check database.port, the firewall and whether the database allows remote connections");
        }
    }

    private Check checkLogin() {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            return new Check("Login", true, "the credentials work");
        } catch (SQLException e) {
            String hint = DatabaseSettings.hintFor(e.getMessage());
            return new Check("Login", false, e.getMessage() + (hint == null ? "" : " - " + hint));
        }
    }

    private Check checkTables() {
        try {
            Long count = withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema = ? AND table_name LIKE ?")) {
                    statement.setString(1, databaseName);
                    statement.setString(2, tablePrefix + "%");
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? result.getLong(1) : 0L;
                    }
                }
            });
            return new Check("Tables", true, (count == null ? 0L : count)
                    + " of the plugin's tables exist (prefix '" + tablePrefix + "')");
        } catch (SQLException e) {
            return new Check("Tables", false, e.getMessage());
        }
    }

    /**
     * Whether the database is currently usable. When this is {@code false} the
     * plugin keeps running from its local safe files and retries periodically.
     */
    public boolean isAvailable() {
        return available && !closed;
    }

    /**
     * Verifies the pooled connection, reconnecting when the database is down.
     * Called periodically so a database that comes back is picked up without a
     * server restart. Returns {@code true} when the database is reachable
     * afterwards.
     */
    public boolean checkConnection() {
        if (closed || !engine.supported()) {
            return false;
        }
        if (!available) {
            try {
                connect();
                plugin.getLogger().info("Reconnected to the MySQL database.");
                return true;
            } catch (SQLException e) {
                plugin.getLogger().warning("MySQL is still unavailable: " + e.getMessage());
                return false;
            }
        }
        Connection connection = null;
        try {
            connection = getConnection();
            if (connection.isValid(2)) {
                return true;
            }
        } catch (SQLException e) {
            // fall through and mark the database as gone
        } finally {
            if (connection != null) {
                release(connection);
            }
        }
        available = false;
        plugin.getLogger().warning("Lost the MySQL connection - switching to the local safe files.");
        return false;
    }

    /** Marks the database as unavailable so the reconnect task starts trying again. */
    public void markUnavailable() {
        available = false;
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

                // Auction house listings. `item` holds the Base64 encoded form
                // of ItemStack#serializeAsBytes(), so items survive a restart.
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("ah_listings") + "` ("
                        + "`id` CHAR(36) NOT NULL,"
                        + "`seller_uuid` CHAR(36) NOT NULL,"
                        + "`seller_name` VARCHAR(16) NOT NULL,"
                        + "`item` MEDIUMTEXT NOT NULL,"
                        + "`price` DOUBLE NOT NULL,"
                        + "`created_at` BIGINT NOT NULL,"
                        + "`expires_at` BIGINT NOT NULL,"
                        + "`state` VARCHAR(12) NOT NULL,"
                        + "`claimed` TINYINT NOT NULL DEFAULT 0,"
                        + "`buyer_name` VARCHAR(16) NULL,"
                        + "PRIMARY KEY (`id`),"
                        + "KEY `idx_ah_state` (`state`),"
                        + "KEY `idx_ah_seller` (`seller_uuid`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                // The shop: one row per imported shop and one per entry. `item`
                // again holds the Base64 form of ItemStack#serializeAsBytes(), so
                // display names, lore and enchantments all survive.
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("shops") + "` ("
                        + "`id` VARCHAR(96) NOT NULL,"
                        + "`display` VARCHAR(96) NULL,"
                        + "`icon` MEDIUMTEXT NULL,"
                        + "`rows` INT NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (`id`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("shop_items") + "` ("
                        + "`shop` VARCHAR(96) NOT NULL,"
                        + "`item_key` VARCHAR(160) NOT NULL,"
                        + "`material` VARCHAR(64) NOT NULL,"
                        + "`search` VARCHAR(160) NULL,"
                        + "`item` MEDIUMTEXT NOT NULL,"
                        + "`buy_price` DOUBLE NOT NULL DEFAULT -1,"
                        + "`sell_price` DOUBLE NOT NULL DEFAULT -1,"
                        + "`slot` INT NOT NULL DEFAULT 0,"
                        + "`page` INT NOT NULL DEFAULT 1,"
                        + "PRIMARY KEY (`shop`, `item_key`),"
                        + "KEY `idx_shop_material` (`material`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                // Report tickets and their two-way message threads.
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("reports") + "` ("
                        + "`id` CHAR(36) NOT NULL,"
                        + "`reporter_uuid` CHAR(36) NOT NULL,"
                        + "`reporter_name` VARCHAR(16) NOT NULL,"
                        + "`target_name` VARCHAR(32) NULL,"
                        + "`category` VARCHAR(32) NOT NULL,"
                        + "`status` VARCHAR(12) NOT NULL,"
                        + "`created_at` BIGINT NOT NULL,"
                        + "`updated_at` BIGINT NOT NULL,"
                        + "PRIMARY KEY (`id`),"
                        + "KEY `idx_report_status` (`status`),"
                        + "KEY `idx_report_reporter` (`reporter_uuid`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("report_messages") + "` ("
                        + "`id` CHAR(36) NOT NULL,"
                        + "`report_id` CHAR(36) NOT NULL,"
                        + "`author_uuid` CHAR(36) NOT NULL,"
                        + "`author_name` VARCHAR(16) NOT NULL,"
                        + "`staff` TINYINT NOT NULL DEFAULT 0,"
                        + "`message` TEXT NOT NULL,"
                        + "`created_at` BIGINT NOT NULL,"
                        + "PRIMARY KEY (`id`),"
                        + "KEY `idx_rm_report` (`report_id`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                // Everybody who took part in a ticket, with the read marker that
                // drives the unread counts and the notifications.
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table("report_participants") + "` ("
                        + "`report_id` CHAR(36) NOT NULL,"
                        + "`uuid` CHAR(36) NOT NULL,"
                        + "`name` VARCHAR(16) NOT NULL,"
                        + "`staff` TINYINT NOT NULL DEFAULT 0,"
                        + "`last_read` BIGINT NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (`report_id`, `uuid`)"
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
            available = false;
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
