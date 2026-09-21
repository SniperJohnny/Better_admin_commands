package io.sniperjohnny.github.better_admin_commands.player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every direct call into the LuckPerms API lives in this class.
 *
 * <p>It is deliberately kept apart from {@link NickService}: that class only
 * instantiates this one when the LuckPerms plugin is actually installed, so the
 * API classes are never loaded - and never missed - on a server without
 * LuckPerms.</p>
 *
 * <p>Only reading happens here. Groups and their prefixes are inspected, the
 * plugin never changes a player's permissions or group.</p>
 */
class LuckPermsBridge {

    private final LuckPerms api = LuckPermsProvider.get();

    /** Names of every group LuckPerms has loaded, sorted, for tab completion. */
    List<String> groupNames() {
        List<String> names = new ArrayList<>();
        for (Group group : api.getGroupManager().getLoadedGroups()) {
            names.add(group.getName().toLowerCase(Locale.ROOT));
        }
        names.sort(String::compareTo);
        return names;
    }

    /** Whether a group with this name is loaded. */
    boolean hasGroup(String name) {
        return api.getGroupManager().getGroup(name) != null;
    }

    /** The prefix configured on a group, or {@code null} when it has none. */
    String prefix(String name) {
        Group group = api.getGroupManager().getGroup(name);
        if (group == null) {
            return null;
        }
        String prefix = group.getCachedData().getMetaData().getPrefix();
        return prefix == null || prefix.isBlank() ? null : prefix;
    }
}
