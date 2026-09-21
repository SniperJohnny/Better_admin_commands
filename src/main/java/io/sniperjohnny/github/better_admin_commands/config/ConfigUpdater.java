package io.sniperjohnny.github.better_admin_commands.config;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Keeps {@code config.yml} in sync with the template that is shipped inside the
 * plugin jar.
 *
 * <p>Updating works by <em>adding missing options only</em>: an option that is
 * already present in the file on disk is never touched, so the values a server
 * owner set survive every plugin update. The comments belonging to a newly
 * added option are copied over as well, and the file is only rewritten when
 * something actually changed. A version bump also drops a copy of the previous
 * file next to it, so a bad migration is easy to undo.</p>
 */
public class ConfigUpdater {

    /** Version of the template bundled with this build. */
    public static final int CURRENT_VERSION = 1;

    private static final String FILE_NAME = "config.yml";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Better_Admin_Commands plugin;

    public ConfigUpdater(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    /**
     * Merges the bundled template into {@code config.yml}. Safe to call more
     * than once, for example from a reload.
     *
     * @return the number of options that were added, or {@code 0} when the file
     *         was already up to date
     */
    public int update() {
        YamlConfiguration template = template();
        if (template == null) {
            plugin.getLogger().warning("The bundled config.yml could not be read - skipping the config update.");
            return 0;
        }

        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        int previousVersion = config.getInt("config-version", 0);
        int currentVersion = template.getInt("config-version", CURRENT_VERSION);

        int added = 0;
        for (String path : template.getKeys(true)) {
            if (template.isConfigurationSection(path)) {
                continue;
            }
            // An existing option - whatever the value - always wins.
            if (config.contains(path)) {
                continue;
            }
            config.set(path, template.get(path));
            List<String> comments = template.getComments(path);
            if (!comments.isEmpty()) {
                config.setComments(path, comments);
            }
            List<String> inlineComments = template.getInlineComments(path);
            if (!inlineComments.isEmpty()) {
                config.setInlineComments(path, inlineComments);
            }
            added++;
        }

        boolean versionChanged = previousVersion != currentVersion;
        if (!versionChanged && added == 0) {
            return 0;
        }

        if (versionChanged) {
            backup(previousVersion);
            config.set("config-version", currentVersion);
        }

        try {
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not write config.yml: " + e.getMessage());
            return 0;
        }

        if (versionChanged) {
            plugin.getLogger().info("config.yml was migrated from version " + previousVersion
                    + " to " + currentVersion + ".");
        }
        if (added > 0) {
            plugin.getLogger().info("config.yml updated: " + added
                    + " new option(s) added, every existing value was kept.");
        }
        return added;
    }

    /** Copies the current file aside before it is migrated. */
    private void backup(int previousVersion) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists() || file.length() == 0) {
            return;
        }
        File target = new File(plugin.getDataFolder(),
                "config-backup-v" + previousVersion + "-" + LocalDateTime.now().format(STAMP) + ".yml");
        try {
            Files.copy(file.toPath(), target.toPath());
            plugin.getLogger().info("Backed up the previous config.yml to " + target.getName() + ".");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not back up config.yml: " + e.getMessage());
        }
    }

    /** Reads the config.yml that is packaged inside the plugin jar. */
    private YamlConfiguration template() {
        try (InputStream stream = plugin.getResource(FILE_NAME)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.options().parseComments(true);
                yaml.load(reader);
                return yaml;
            }
        } catch (IOException | InvalidConfigurationException e) {
            return null;
        }
    }
}
