package io.sniperjohnny.github.better_admin_commands.powertool;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PowertoolService {

    private static final String PREFIX = "powertool.";

    private final Better_Admin_Commands plugin;

    public PowertoolService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void bind(Player player, Material material, String command) {
        plugin.preferences().set(player.getUniqueId(), PREFIX + material.name(), command);
    }

    public void clear(Player player, Material material) {
        plugin.preferences().set(player.getUniqueId(), PREFIX + material.name(), null);
    }

    public void clearAll(Player player) {
        for (Map.Entry<String, String> entry : bindings(player).entrySet()) {
            plugin.preferences().set(player.getUniqueId(), PREFIX + entry.getKey(), null);
        }
    }

    public Map<String, String> bindings(Player player) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : plugin.preferences().allEntries(player.getUniqueId()).entrySet()) {
            if (entry.getKey().startsWith(PREFIX)) {
                result.put(entry.getKey().substring(PREFIX.length()).toUpperCase(Locale.ROOT), entry.getValue());
            }
        }
        return result;
    }

    public String commandFor(Player player, Material material) {
        return plugin.preferences().get(player.getUniqueId(), PREFIX + material.name(), null);
    }

    public boolean run(Player player, Material material) {
        String command = commandFor(player, material);
        if (command == null || command.isBlank()) {
            return false;
        }
        String resolved = command.replace("{player}", player.getName());
        if (resolved.startsWith("/")) {
            resolved = resolved.substring(1);
        }
        player.performCommand(resolved);
        return true;
    }
}
