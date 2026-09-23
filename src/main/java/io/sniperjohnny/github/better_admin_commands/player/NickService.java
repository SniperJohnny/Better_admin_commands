package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;

import java.util.List;
import java.util.UUID;

/**
 * Access to the LuckPerms group data {@code /nick} uses.
 *
 * <p>The point of this wrapper is that LuckPerms stays optional: the API is only
 * touched through {@link LuckPermsBridge}, which is created when the LuckPerms
 * plugin is installed and left alone otherwise. Every method therefore answers
 * something sensible on a server without LuckPerms.</p>
 */
public class NickService {

    private final LuckPermsBridge bridge;

    public NickService(Better_Admin_Commands plugin) {
        this.bridge = createBridge(plugin);
    }

    private static LuckPermsBridge createBridge(Better_Admin_Commands plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.getLogger().info("LuckPerms is not installed - /nick renames players, "
                    + "but group prefixes are unavailable.");
            return null;
        }
        try {
            return new LuckPermsBridge();
        } catch (Throwable e) {
            plugin.getLogger().warning("Could not hook into LuckPerms: " + e.getMessage());
            return null;
        }
    }

    /** Whether group prefixes can be read from LuckPerms. */
    public boolean available() {
        return bridge != null;
    }

    /** Every group LuckPerms has loaded, for tab completion. */
    public List<String> groupNames() {
        return bridge == null ? List.of() : bridge.groupNames();
    }

    /** Whether a group with this name exists. */
    public boolean hasGroup(String group) {
        return bridge != null && group != null && bridge.hasGroup(group);
    }

    /**
     * The prefix of a group, or {@code null} when the group has none or when
     * LuckPerms is not installed.
     */
    public String prefix(String group) {
        return bridge == null || group == null ? null : bridge.prefix(group);
    }

    /**
     * The prefix of a player's own rank, used by {@code /nick <nickname>} when no
     * group is named. Returns {@code null} when LuckPerms is absent or the rank
     * has no prefix.
     */
    public String userPrefix(UUID uuid) {
        return bridge == null || uuid == null ? null : bridge.userPrefix(uuid);
    }
}
