package io.sniperjohnny.github.better_admin_commands.player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

class LuckPermsBridge {

    private final LuckPerms api = LuckPermsProvider.get();

    List<String> groupNames() {
        List<String> names = new ArrayList<>();
        for (Group group : api.getGroupManager().getLoadedGroups()) {
            names.add(group.getName().toLowerCase(Locale.ROOT));
        }
        names.sort(String::compareTo);
        return names;
    }

    boolean hasGroup(String name) {
        return api.getGroupManager().getGroup(name) != null;
    }

    String prefix(String name) {
        Group group = api.getGroupManager().getGroup(name);
        if (group == null) {
            return null;
        }
        String prefix = group.getCachedData().getMetaData().getPrefix();
        return prefix == null || prefix.isBlank() ? null : prefix;
    }

    String userGroup(UUID uuid) {
        User user = api.getUserManager().getUser(uuid);
        if (user == null) {
            return null;
        }
        String primary = user.getPrimaryGroup();
        return primary == null || primary.isBlank() ? null : primary;
    }

    String userPrefix(UUID uuid) {
        User user = api.getUserManager().getUser(uuid);
        if (user == null) {
            return null;
        }
        String prefix = user.getCachedData().getMetaData().getPrefix();
        if (prefix != null && !prefix.isBlank()) {
            return prefix;
        }
        String primary = user.getPrimaryGroup();
        if (primary == null || primary.isBlank()) {
            return null;
        }
        Group group = api.getGroupManager().getGroup(primary);
        if (group == null) {
            return null;
        }
        prefix = group.getCachedData().getMetaData().getPrefix();
        return prefix == null || prefix.isBlank() ? null : prefix;
    }
}
