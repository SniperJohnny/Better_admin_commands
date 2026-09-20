package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Players in "unlimited" mode place blocks and consume items without losing
 * them. The flag is stored in the player settings.
 */
public class UnlimitedService {

    private static final String SETTING = "unlimited";

    private final Better_Admin_Commands plugin;

    public UnlimitedService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public boolean isUnlimited(UUID uuid) {
        return plugin.preferences().getBoolean(uuid, SETTING, false);
    }

    public boolean isUnlimited(Player player) {
        return isUnlimited(player.getUniqueId());
    }

    /** Toggles the mode and returns the new state. */
    public boolean toggle(Player player) {
        boolean enabled = !isUnlimited(player);
        plugin.preferences().setBoolean(player.getUniqueId(), SETTING, enabled);
        return enabled;
    }

    public void clear(Player player) {
        plugin.preferences().setBoolean(player.getUniqueId(), SETTING, false);
    }

    /** Names of every online player who currently has unlimited items. */
    public List<String> activePlayers() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isUnlimited(player)) {
                names.add(player.getName());
            }
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }
}
