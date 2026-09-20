package io.sniperjohnny.github.better_admin_commands.commands.moderation;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Shows every ban, both names and IPs. */
public class Banlist_Command implements TabExecutor {

    @Override
    @SuppressWarnings("rawtypes")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        int count = 0;

        Msg.raw(sender, "&6Banned players:");
        count += print(sender, Bukkit.getBanList(BanList.Type.NAME), format);

        Msg.raw(sender, "&6Banned IPs:");
        count += print(sender, Bukkit.getBanList(BanList.Type.IP), format);

        Msg.raw(sender, "&7Total: &f" + count + " &7ban(s).");
        return true;
    }

    @SuppressWarnings("rawtypes")
    private int print(CommandSender sender, BanList banList, SimpleDateFormat format) {
        int count = 0;
        for (Object object : banList.getEntries()) {
            if (!(object instanceof BanEntry entry)) {
                continue;
            }
            Date expires = entry.getExpiration();
            Msg.raw(sender, " &8- &f" + entry.getTarget() + " &7by &f" + entry.getSource()
                    + " &7(" + (expires == null ? "permanent" : "until " + format.format(expires)) + ")"
                    + " &8- &7" + entry.getReason());
            count++;
        }
        if (count == 0) {
            Msg.raw(sender, " &8- &7none");
        }
        return count;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
