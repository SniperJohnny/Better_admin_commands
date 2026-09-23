package io.sniperjohnny.github.better_admin_commands.trade;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Player to player trading, with a GUI at its core:
 *
 * <ul>
 *   <li>{@code /trade <player>} - ask for a trade (opens the window)</li>
 *   <li>{@code /trade accept [player]} - accept a request</li>
 *   <li>{@code /trade deny [player]} - decline a request</li>
 *   <li>{@code /trade log [player]} - the last trades, for staff or for yourself</li>
 * </ul>
 *
 * <p>The window itself is where items and money are added; the command covers
 * everything for players who prefer typing.</p>
 */
public class Trade_Command implements TabExecutor {

    /** Permission that may look at everyone's trade history. */
    public static final String LOG_OTHERS_PERMISSION = "betteradmincommands.trade.log.others";

    private static final List<String> SUBCOMMANDS = List.of("accept", "deny", "log", "help");

    private final Better_Admin_Commands plugin;

    public Trade_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            if (sender instanceof Player player && plugin.trades().isTrading(player.getUniqueId())) {
                Msg.send(sender, "&7You are in a trade - use the window to add items and confirm.");
                return true;
            }
            Msg.usage(sender, command);
            Msg.send(sender, "&7Use &f/trade <player> &7to start a trade, or &f/trade help&7.");
            return true;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        switch (first) {
            case "accept" -> {
                if (!(sender instanceof Player player)) {
                    Msg.playerOnly(sender);
                    return true;
                }
                plugin.trades().accept(player, args.length >= 2 ? args[1] : null);
                return true;
            }
            case "deny", "decline" -> {
                if (!(sender instanceof Player player)) {
                    Msg.playerOnly(sender);
                    return true;
                }
                plugin.trades().deny(player, args.length >= 2 ? args[1] : null);
                return true;
            }
            case "log", "history" -> {
                log(sender, args);
                return true;
            }
            case "help" -> {
                help(sender, command);
                return true;
            }
            default -> {
                // Not a subcommand, so it has to be a player name.
            }
        }

        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (!plugin.trades().enabled()) {
            Msg.error(sender, "Trading is disabled on this server.");
            return true;
        }
        Player target = Targets.online(sender, args[0]);
        if (target == null) {
            return true;
        }
        plugin.trades().request(player, target);
        return true;
    }

    /** Shows the newest trades of one player, or everyone's for staff. */
    private void log(CommandSender sender, String[] args) {
        if (plugin.tradeLogs() == null) {
            Msg.error(sender, "The trade history is not available.");
            return;
        }
        boolean others = plugin.permissions().has(sender, LOG_OTHERS_PERMISSION)
                || plugin.permissions().has(sender, "betteradmincommands.trade.staff");

        if (args.length >= 2 && !args[1].equalsIgnoreCase("me")) {
            if (!others) {
                Msg.noPermission(sender);
                return;
            }
            OfflinePlayer target = Targets.offline(sender, args[1]);
            if (target == null) {
                return;
            }
            show(sender, plugin.tradeLogs().recentFor(target.getUniqueId(), 10),
                    "Trades of " + target.getName());
            return;
        }

        if (!(sender instanceof Player player)) {
            if (!others) {
                Msg.error(sender, "From the console, name a player: /trade log <player>.");
                return;
            }
            show(sender, plugin.tradeLogs().recent(10), "The last trades");
            return;
        }
        if (!others) {
            show(sender, plugin.tradeLogs().recentFor(player.getUniqueId(), 10), "Your trades");
            return;
        }
        show(sender, plugin.tradeLogs().recent(10), "The last trades");
    }

    private void show(CommandSender sender, List<TradeLogService.TradeRecord> records, String title) {
        if (records.isEmpty()) {
            Msg.send(sender, "&7No trades found in the last &f"
                    + (plugin.tradeLogs().retentionMillis() / 3_600_000L) + " hour(s)&7.");
            return;
        }
        Msg.raw(sender, "&6" + title + " &7(last " + records.size() + ")");
        for (TradeLogService.TradeRecord record : records) {
            Msg.raw(sender, " &7" + Targets.formatDuration(
                    (System.currentTimeMillis() - record.createdAt()) / 1000L) + " ago: &f"
                    + record.firstName() + " &7\u2194 &f" + record.secondName());
            Msg.raw(sender, "   " + plugin.tradeLogs().summaryOf(record, false));
        }
    }

    private void help(CommandSender sender, Command command) {
        Msg.raw(sender, "&6Trade");
        Msg.raw(sender, " &f/trade <player> &7- ask to trade; the window opens for both");
        Msg.raw(sender, " &f/trade accept [player] &7- accept a request");
        Msg.raw(sender, " &f/trade deny [player] &7- decline a request");
        Msg.raw(sender, " &f/trade log [player] &7- the last trades");
        Msg.raw(sender, " &7In the window: put items on your side, press the gold button for money,");
        Msg.raw(sender, " &7then confirm. Both sides have to confirm before anything moves.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> choices = new ArrayList<>(SUBCOMMANDS);
            choices.addAll(Targets.onlineNames());
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), choices);
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (first.equals("log") && plugin.permissions().has(sender, LOG_OTHERS_PERMISSION)) {
                List<String> names = new ArrayList<>(Targets.onlineNames());
                names.add("me");
                return Targets.completeFrom(args[1], names);
            }
            return Targets.complete(args[1]);
        }
        return Collections.emptyList();
    }
}
