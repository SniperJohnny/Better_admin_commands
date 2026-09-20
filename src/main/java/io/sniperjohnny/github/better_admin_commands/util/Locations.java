package io.sniperjohnny.github.better_admin_commands.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Serialises locations to a compact string so they can be stored in YAML files
 * or single database columns without losing the world or rotation.
 */
public final class Locations {

    private Locations() {
    }

    /** Format: {@code world;x;y;z;yaw;pitch} */
    public static String serialize(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return location.getWorld().getName() + ';'
                + location.getX() + ';'
                + location.getY() + ';'
                + location.getZ() + ';'
                + location.getYaw() + ';'
                + location.getPitch();
    }

    public static Location deserialize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(";");
        if (parts.length < 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void write(ConfigurationSection section, String path, Location location) {
        section.set(path, serialize(location));
    }

    public static Location read(ConfigurationSection section, String path) {
        return deserialize(section.getString(path));
    }
}
