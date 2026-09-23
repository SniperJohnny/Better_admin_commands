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

/**
 * Central place for the plugin's permission checks and for the defaults every
 * player gets without a permission plugin.
 *
 * <p>Two things it solves that plain {@code hasPermission} cannot:</p>
 * <ul>
 *   <li><b>One node for everything.</b> Servers that do not want to hand out a
 *       hundred nodes can give a rank the single wildcard
 *       {@code better_admin_commands.permissionall}. It is accepted anywhere a
 *       specific node is checked, and - because the real nodes are attached to
 *       the player - it also satisfies the {@code permission:} entries declared
 *       in {@code plugin.yml}.</li>
 *   <li><b>Easy set-up without a permission plugin.</b> The
 *       {@code permissions.default-access} switch decides what everyone may use:
 *       {@code default} keeps the plugin.yml defaults (player commands for all,
 *       the rest for operators), {@code all} gives every command to everyone and
 *       {@code none} gives nothing. The per-group switches below it grant or
 *       revoke the {@code player}, {@code mod} and {@code admin} bundles for
 *       everyone.</li>
 * </ul>
 */
public class PermissionService {

    /** Player-facing bundle: homes, warps, economy, auction, mail, … */
    public static final String GROUP_PLAYER = "betteradmincommands.player";
    /** Moderation bundle: kick, ban, mute, jail, … */
    public static final String GROUP_MOD = "betteradmincommands.mod";
    /** Administration bundle: gamemode, give, eco, world tools, … */
    public static final String GROUP_ADMIN = "betteradmincommands.admin";
    /** The bundle that parents the three above, i.e. every command at once. */
    public static final String BUNDLE_ALL = "betteradmincommands.*";
    /** The name people asked for: one node that unlocks the whole plugin. */
    public static final String WILDCARD = "better_admin_commands.permissionall";

    private final Better_Admin_Commands plugin;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    /** Cached result of {@link #wildcards()}; {@code has} is called very often. */
    private volatile List<String> cachedWildcards;

    public PermissionService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    /** Drops the cached configuration, so the next check reads it again. */
    public void refresh() {
        cachedWildcards = null;
    }

    /* ------------------------------------------------------------- checks --- */

    /**
     * Whether the sender may use something. This is the check every command
     * added by this plugin uses (instead of {@code hasPermission}) so granting
     * the wildcard is enough to unlock everything.
     */
    public boolean has(CommandSender sender, String node) {
        return has(sender, node, false);
    }

    /** The same, treating "no permission given at all" as allowed. */
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

    /** Every configured node that means "everything", lowest priority last. */
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

    /** Whether a player has been given one of the wildcard nodes. */
    public boolean hasWildcard(CommandSender sender) {
        for (String node : wildcards()) {
            if (sender.hasPermission(node)) {
                return true;
            }
        }
        return false;
    }

    /** The configured default access level: {@code default}, {@code all} or {@code none}. */
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

    /** Whether the given bundle is handed to everyone in {@code default} mode. */
    public boolean groupEnabled(String group) {
        return plugin.getConfig().getBoolean("permissions.groups." + group, "player".equals(group));
    }

    /* -------------------------------------------------------- attachments --- */

    /**
     * Applies the configured defaults to a player. Called when they join, after a
     * reload and whenever a permission plugin changed what they hold. A player
     * holding a wildcard node gets every real node attached, so the
     * {@code plugin.yml} command permissions accept it too.
     */
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

    /** Re-applies the defaults to every online player. Used by a reload. */
    public void applyToAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyTo(player);
        }
    }

    /** Drops the attachment of a player who left. */
    public void clear(Player player) {
        if (player != null) {
            remove(player);
        }
    }

    /** Removes each stored attachment again, for example when the plugin stops. */
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
