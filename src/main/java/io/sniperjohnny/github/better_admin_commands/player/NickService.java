package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;

import java.util.List;
import java.util.UUID;

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

    public boolean available() {
        return bridge != null;
    }

    public List<String> groupNames() {
        return bridge == null ? List.of() : bridge.groupNames();
    }

    public boolean hasGroup(String group) {
        return bridge != null && group != null && bridge.hasGroup(group);
    }

    public String prefix(String group) {
        return bridge == null || group == null ? null : bridge.prefix(group);
    }

    public String userPrefix(UUID uuid) {
        return bridge == null || uuid == null ? null : bridge.userPrefix(uuid);
    }

    public String userGroup(UUID uuid) {
        return bridge == null || uuid == null ? null : bridge.userGroup(uuid);
    }
}
