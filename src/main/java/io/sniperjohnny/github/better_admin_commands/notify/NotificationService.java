package io.sniperjohnny.github.better_admin_commands.notify;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationService {

    public record Category(String id, String display, String permission, boolean defaultOn) {
    }

    private static final List<Category> DEFAULTS = List.of(
            new Category("trade", "&6Trade alerts", "betteradmincommands.trade.notify", true),
            new Category("report", "&bReport updates", "betteradmincommands.report.staff", true),
            new Category("kick", "&eKick notices", "betteradmincommands.kick.notify", true),
            new Category("ban", "&cBan notices", "betteradmincommands.ban.notify", true),
            new Category("unban", "&aUnban notices", "betteradmincommands.unban.notify", true),
            new Category("mail", "&dMail reminders", "betteradmincommands.mail", true),
            new Category("admin", "&6Admin usage", "betteradmincommands.admin.notify", true));

    private final Better_Admin_Commands plugin;

    private volatile List<Category> cachedCategories;

    public NotificationService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        cachedCategories = null;
    }

    public List<Category> categories() {
        List<Category> cached = cachedCategories;
        if (cached != null) {
            return cached;
        }
        ConfigurationSection section = plugin.getConfig()
                .getConfigurationSection("notifications.categories");
        if (section == null) {
            return DEFAULTS;
        }
        Map<String, Category> byId = new LinkedHashMap<>();
        for (Category category : DEFAULTS) {
            byId.put(category.id(), category);
        }
        List<Category> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            Category fallback = byId.get(id.toLowerCase(Locale.ROOT));
            String display = section.getString(id + ".display",
                    fallback == null ? "&7" + id : fallback.display());
            String permission = section.getString(id + ".permission",
                    fallback == null ? "" : fallback.permission());
            boolean defaultOn = section.getBoolean(id + ".default",
                    fallback == null || fallback.defaultOn());
            result.add(new Category(id.toLowerCase(Locale.ROOT), display,
                    permission == null ? "" : permission, defaultOn));
        }
        // Anything shipped but not mentioned in the config stays available.
        for (Category category : DEFAULTS) {
            boolean present = result.stream().anyMatch(entry -> entry.id().equals(category.id()));
            if (!present) {
                result.add(category);
            }
        }
        List<Category> copy = List.copyOf(result);
        cachedCategories = copy;
        return copy;
    }

    public Category category(String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.toLowerCase(Locale.ROOT);
        for (Category category : categories()) {
            if (category.id().equals(wanted)) {
                return category;
            }
        }
        return null;
    }

    public boolean toggleEnabled(Player player, Category category) {
        if (player == null || category == null) {
            return false;
        }
        return plugin.preferences().notificationEnabled(player.getUniqueId(),
                category.id(), category.defaultOn());
    }

    public boolean enabled(Player player, String categoryId) {
        Category category = category(categoryId);
        if (player == null || category == null) {
            return false;
        }
        if (!category.permission().isBlank()
                && !plugin.permissions().has(player, category.permission())) {
            return false;
        }
        return toggleEnabled(player, category);
    }

    public boolean toggle(Player player, String categoryId, Boolean force) {
        Category category = category(categoryId);
        if (player == null || category == null) {
            return false;
        }
        boolean current = toggleEnabled(player, category);
        boolean value = force == null ? !current : force;
        plugin.preferences().setNotification(player.getUniqueId(), category.id(), value);
        return value;
    }

    public void reset(Player player, String categoryId) {
        Category category = category(categoryId);
        if (player != null && category != null) {
            plugin.preferences().setNotification(player.getUniqueId(), category.id(), null);
        }
    }

    public void send(Player player, String categoryId, String message) {
        if (enabled(player, categoryId)) {
            Msg.send(player, message);
        }
    }

    public void broadcast(String categoryId, String permission, String message) {
        broadcast(categoryId, permission, message, null);
    }

    public void broadcast(String categoryId, String permission, String message, Player exclude) {
        Category category = category(categoryId);
        String node = permission != null && !permission.isBlank()
                ? permission
                : (category == null ? null : category.permission());
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.equals(exclude)) {
                continue;
            }
            if (node != null && !node.isBlank() && !plugin.permissions().has(online, node)) {
                continue;
            }
            if (category != null && !toggleEnabled(online, category)) {
                continue;
            }
            Msg.send(online, message);
        }
    }

    public static void staffBroadcast(String categoryId, String permission, String message) {
        Better_Admin_Commands plugin = Better_Admin_Commands.get_Instance();
        if (plugin == null || plugin.notifications() == null) {
            return;
        }
        plugin.notifications().broadcast(categoryId, permission, message);
    }
}
