package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Optional hook into the TAB plugin.
 *
 * <p>TAB renders the tab list and the name tags from its own packets, so it
 * overwrites whatever the server sets on a player. This service lets {@code /nick}
 * drive TAB instead: the nickname and the rank prefix it is shown with are put
 * into the tab list for everyone, a borrowed LuckPerms group is applied for real
 * (sorting included), the name tag above a head is hidden as configured and the
 * vanish cue is drawn by TAB.</p>
 *
 * <p>Like {@link NickService}, the whole thing stays optional: the API is only
 * touched through {@link TabBridge}, which is created when TAB is installed and
 * left alone otherwise. Every method answers something sensible without it.</p>
 */
public class TabService {

    private final Better_Admin_Commands plugin;
    private TabBridge bridge;
    /** Errors are logged once, so a version mismatch cannot spam the console. */
    private boolean warned;

    public TabService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        if (plugin.getServer().getPluginManager().getPlugin("TAB") == null) {
            plugin.getLogger().info("TAB is not installed - the nickname, the rank prefix and the "
                    + "vanish cue are applied through Bukkit only, and the name tags above the "
                    + "players' heads are hidden with scoreboard teams.");
            return;
        }
        this.bridge = create();
        logState();
    }

    private TabBridge create() {
        try {
            return new TabBridge();
        } catch (Throwable e) {
            plugin.getLogger().warning("Could not hook into TAB: " + e.getMessage());
            return null;
        }
    }

    /**
     * The bridge, created late when TAB only became reachable after this plugin
     * started. Returns {@code null} when TAB is not installed or not reachable.
     */
    private TabBridge bridge() {
        if (bridge == null && plugin.getServer().getPluginManager().getPlugin("TAB") != null) {
            bridge = create();
            logState();
        }
        return bridge;
    }

    /** Says exactly which TAB features answer, so a wrong config is easy to spot. */
    private void logState() {
        if (bridge == null) {
            return;
        }
        plugin.getLogger().info("Hooked into TAB - tab list names: "
                + (bridge.tabListAvailable() ? "on" : "OFF (enable tablist-name-formatting in TAB's config)")
                + ", name tags: "
                + (bridge.nameTagsAvailable() ? "on" : "OFF (enable a name tag feature in TAB's config)")
                + ".");
    }

    /** Whether TAB is installed and its API can be reached. */
    public boolean available() {
        return bridge() != null;
    }

    /** Whether TAB can hide a player's name tag (its name tag feature is on). */
    public boolean nameTagsAvailable() {
        TabBridge active = bridge();
        return active != null && active.nameTagsAvailable();
    }

    /**
     * Hands a player's nickname and rank to TAB. Does nothing when TAB is absent.
     *
     * @param uuid     the player
     * @param nickname the nickname, or {@code null} to reset back to the real name
     * @param group    the borrowed LuckPerms group, or {@code null} for the own rank
     */
    public void apply(UUID uuid, String nickname, String group) {
        apply(uuid, nickname, group, "");
    }

    /**
     * The same, with the vanish cue that TAB puts in front of the player's rank
     * prefix while they are vanished (empty when they are not).
     */
    public void apply(UUID uuid, String nickname, String group, String vanishCue) {
        TabBridge active = bridge();
        if (active == null || uuid == null) {
            return;
        }
        boolean noPrefix = PlayerPreferences.PREFIX_NONE.equals(group);
        // A borrowed group is an actual group name; the own rank and "no prefix"
        // both leave TAB's own group detection in place.
        String temporaryGroup = (group == null || noPrefix) ? null : group;
        // The rank that belongs next to the nickname in the tab bar: the player's
        // own rank, or the group borrowed with /nick. It is resolved on every
        // call, so the tab bar shows the rank the nickname currently has.
        String rankPrefix = plugin.preferences().nicknamePrefix(uuid);
        boolean hideNameTags = plugin.preferences().hideNameTags();
        try {
            active.apply(uuid, nickname, temporaryGroup, noPrefix, vanishCue == null ? "" : vanishCue,
                    rankPrefix, hideNameTags);
        } catch (Throwable e) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("A TAB API call failed (" + e.getClass().getSimpleName()
                        + (e.getMessage() == null ? "" : ": " + e.getMessage())
                        + "). Check that the TAB version matches; naming still works through Bukkit.");
            }
        }
    }

    /**
     * Registers TAB's load events so a nickname survives TAB loading a player
     * late or being reloaded. TAB may finish with a player after the join event,
     * so this is what actually makes the nickname stick on some servers.
     */
    public void registerEvents() {
        TabBridge active = bridge();
        if (active == null) {
            return;
        }
        active.registerLoad(uuid -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (uuid == null) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    plugin.preferences().applyNickname(player);
                }
                return;
            }
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                plugin.preferences().applyNickname(player);
            }
        }));
    }
}
