package io.sniperjohnny.github.better_admin_commands.warp;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Locations;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Named teleport points stored in {@code warps.yml}.
 */
public class WarpManager {

    private final Better_Admin_Commands plugin;
    private final File file;
    private final Map<String, Location> warps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public WarpManager(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
    }

    public void load() {
        warps.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("warps");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Location location = Locations.read(section, key);
            if (location != null) {
                warps.put(key.toLowerCase(Locale.ROOT), location);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            Locations.write(yaml, "warps." + entry.getKey(), entry.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps.yml: " + e.getMessage());
        }
    }

    public boolean exists(String name) {
        return warps.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Location get(String name) {
        Location location = warps.get(name.toLowerCase(Locale.ROOT));
        return location == null ? null : location.clone();
    }

    public void set(String name, Location location) {
        warps.put(name.toLowerCase(Locale.ROOT), location.clone());
        save();
    }

    public boolean delete(String name) {
        if (warps.remove(name.toLowerCase(Locale.ROOT)) == null) {
            return false;
        }
        save();
        return true;
    }

    /** All warp names, sorted alphabetically. */
    public java.util.Set<String> names() {
        return warps.keySet();
    }

    public int size() {
        return warps.size();
    }
}
