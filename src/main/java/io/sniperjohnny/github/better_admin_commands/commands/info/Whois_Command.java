package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Shows information about a player. */
public class Whois_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Whois_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        OfflinePlayer target;
        if (args.length >= 1) {
            target = Targets.offline(sender, args[0]);
            if (target == null) {
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            Msg.playerOnly(sender);
            return true;
        }

        String name = target.getName() == null ? "unknown" : target.getName();
        String nickname = plugin.preferences().nickname(target.getUniqueId());
        Msg.raw(sender, "&6--- " + name + " ---");
        Msg.raw(sender, " &7UUID: &f" + target.getUniqueId());
        if (nickname != null) {
            Msg.raw(sender, " &7Nickname: &f" + nickname);
        }
        Msg.raw(sender, " &7Status: &f" + (target.isOnline() ? "online" : "offline"));
        Msg.raw(sender, " &7Balance: &a" + plugin.economy().format(plugin.economy().getBalance(target.getUniqueId())));
        Msg.raw(sender, " &7Homes: &f" + plugin.homes().homesOf(target.getUniqueId()).size());

        boolean muted = plugin.mutes().isMuted(target.getUniqueId());
        Msg.raw(sender, " &7Muted: &f" + (muted ? "yes" : "no"));
        Msg.raw(sender, " &7Banned: &f" + isBanned(name));

        Player online = target.getPlayer();
        if (online != null) {
            Msg.raw(sender, " &7World: &f" + online.getWorld().getName());
            Msg.raw(sender, " &7Gamemode: &f" + online.getGameMode().name().toLowerCase(Locale.ROOT));
            Msg.raw(sender, " &7Health: &f" + Math.round(online.getHealth()) + " &7Food: &f" + online.getFoodLevel());
            Msg.raw(sender, " &7Ping: &f" + online.getPing() + "ms");
        } else {
            Long lastSeen = plugin.database().lastSeen(target.getUniqueId());
            if (lastSeen != null) {
                Msg.raw(sender, " &7Last seen: &f"
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(lastSeen)));
            }
        }
        return true;
    }

    private String isBanned(String name) {
        return org.bukkit.Bukkit.getBanList(org.bukkit.BanList.Type.NAME).isBanned(name) ? "yes" : "no";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        return Collections.emptyList();
    }
}
