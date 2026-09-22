package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The ignore list behind {@code /ignorelist}. Every ignored name is a head;
 * clicking it stops ignoring that player, which is safe because ignoring them
 * again is one click away. Names are worked with as text, so someone can be
 * ignored while they are offline.
 */
public class IgnoreList_Gui {

    private final Better_Admin_Commands plugin;

    public IgnoreList_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        Set<String> ignored = new TreeSet<>(plugin.preferences().ignored(player.getUniqueId()));

        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("ignorelist.gui-rows", 6)));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(ignored.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Ignoring &7(" + ignored.size() + ")", rows);
        menu.fillEmpty(Items.filler());

        List<String> names = new ArrayList<>(ignored);
        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < names.size(); index++) {
            String name = names.get(start + index);
            menu.button(index, Items.head(uuidOf(name), "&7" + name,
                            "&7You are ignoring this player.",
                            "&7Their chat and private messages stay hidden.",
                            "",
                            "&eClick to stop ignoring"),
                    event -> {
                        boolean stillIgnored = plugin.preferences().toggleIgnore(player.getUniqueId(), name);
                        Msg.success(player, name + (stillIgnored ? " is ignored again." : " is no longer ignored."));
                        open(player, current);
                    });
        }
        if (ignored.isEmpty()) {
            menu.button((rows - 1) / 2 * 9 + 4, Items.of(Material.LIME_DYE, "&aYou are not ignoring anyone",
                    "&7Nobody is hidden from you right now.",
                    "",
                    "&7Use the button below to ignore someone."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> open(player, current - 1));
        }
        menu.button(nav + 2, Items.of(Material.PLAYER_HEAD, "&cIgnore a player",
                        "&7Pick someone online, or type a name.",
                        "",
                        "&eClick to choose"),
                event -> openPicker(player, 0));
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        menu.button(nav + 6, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> open(player, current + 1));
        }
        menu.open(player);
    }

    /* ---------------------------------------------------------- picker ---- */

    private void openPicker(Player player, int page) {
        List<Player> online = new ArrayList<>();
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                online.add(other);
            }
        }
        online.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));

        int rows = 5;
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(online.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Ignore a player", rows);
        menu.fillEmpty(Items.filler());

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < online.size(); index++) {
            Player other = online.get(start + index);
            boolean ignored = plugin.preferences().isIgnoring(player.getUniqueId(), other.getName());
            menu.button(index, Items.head(other.getUniqueId(),
                            (ignored ? "&7" : "&f") + other.getName(),
                            ignored ? "&7Currently ignored" : "&7Currently visible",
                            "",
                            ignored ? "&eClick to stop ignoring" : "&cClick to ignore"),
                    event -> toggle(player, other.getName(), current));
        }
        if (online.isEmpty()) {
            menu.button((rows - 1) / 2 * 9 + 4, Items.of(Material.GRAY_DYE, "&7Nobody else is online",
                    "&7Use the button below to type a name."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openPicker(player, current - 1));
        }
        menu.button(nav + 3, Items.of(Material.NAME_TAG, "&bType a name",
                        "&7For someone who is not online.",
                        "",
                        "&eClick to type a name"),
                event -> plugin.chatPrompts().request(player,
                        "&7Which player do you want to ignore?", answer -> {
                            if (answer.equalsIgnoreCase("cancel")) {
                                Msg.send(player, "&7Cancelled.");
                                open(player, 0);
                                return;
                            }
                            toggle(player, answer, current);
                        }));
        menu.button(nav + 5, Items.of(Material.BARRIER, "&cBack"), event -> open(player, 0));
        menu.button(nav + 7, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openPicker(player, current + 1));
        }
        menu.open(player);
    }

    private void toggle(Player player, String name, int returnPage) {
        boolean ignored = plugin.preferences().toggleIgnore(player.getUniqueId(), name);
        Msg.send(player, ignored
                ? "&7You are now ignoring &f" + name + "&7."
                : "&7You are no longer ignoring &f" + name + "&7.");
        openPicker(player, returnPage);
    }

    /**
     * The skin for a name, or {@code null} when no account is cached for it.
     * Only cached lookups are used, so opening the menu never waits for Mojang.
     */
    private static UUID uuidOf(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name.toLowerCase(Locale.ROOT));
        return cached == null ? null : cached.getUniqueId();
    }
}
