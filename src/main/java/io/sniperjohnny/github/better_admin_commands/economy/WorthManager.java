package io.sniperjohnny.github.better_admin_commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * Item prices for {@code /worth} and {@code /sell}, read from the
 * {@code worth} section of config.yml. Items without an entry are worthless and
 * cannot be sold.
 */
public class WorthManager {

    private final Better_Admin_Commands plugin;

    public WorthManager(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    /** Price of a single item, 0.0 when the item cannot be sold. */
    public double price(Material material) {
        if (material == null || material.isAir()) {
            return 0.0;
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("worth");
        if (section == null) {
            return 0.0;
        }
        // The section is not case sensitive in config, so try the exact name first.
        Double direct = section.getDouble(material.name(), -1.0);
        if (direct >= 0) {
            return direct;
        }
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(material.name())) {
                return Math.max(0.0, section.getDouble(key, 0.0));
            }
        }
        return 0.0;
    }

    public boolean isSellable(Material material) {
        return price(material) > 0.0;
    }

    /** Formats a price using the currency settings. */
    public String format(double amount) {
        return plugin.economy().format(amount);
    }

    public String nameOf(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
