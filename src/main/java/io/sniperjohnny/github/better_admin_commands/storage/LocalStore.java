package io.sniperjohnny.github.better_admin_commands.storage;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A small YAML backed row store that mirrors one database table on disk.
 *
 * <p>It serves two purposes:</p>
 * <ul>
 *   <li>it is the fallback the plugin reads from while MySQL is unavailable, so
 *       the server keeps working offline;</li>
 *   <li>it is an always-on local safe copy - every write the plugin makes is
 *       mirrored here, even while the database is reachable.</li>
 * </ul>
 *
 * <p>Every row carries a synthetic {@value #KEY} column holding a stable
 * identifier for the row (usually the primary key, or a composite of it).</p>
 */
public class LocalStore {

    /** Name of the synthetic column that identifies a row. */
    public static final String KEY = "_key";

    private final Better_Admin_Commands plugin;
    private final File file;
    private final Object lock = new Object();
    private final Map<String, Map<String, Object>> rows = new LinkedHashMap<>();

    public LocalStore(Better_Admin_Commands plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public File file() {
        return file;
    }

    /** Reads the safe file from disk, replacing the in-memory contents. */
    public void load() {
        synchronized (lock) {
            rows.clear();
            if (!file.exists()) {
                return;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            List<Map<?, ?>> loaded = yaml.getMapList("rows");
            for (Map<?, ?> raw : loaded) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : raw.entrySet()) {
                    row.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                Object key = row.get(KEY);
                if (key == null) {
                    continue;
                }
                rows.put(String.valueOf(key), row);
            }
        }
    }

    /** A deep-ish copy of every stored row, without the synthetic key column. */
    public List<Map<String, Object>> snapshot() {
        synchronized (lock) {
            List<Map<String, Object>> copy = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows.values()) {
                copy.add(copyOf(row));
            }
            return copy;
        }
    }

    /** Every stored row, including the synthetic {@value #KEY} column. */
    public List<Map<String, Object>> rawSnapshot() {
        synchronized (lock) {
            List<Map<String, Object>> copy = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows.values()) {
                copy.add(new LinkedHashMap<>(row));
            }
            return copy;
        }
    }

    /** Replaces the whole store with the given rows and writes the safe file. */
    public void replaceAll(List<Map<String, Object>> newRows) {
        synchronized (lock) {
            rows.clear();
            for (Map<String, Object> row : newRows) {
                Object key = row.get(KEY);
                if (key == null) {
                    continue;
                }
                Map<String, Object> copy = copyOf(row);
                copy.put(KEY, String.valueOf(key));
                rows.put(String.valueOf(key), copy);
            }
            saveLocked();
        }
    }

    /** Inserts or updates a single row and writes the safe file. */
    public void merge(String key, Map<String, Object> values) {
        if (key == null || key.isBlank()) {
            return;
        }
        synchronized (lock) {
            Map<String, Object> row = rows.computeIfAbsent(key, ignored -> {
                Map<String, Object> created = new LinkedHashMap<>();
                created.put(KEY, key);
                return created;
            });
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (KEY.equals(entry.getKey())) {
                    continue;
                }
                row.put(entry.getKey(), entry.getValue());
            }
            saveLocked();
        }
    }

    /**
     * Inserts or updates several rows in one go, writing the safe file once.
     * The map is keyed by the synthetic row key.
     */
    public void mergeAll(Map<String, Map<String, Object>> entries) {
        if (entries.isEmpty()) {
            return;
        }
        synchronized (lock) {
            for (Map.Entry<String, Map<String, Object>> entry : entries.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) {
                    continue;
                }
                Map<String, Object> row = rows.computeIfAbsent(key, ignored -> {
                    Map<String, Object> created = new LinkedHashMap<>();
                    created.put(KEY, key);
                    return created;
                });
                for (Map.Entry<String, Object> value : entry.getValue().entrySet()) {
                    if (!KEY.equals(value.getKey())) {
                        row.put(value.getKey(), value.getValue());
                    }
                }
            }
            saveLocked();
        }
    }

    /** Removes a single row and writes the safe file. */
    public void delete(String key) {
        synchronized (lock) {
            if (rows.remove(key) != null) {
                saveLocked();
            }
        }
    }

    /** Applies the given updates to every row matching the predicate. */
    public void updateWhere(Predicate<Map<String, Object>> test, Map<String, Object> updates) {
        synchronized (lock) {
            boolean changed = false;
            for (Map<String, Object> row : rows.values()) {
                if (test.test(row)) {
                    row.putAll(updates);
                    changed = true;
                }
            }
            if (changed) {
                saveLocked();
            }
        }
    }

    /** Removes every row matching the predicate and writes the safe file. */
    public void deleteWhere(Predicate<Map<String, Object>> test) {
        synchronized (lock) {
            if (rows.values().removeIf(test)) {
                saveLocked();
            }
        }
    }

    /** Looks a row up by its synthetic key, or {@code null}. */
    public Map<String, Object> find(String key) {
        synchronized (lock) {
            Map<String, Object> row = rows.get(key);
            return row == null ? null : copyOf(row);
        }
    }

    public boolean isEmpty() {
        synchronized (lock) {
            return rows.isEmpty();
        }
    }

    /** Reads a value from a row, tolerant about numeric/string representations. */
    public static String string(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : String.valueOf(value);
    }

    public static long longValue(Map<String, Object> row, String column, long fallback) {
        Object value = row.get(column);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }

    public static double doubleValue(Map<String, Object> row, String column, double fallback) {
        Object value = row.get(column);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }

    private void saveLocked() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("rows", new ArrayList<>(rows.values()));
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not write the local safe file " + file.getName() + ": "
                    + e.getMessage());
        }
    }

    private static Map<String, Object> copyOf(Map<String, Object> row) {
        Map<String, Object> copy = new LinkedHashMap<>(row);
        copy.remove(KEY);
        return copy;
    }

    /** Builds a composite key from the given parts, e.g. uuid + ":" + home. */
    public static String composite(Object... parts) {
        StringBuilder builder = new StringBuilder();
        for (Object part : parts) {
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(Objects.toString(part, ""));
        }
        return builder.toString();
    }
}
