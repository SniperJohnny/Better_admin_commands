package io.sniperjohnny.github.better_admin_commands.commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.economy.EconomyService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Shows the richest players on the server. */
public class Baltop_Command implements TabExecutor {

    private static final int PAGE_SIZE = 10;

    private final Better_Admin_Commands plugin;

    public Baltop_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        int page = 1;
        if (args.length >= 1) {
            Integer parsed = Targets.parseInt(args[0]);
            if (parsed == null || parsed < 1) {
                Msg.error(sender, "The page has to be a positive number.");
                return true;
            }
            page = parsed;
        }

        List<EconomyService.BalanceEntry> entries = plugin.economy().top(1000);
        if (entries.isEmpty()) {
            Msg.error(sender, "There is no economy data yet.");
            return true;
        }
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        if (page > pages) {
            Msg.error(sender, "There are only " + pages + " page(s).");
            return true;
        }

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        Msg.raw(sender, "&6Richest players &7(page " + page + "/" + pages + ")&6:");
        for (int index = start; index < end; index++) {
            EconomyService.BalanceEntry entry = entries.get(index);
            Msg.raw(sender, " &8" + (index + 1) + ". &f" + entry.name() + " &7- &a"
                    + plugin.economy().format(entry.balance()));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "1", "2", "3");
        }
        return Collections.emptyList();
    }
}
