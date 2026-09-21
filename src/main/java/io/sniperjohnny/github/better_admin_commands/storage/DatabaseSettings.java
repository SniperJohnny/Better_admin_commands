package io.sniperjohnny.github.better_admin_commands.storage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Reads the database connection out of a single connection string.
 *
 * <p>Most hosting panels hand out something like</p>
 *
 * <pre>jdbc:mysql://user:p%40ssword@127.0.0.1:3306/minecraft</pre>
 *
 * <p>and this class takes that apart: which engine it is, where the server
 * lives, which database, and the credentials - decoded, because panels
 * percent-encode the characters that would otherwise break the URL.</p>
 *
 * <p>Both the {@code jdbc:} form and the plain URL form are accepted, with or
 * without credentials, so a string can be pasted straight into config.yml.</p>
 */
public final class DatabaseSettings {

    /** What kind of server a connection string points at. */
    public enum Engine {
        MYSQL("MySQL"),
        MARIADB("MariaDB"),
        POSTGRESQL("PostgreSQL"),
        SQLITE("SQLite"),
        UNKNOWN("an unknown database");

        private final String label;

        Engine(String label) {
            this.label = label;
        }

        /** Human readable name, for logs and messages. */
        public String label() {
            return label;
        }

        /** Whether this plugin can actually talk to it. */
        public boolean supported() {
            return this == MYSQL || this == MARIADB;
        }

        /** The port used when the connection string does not name one. */
        public int defaultPort() {
            return switch (this) {
                case POSTGRESQL -> 5432;
                case SQLITE -> 0;
                default -> 3306;
            };
        }
    }

    /** Everything needed to open the connection. */
    public record Connection(Engine engine, String host, int port, String database, String user,
                             String password, String parameters) {

        /** A one line summary that never prints the password. */
        public String describe() {
            if (engine == Engine.SQLITE) {
                return engine.label() + " file " + database;
            }
            StringBuilder text = new StringBuilder(engine.label()).append(" ");
            if (user != null && !user.isBlank()) {
                text.append(user).append('@');
            }
            text.append(host).append(':').append(port);
            if (database != null && !database.isBlank()) {
                text.append('/').append(database);
            }
            if (password != null && !password.isBlank()) {
                text.append(" (password set)");
            }
            return text.toString();
        }
    }

    private DatabaseSettings() {
    }

    /**
     * Parses a connection string.
     *
     * @return the parsed settings, or {@code null} when the input is empty
     */
    public static Connection parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.regionMatches(true, 0, "jdbc:", 0, 5)) {
            value = value.substring(5);
        }

        String scheme;
        String remainder;
        int schemeEnd = value.indexOf("://");
        if (schemeEnd > 0) {
            scheme = value.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
            remainder = value.substring(schemeEnd + 3);
        } else if (value.toLowerCase(Locale.ROOT).startsWith("sqlite:")) {
            scheme = "sqlite";
            remainder = value.substring("sqlite:".length());
        } else {
            // A bare host[:port][/database] can only sensibly mean MySQL.
            scheme = "mysql";
            remainder = value;
        }

        Engine engine = engineOf(scheme);
        if (engine == Engine.SQLITE) {
            return new Connection(engine, "", 0, percentDecode(remainder), "", "", "");
        }

        // Credentials sit in front of the last @, so a password may contain one.
        String credentials = null;
        int at = remainder.lastIndexOf('@');
        if (at >= 0) {
            credentials = remainder.substring(0, at);
            remainder = remainder.substring(at + 1);
        }

        String parameters = "";
        int query = remainder.indexOf('?');
        if (query >= 0) {
            parameters = remainder.substring(query + 1);
            remainder = remainder.substring(0, query);
        }

        String database = "";
        int slash = remainder.indexOf('/');
        if (slash >= 0) {
            database = percentDecode(remainder.substring(slash + 1));
            remainder = remainder.substring(0, slash);
        }

        String host = remainder;
        int port = engine.defaultPort();
        if (host.startsWith("[")) { // IPv6, for example [::1]:3306
            int end = host.indexOf(']');
            if (end > 0) {
                String after = host.substring(end + 1);
                host = host.substring(1, end);
                if (after.startsWith(":")) {
                    port = parsePort(after.substring(1), port);
                }
            }
        } else {
            int colon = host.lastIndexOf(':');
            if (colon > 0) {
                port = parsePort(host.substring(colon + 1), port);
                host = host.substring(0, colon);
            }
        }

        String user = "";
        String password = "";
        if (credentials != null) {
            int separator = credentials.indexOf(':');
            if (separator < 0) {
                user = percentDecode(credentials);
            } else {
                user = percentDecode(credentials.substring(0, separator));
                password = percentDecode(credentials.substring(separator + 1));
            }
        }

        return new Connection(engine, percentDecode(host).trim(), port, database, user, password, parameters);
    }

    /** Maps a URL scheme onto the engine it belongs to. */
    public static Engine engineOf(String scheme) {
        if (scheme == null) {
            return Engine.UNKNOWN;
        }
        return switch (scheme.toLowerCase(Locale.ROOT)) {
            case "mysql" -> Engine.MYSQL;
            case "mariadb" -> Engine.MARIADB;
            case "postgres", "postgresql" -> Engine.POSTGRESQL;
            case "sqlite", "file" -> Engine.SQLITE;
            default -> Engine.UNKNOWN;
        };
    }

    /**
     * Decodes {@code %XX} escapes the way a URL userinfo section is encoded.
     *
     * <p>Unlike a query string, a {@code +} is a literal plus here - which
     * matters, because panels hand out passwords with {@code %2B} for a plus and
     * a naive URL decoder would silently turn it into a space.</p>
     */
    public static String percentDecode(String value) {
        if (value == null || value.indexOf('%') < 0) {
            return value == null ? "" : value;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    bytes.write((high << 4) + low);
                    i += 2;
                    continue;
                }
            }
            bytes.writeBytes(String.valueOf(current).getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /**
     * A suggestion for the most common connection failures, or {@code null}
     * when the message does not look familiar.
     */
    public static String hintFor(String message) {
        if (message == null) {
            return null;
        }
        String lowered = message.toLowerCase(Locale.ROOT);
        if (lowered.contains("access denied")) {
            return "the user name or password is wrong, or the user is not allowed to connect from this server";
        }
        if (lowered.contains("unknown database")) {
            return "the database does not exist yet; let the plugin create it or make it yourself";
        }
        if (lowered.contains("communications link failure") || lowered.contains("connection refused")
                || lowered.contains("connection timed out") || lowered.contains("unknownhost")
                || lowered.contains("no suitable driver")) {
            return "the database is not reachable at that address; check database.host and database.port "
                    + "and that the database accepts connections from this machine";
        }
        if (lowered.contains("public key retrieval")) {
            return "add useSSL=false to the connection parameters - the plugin already does that by default";
        }
        if (lowered.contains("too many connections")) {
            return "lower database.pool-size or raise the connection limit on the database server";
        }
        return null;
    }

    private static int parsePort(String text, int fallback) {
        try {
            int port = Integer.parseInt(text.trim());
            return port > 0 && port <= 65535 ? port : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
