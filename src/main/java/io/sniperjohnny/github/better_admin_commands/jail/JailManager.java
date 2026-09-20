package io.sniperjohnny.github.better_admin_commands.jail;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Locations;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Jail cells live in {@code jails.yml}, the state of a jailed player is kept in
 * their player settings so it survives restarts.
 */
public class JailManager {

    /** A player serving time. */
    public record JailEntry(String jail, long until) {

        public boolean permanent() {
            return until < 0;
        }

        public boolean expired() {
            return !permanent() && System.currentTimeMillis() > until;
        }

        public long remainingMillis() {
            return permanent() ? -1 : Math.max(0, until - System.currentTimeMillis());
        }
    }

    private static final String SETTING = "jail";

    private final Better_Admin_Commands plugin;
    private final File file;
    private final Map<String, Location> jails = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public JailManager(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "jails.yml");
    }

    public void load() {
        jails.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("jails");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Location location = Locations.read(section, key);
            if (location != null) {
                jails.put(key.toLowerCase(Locale.ROOT), location);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Location> entry : jails.entrySet()) {
            Locations.write(yaml, "jails." + entry.getKey(), entry.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save jails.yml: " + e.getMessage());
        }
    }

    public boolean exists(String name) {
        return jails.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Location get(String name) {
        Location location = jails.get(name.toLowerCase(Locale.ROOT));
        return location == null ? null : location.clone();
    }

    public void set(String name, Location location) {
        jails.put(name.toLowerCase(Locale.ROOT), location.clone());
        save();
    }

    public boolean delete(String name) {
        if (jails.remove(name.toLowerCase(Locale.ROOT)) == null) {
            return false;
        }
        save();
        return true;
    }

    public Set<String> names() {
        return jails.keySet();
    }

    public int size() {
        return jails.size();
    }

    /** The configured cell size players are contained in. */
    public double radius() {
        return Math.max(1.0, plugin.getConfig().getDouble("jail.radius", 8.0));
    }

    /* ---------------------------------------------------------- prisoners -- */

    public JailEntry entryOf(UUID uuid) {
        String raw = plugin.preferences().get(uuid, SETTING, null);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int separator = raw.lastIndexOf(';');
        if (separator < 0) {
            return new JailEntry(raw, -1L);
        }
        try {
            return new JailEntry(raw.substring(0, separator), Long.parseLong(raw.substring(separator + 1)));
        } catch (NumberFormatException e) {
            return new JailEntry(raw.substring(0, separator), -1L);
        }
    }

    public boolean isJailed(UUID uuid) {
        JailEntry entry = entryOf(uuid);
        if (entry == null) {
            return false;
        }
        if (entry.expired()) {
            unjail(uuid);
            return false;
        }
        return true;
    }

    /** Jails a player. {@code until} is an epoch millisecond timestamp or -1. */
    public void jail(UUID uuid, String jailName, long until) {
        plugin.preferences().set(uuid, SETTING, jailName + ";" + until);
    }

    public void unjail(UUID uuid) {
        plugin.preferences().set(uuid, SETTING, null);
    }

    /** Moves a jailed player back to their cell. */
    public void sendToCell(Player player) {
        JailEntry entry = entryOf(player.getUniqueId());
        if (entry == null) {
            return;
        }
        Location cell = get(entry.jail());
        if (cell != null) {
            player.teleport(cell);
        }
    }
}
