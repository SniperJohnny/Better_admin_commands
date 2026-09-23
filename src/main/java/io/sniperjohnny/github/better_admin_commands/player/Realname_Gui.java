package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The nickname overview behind {@code /realname}: everyone online who is using a
 * nickname, with the real account name underneath, and a lookup button for
 * asking about one nickname directly.
 *
 * <p>The list answers the usual question ("who is that player?") without typing
 * anything, which is what {@code /realname} alone cannot do - it needs the
 * nickname up front.</p>
 */
public class Realname_Gui {

    private final Better_Admin_Commands plugin;

    public Realname_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        List<Player> nicknamed = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (plugin.preferences().nickname(online.getUniqueId()) != null) {
                nicknamed.add(online);
            }
        }
        nicknamed.sort((first, second) ->
                plugin.preferences().nickname(first.getUniqueId())
                        .compareToIgnoreCase(plugin.preferences().nickname(second.getUniqueId())));

        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("realname.gui-rows", 6)));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(nicknamed.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Nicknames &7(" + nicknamed.size() + ")", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < nicknamed.size(); index++) {
            Player target = nicknamed.get(start + index);
            String nickname = plugin.preferences().nickname(target.getUniqueId());
            menu.button(index, Items.head(target.getUniqueId(), nickname,
                    "&7Real name: &f" + target.getName(),
                    "&7Shown as: &f" + nickname,
                    "",
                    "&eLeft-click to look this name up",
                    "&7Right-click to see their balance"),
                    event -> {
                        if (event.isRightClick()) {
                            showBalance(player, target);
                            return;
                        }
                        player.closeInventory();
                        Msg.send(player, "&f" + nickname + " &7is &f" + target.getName() + "&7.");
                    });
        }
        if (nicknamed.isEmpty()) {
            menu.button((rows - 1) / 2 * 9 + 4, Items.of(Material.GRAY_DYE, "&7Nobody is using a nickname",
                    "&7No online player has set one.",
                    "",
                    "&7Set one with &f/nick <name>&7."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> open(player, current - 1));
        }
        menu.button(nav + 2, Items.of(Material.NAME_TAG, "&bLook up a nickname",
                        "&7Type a nickname to find the account.",
                        "",
                        "&eClick to look one up"),
                event -> plugin.chatPrompts().request(player,
                        "&7Which nickname do you want to look up?", answer -> {
                            if (answer.equalsIgnoreCase("cancel")) {
                                Msg.send(player, "&7Cancelled.");
                                open(player, current);
                                return;
                            }
                            lookup(player, answer, current);
                        }));
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages,
                "&7Nicknamed players online: &f" + nicknamed.size()));
        menu.button(nav + 6, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> open(player, current + 1));
        }
        menu.open(player);
    }

    /** The same answer {@code /realname <nickname>} gives, so both agree. */
    private void lookup(Player player, String query, int returnPage) {
        List<String> matches = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            String nickname = plugin.preferences().nickname(online.getUniqueId());
            if (nickname != null && nickname.equalsIgnoreCase(query)) {
                matches.add(online.getName());
            }
        }
        if (matches.isEmpty()) {
            Msg.send(player, "&7Nobody is using the nickname &f" + query + "&7.");
        } else {
            Msg.send(player, "&f" + query + " &7is &f" + String.join("&7, &f", matches) + "&7.");
        }
        open(player, returnPage);
    }

    private void showBalance(Player player, Player target) {
        if (!player.hasPermission("betteradmincommands.balance.others")) {
            Msg.noPermission(player);
            return;
        }
        Msg.send(player, "&7Balance of &f" + target.getName() + "&7: &a"
                + plugin.economy().format(plugin.economy().getBalance(target.getUniqueId())));
    }
}
