package io.sniperjohnny.github.better_admin_commands.player;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;

import java.util.UUID;
import java.util.function.Consumer;

class TabBridge {

    private final TabAPI api = TabAPI.getInstance();

    boolean tabListAvailable() {
        return api.getTabListFormatManager() != null;
    }

    boolean nameTagsAvailable() {
        return api.getNameTagManager() != null;
    }

    TabPlayer player(UUID uuid) {
        try {
            return api.getPlayer(uuid);
        } catch (RuntimeException e) {
            return null;
        }
    }

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
