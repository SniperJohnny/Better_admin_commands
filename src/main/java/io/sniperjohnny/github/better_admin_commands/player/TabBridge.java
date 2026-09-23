package io.sniperjohnny.github.better_admin_commands.player;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Every direct call into the TAB API lives in this class.
 *
 * <p>It is deliberately kept apart from {@link TabService}: that class only
 * instantiates this one when the TAB plugin is actually installed, so the API
 * classes are never loaded - and never missed - on a server without TAB.</p>
 *
 * <p>TAB is the plugin that owns the tab list and the name tags, so instead of
 * fighting it over the same packets {@code /nick} hands the nickname to TAB:
 * the tab list name is replaced, a borrowed LuckPerms group is applied through
 * {@code setTemporaryGroup} (so sorting and the group's own prefix behave as if
 * the player really held that rank), and the real name above a nicked player's
 * head is hidden with the name tag manager.</p>
 */
class TabBridge {

    private final TabAPI api = TabAPI.getInstance();

    /** Whether TAB's tab list name formatting is enabled and can be driven. */
    boolean tabListAvailable() {
        return api.getTabListFormatManager() != null;
    }

    /** Whether TAB's name tag feature is enabled and can hide a name tag. */
    boolean nameTagsAvailable() {
        return api.getNameTagManager() != null;
    }

    /** The TAB representation of a player, or {@code null} while TAB has not loaded them. */
    TabPlayer player(UUID uuid) {
        try {
            return api.getPlayer(uuid);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Applies a player's nickname and rank to TAB.
     *
     * @param uuid         the player
     * @param nickname     the nickname, or {@code null} to reset back to the real name
     * @param group        the borrowed LuckPerms group, or {@code null} for the own rank
     * @param noPrefix     {@code true} when the player asked for no prefix at all
     * @param vanishCue    the vanish marker to put in front of the prefix, or empty
     * @param rankPrefix   the rank shown next to the nickname (the own rank, or the
     *                     group borrowed with {@code /nick}), as a legacy string
     * @param hideNameTags {@code true} when the name tag above the head has to be
     *                     hidden for everyone
     */
    void apply(UUID uuid, String nickname, String group, boolean noPrefix, String vanishCue,
               String rankPrefix, boolean hideNameTags) {
        TabPlayer player = player(uuid);
        if (player == null) {
            // TAB processes players asynchronously, so a join event can arrive
            // before TAB knows the player. The PlayerLoadEvent re-applies this.
            return;
        }
        // Borrowing a group makes TAB treat the player as if they really held
        // that rank - that is what keeps a staff member nicked as "player" from
        // being sorted to the top of the tab list.
        player.setTemporaryGroup(group);

        String cue = vanishCue == null ? "" : vanishCue;
        boolean vanished = !cue.isEmpty();
        String ownPrefix = rankPrefix == null ? "" : rankPrefix;
        // While a nickname is set (or the prefix was dropped with `/nick <nick> off`)
        // the tab list entry is written by us, so the rank next to the nickname is
        // the rank the nickname currently has - the own rank, or a group borrowed
        // with `/nick <nickname> <group>`. When the plugin cannot resolve a rank
        // (for example on a TAB server without LuckPerms) TAB's own configured
        // prefix is kept; without a nickname TAB's formatting stays untouched.
        boolean ownFormat = nickname != null || noPrefix;

        TabListFormatManager formats = api.getTabListFormatManager();
        if (formats != null) {
            String tabOwnPrefix = formats.getOriginalReplacedPrefix(player);
            String resolved = noPrefix ? ""
                    : (ownPrefix.isEmpty() ? tabOwnPrefix : ownPrefix);
            formats.setName(player, nickname);
            if (ownFormat) {
                formats.setPrefix(player, cue + resolved);
            } else if (vanished) {
                // The cue goes in front of the rank prefix TAB would show, so a
                // vanished player keeps their rank and is marked as vanished.
                formats.setPrefix(player, cue + tabOwnPrefix);
            } else {
                // null resets the prefix to what TAB would show on its own.
                formats.setPrefix(player, null);
            }
        }

        NameTagManager nameTags = api.getNameTagManager();
        if (nameTags == null) {
            return;
        }
        if (hideNameTags) {
            // The configured setup: no name tags above anyone's head.
            nameTags.hideNameTag(player);
            return;
        }
        String nameTagRank = noPrefix ? ""
                : (ownPrefix.isEmpty() ? nameTags.getOriginalReplacedPrefix(player) : ownPrefix);
        if (vanished) {
            nameTags.setPrefix(player, cue + nameTagRank);
        } else {
            nameTags.setPrefix(player, ownFormat ? nameTagRank : null);
        }
        if (nickname != null) {
            // TAB cannot change the name part of a name tag (it is the profile
            // name), so a nicked player simply gets no name tag at all instead of
            // one showing the real name.
            nameTags.hideNameTag(player);
        } else {
            nameTags.showNameTag(player);
        }
    }

    /**
     * Registers TAB's own events so the nickname is applied even when TAB only
     * finishes loading a player after the join, and re-applied after a
     * {@code /tab reload}.
     *
     * @param onLoad receives the player UUID, or {@code null} to re-apply everyone
     */
    void registerLoad(Consumer<UUID> onLoad) {
        if (api.getEventBus() == null) {
            return;
        }
        api.getEventBus().register(PlayerLoadEvent.class, event -> {
            TabPlayer player = event.getPlayer();
            if (player != null) {
                onLoad.accept(player.getUniqueId());
            }
        });
        api.getEventBus().register(TabLoadEvent.class, event -> onLoad.accept(null));
    }
}
