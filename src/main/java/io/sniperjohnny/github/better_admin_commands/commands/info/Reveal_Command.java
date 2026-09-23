package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Staff lookup behind a nickname: {@code /reveal <name|nickname>} answers with the
 * original account name and the real LuckPerms rank of an online player.
 *
 * <p>Unlike the automatic reveal that used to sit in chat, this is a deliberate,
 * permission-gated command - nobody sees the real name by accident.</p>
 */
public class Reveal_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Reveal_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        String query = args[0];
        Player target = findOnline(query);
        if (target == null) {
            Msg.error(sender, "No online player matches \"" + query + "\".");
            return true;
        }
        String group = plugin.nicks().userGroup(target.getUniqueId());
        String prefix = plugin.nicks().userPrefix(target.getUniqueId());
        String nickname = plugin.preferences().nickname(target.getUniqueId());

        Msg.raw(sender, "&6Reveal &7- &f" + query);
        Msg.raw(sender, " &7Real name: &f" + target.getName());
        Msg.raw(sender, " &7Rank: &f" + (group == null ? "none" : group)
                + (prefix == null || prefix.isBlank() ? "" : " &8(" + prefix + "&8)"));
        if (nickname != null) {
            Msg.raw(sender, " &7Nickname: &f" + nickname);
        }
        return true;
    }

    /** Finds an online player by their real name or their nickname. */
    private Player findOnline(String query) {
        String wanted = plain(query);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(query)) {
                return online;
            }
            String nickname = plugin.preferences().nickname(online.getUniqueId());
            if (nickname != null && plain(nickname).equalsIgnoreCase(wanted)) {
                return online;
            }
        }
        return null;
    }

    /** Strips colour codes, so a coloured nickname still matches what was typed. */
    private static String plain(String value) {
        return value == null ? "" : value.replaceAll("(?i)[&\u00a7][0-9a-fk-or]", "").trim();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
                String nickname = plugin.preferences().nickname(online.getUniqueId());
                if (nickname != null) {
                    names.add(nickname);
                }
            }
            return Targets.completeFrom(args[0].toLowerCase(), names);
        }
        return Collections.emptyList();
    }
}
