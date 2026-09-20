package io.sniperjohnny.github.better_admin_commands.spawn;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Locations;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Stores the server spawn in {@code spawn.yml} next to config.yml.
 * /setspawn writes the coordinates of the executing player, /spawn reads them.
 */
public class SpawnManager {

    private final Better_Admin_Commands plugin;
    private final File file;
    private Location spawn;

    public SpawnManager(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getLogger().info("No spawn.yml found yet - use /setspawn to create one.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        spawn = Locations.read(yaml, "spawn");
        if (spawn == null) {
            plugin.getLogger().warning("spawn.yml exists but does not contain a valid spawn location.");
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        Locations.write(yaml, "spawn", spawn);
        if (spawn != null) {
            yaml.set("world", spawn.getWorld().getName());
            yaml.set("x", spawn.getX());
            yaml.set("y", spawn.getY());
            yaml.set("z", spawn.getZ());
            yaml.set("yaw", spawn.getYaw());
            yaml.set("pitch", spawn.getPitch());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spawn.yml: " + e.getMessage());
        }
    }

    public boolean hasSpawn() {
        return spawn != null;
    }

    public Location getSpawn() {
        return spawn == null ? null : spawn.clone();
    }

    public void setSpawn(Location location) {
        this.spawn = location.clone();
        save();
    }
}
