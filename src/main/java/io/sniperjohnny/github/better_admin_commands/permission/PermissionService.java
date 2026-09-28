package io.sniperjohnny.github.better_admin_commands.permission;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionService {

    public static final String GROUP_PLAYER = "betteradmincommands.player";

    public static final String GROUP_MOD = "betteradmincommands.mod";

    public static final String GROUP_ADMIN = "betteradmincommands.admin";

    public static final String BUNDLE_ALL = "betteradmincommands.*";

    public static final String WILDCARD = "better_admin_commands.permissionall";

    private final Better_Admin_Commands plugin;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    private volatile List<String> cachedWildcards;

    public PermissionService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        cachedWildcards = null;
    }

    public boolean has(CommandSender sender, String node) {
        return has(sender, node, false);
    }

    public boolean has(CommandSender sender, String node, boolean orWhenEmpty) {
        if (sender == null) {
            return false;
        }
        if (node == null || node.isBlank()) {
            return orWhenEmpty;
        }
        if (sender.hasPermission(node)) {
            return true;
        }
        for (String wildcard : wildcards()) {
            if (sender.hasPermission(wildcard)) {
                return true;
            }
        }
        return false;
    }

    public List<String> wildcards() {
        List<String> cached = cachedWildcards;
        if (cached != null) {
            return cached;
        }
        List<String> nodes = new ArrayList<>();
        for (String configured : plugin.getConfig().getStringList("permissions.wildcards")) {
            if (configured != null && !configured.isBlank()) {
                nodes.add(configured.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (!nodes.contains(WILDCARD)) {
            nodes.add(WILDCARD);
        }
        if (!nodes.contains(BUNDLE_ALL)) {
            nodes.add(BUNDLE_ALL);
        }
        List<String> result = List.copyOf(nodes);
        cachedWildcards = result;
        return result;
    }

    public boolean hasWildcard(CommandSender sender) {
        for (String node : wildcards()) {
            if (sender.hasPermission(node)) {
                return true;
            }
        }
        return false;
    }

    public String accessMode() {
        String configured = plugin.getConfig().getString("permissions.default-access", "default");
        if (configured == null) {
            return "default";
        }
        String mode = configured.trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "all", "everyone", "true" -> "all";
            case "none", "disabled", "false", "op" -> "none";
            default -> "default";
        };
    }

    public boolean groupEnabled(String group) {
        return plugin.getConfig().getBoolean("permissions.groups." + group, "player".equals(group));
    }

    public void applyTo(Player player) {
        if (player == null) {
            return;
        }
        remove(player);
        PermissionAttachment attachment = player.addAttachment(plugin);
        if (hasWildcard(player)) {
            attachment.setPermission(BUNDLE_ALL, true);
        } else {
            switch (accessMode()) {
                case "all" -> attachment.setPermission(BUNDLE_ALL, true);
                case "none" -> {
                    attachment.setPermission(GROUP_PLAYER, false);
                    attachment.setPermission(GROUP_MOD, false);
                    attachment.setPermission(GROUP_ADMIN, false);
                }
                default -> {
                    attachment.setPermission(GROUP_PLAYER, groupEnabled("player"));
                    attachment.setPermission(GROUP_MOD, groupEnabled("mod"));
                    attachment.setPermission(GROUP_ADMIN, groupEnabled("admin"));
                }
            }
        }
        player.recalculatePermissions();
        attachments.put(player.getUniqueId(), attachment);
    }

    public void applyToAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyTo(player);
        }
    }

    public void clear(Player player) {
        if (player != null) {
            remove(player);
        }
    }

    public void clearAll() {
        for (UUID uuid : new ArrayList<>(attachments.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                remove(player);
            } else {
                attachments.remove(uuid);
            }
        }
    }

    private void remove(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment == null) {
            return;
        }
        try {
            player.removeAttachment(attachment);
        } catch (IllegalArgumentException ignored) {
            // The player already lost it (a reconnect does that), so there is
            // nothing left to take off.
        }
        player.recalculatePermissions();
    }
}
