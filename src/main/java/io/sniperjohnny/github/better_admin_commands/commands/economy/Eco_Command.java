package io.sniperjohnny.github.better_admin_commands.commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.economy.EconomyService;
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
import java.util.UUID;

/**
 * The administrative economy command, modelled on XConomy.
 *
 * <ul>
 *   <li>{@code /eco give|add <player> <amount>}</li>
 *   <li>{@code /eco take|remove|reduce <player> <amount>}</li>
 *   <li>{@code /eco set <player> <amount>}</li>
 *   <li>{@code /eco reset <player>} and {@code /eco resetall}</li>
 *   <li>{@code /eco balance <player>}</li>
 *   <li>{@code /eco top [page]}</li>
 * </ul>
 *
 * <p>The same logic is reachable through {@code /money …} and
 * {@code /balance …}, which are aliases of the balance command, so the usual
 * {@code /money set player 1000} works. Every action needs
 * {@code betteradmincommands.eco}, or the finer
 * {@code betteradmincommands.eco.<action>} node.</p>
 */
public class Eco_Command implements TabExecutor {

    /** Subcommands that mean "administrative action" rather than a player name. */
    private static final List<String> ACTIONS = List.of("give", "add", "take", "remove", "reduce",
            "set", "reset", "resetall", "balance", "bal", "top", "help");

    private final Better_Admin_Commands plugin;

    public Eco_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!admin(sender, command, args)) {
            Msg.usage(sender, command);
        }
        return true;
    }

    /**
     * Runs the economic subcommand in {@code args}, if it is one.
     *
     * @return {@code true} when the arguments named a subcommand and were handled
     *         (even if they were wrong), {@code false} when the first argument is
     *         not an economy action at all - the balance command uses that to
     *         tell {@code /money set …} from {@code /money Steve}
     */
    public boolean admin(CommandSender sender, Command command, String[] args) {
        if (args.length < 1) {
            return false;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (!ACTIONS.contains(action)) {
            return false;
        }

        switch (action) {
            case "help" -> help(sender, command);
            case "give", "add" -> change(sender, args, "give", true);
            case "take", "remove", "reduce" -> change(sender, args, "take", false);
            case "set" -> set(sender, args);
            case "reset" -> reset(sender, args);
            case "resetall" -> resetAll(sender);
            case "balance", "bal" -> balance(sender, args);
            case "top" -> topOf(sender, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    /* ------------------------------------------------------------ actions --- */

    private void help(CommandSender sender, Command command) {
        Msg.raw(sender, "&6Economy commands");
        Msg.raw(sender, " &f/" + command.getName() + " give <player> <amount> &7- add money");
        Msg.raw(sender, " &f/" + command.getName() + " take <player> <amount> &7- remove money");
        Msg.raw(sender, " &f/" + command.getName() + " set <player> <amount> &7- set a balance");
        Msg.raw(sender, " &f/" + command.getName() + " reset <player> &7- back to the starting balance");
        Msg.raw(sender, " &f/" + command.getName() + " resetall &7- reset every account");
        Msg.raw(sender, " &f/" + command.getName() + " balance [player] &7- show a balance");
        Msg.raw(sender, " &f/" + command.getName() + " top [page] &7- the richest players");
        Msg.raw(sender, " &7Also available as &f/balance &7(alias &f/money&7) and &f/eco&7.");
    }

    private void change(CommandSender sender, String[] args, String action, boolean add) {
        if (!may(sender, action)) {
            return;
        }
        OfflinePlayer target = target(sender, args);
        if (target == null) {
            return;
        }
        Double amount = amount(sender, args);
        if (amount == null) {
            return;
        }
        EconomyService service = plugin.economy();
        if (add) {
            service.deposit(target.getUniqueId(), amount);
        } else if (!service.withdraw(target.getUniqueId(), amount)) {
            Msg.error(sender, target.getName() + " only has "
                    + service.format(service.getBalance(target.getUniqueId())) + ".");
            return;
        }
        service.saveAsync();
        Msg.success(sender, (add ? "Gave " : "Took ") + service.format(amount)
                + (add ? " to " : " from ") + target.getName()
                + ". New balance: " + service.format(service.getBalance(target.getUniqueId())) + ".");
    }

    private void set(CommandSender sender, String[] args) {
        if (!may(sender, "set")) {
            return;
        }
        OfflinePlayer target = target(sender, args);
        if (target == null) {
            return;
        }
        Double amount = amount(sender, args);
        if (amount == null) {
            return;
        }
        EconomyService service = plugin.economy();
        service.setBalance(target.getUniqueId(), amount);
        service.saveAsync();
        Msg.success(sender, "Set the balance of " + target.getName() + " to "
                + service.format(service.getBalance(target.getUniqueId())) + ".");
    }

    private void reset(CommandSender sender, String[] args) {
        if (!may(sender, "reset")) {
            return;
        }
        OfflinePlayer target = target(sender, args);
        if (target == null) {
            return;
        }
        EconomyService service = plugin.economy();
        service.setBalance(target.getUniqueId(), service.startingBalance());
        service.saveAsync();
        Msg.success(sender, "Reset the balance of " + target.getName() + " to "
                + service.format(service.getBalance(target.getUniqueId())) + ".");
    }

    private void resetAll(CommandSender sender) {
        if (!may(sender, "reset")) {
            return;
        }
        EconomyService service = plugin.economy();
        List<UUID> accounts = new ArrayList<>(service.accountIds());
        for (UUID uuid : accounts) {
            service.setBalance(uuid, service.startingBalance());
        }
        service.saveAsync();
        Msg.success(sender, "Reset " + accounts.size() + " balance(s) to "
                + service.format(service.startingBalance()) + ".");
    }

    private void balance(CommandSender sender, String[] args) {
        EconomyService service = plugin.economy();
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                Msg.error(sender, "Name a player, or run this as a player.");
                return;
            }
            Msg.send(sender, "&7Your balance: &a" + service.format(service.getBalance(player.getUniqueId())));
            return;
        }
        if (!may(sender, "balance")) {
            return;
        }
        OfflinePlayer target = Targets.offline(sender, args[1]);
        if (target == null) {
            return;
        }
        Msg.send(sender, "&7Balance of &f" + target.getName() + "&7: &a"
                + service.format(service.getBalance(target.getUniqueId())));
    }

    private void topOf(CommandSender sender, String[] args) {
        if (!may(sender, "top")) {
            return;
        }
        int perPage = 10;
        int page = 1;
        if (args.length >= 2) {
            Integer parsed = Targets.parseInt(args[1]);
            if (parsed == null || parsed < 1) {
                Msg.error(sender, "The page has to be a positive number.");
                return;
            }
            page = parsed;
        }
        EconomyService service = plugin.economy();
        List<EconomyService.BalanceEntry> entries = service.top(page * perPage);
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) perPage));
        if (page > pages) {
            Msg.error(sender, "There is no page " + page + " - the leaderboard has " + pages + ".");
            return;
        }
        Msg.raw(sender, "&6Richest players &7(page &f" + page + "&7/&f" + pages + "&7)");
        int start = (page - 1) * perPage;
        for (int index = start; index < Math.min(entries.size(), start + perPage); index++) {
            EconomyService.BalanceEntry entry = entries.get(index);
            Msg.raw(sender, " &7" + (index + 1) + ". &f" + entry.name()
                    + " &7- &a" + service.format(entry.balance()));
        }
    }

    /* ------------------------------------------------------------ helpers --- */

    /** Whether the sender may run an economic action. */
    private boolean may(CommandSender sender, String action) {
        if (plugin.permissions().has(sender, "betteradmincommands.eco")
                || plugin.permissions().has(sender, "betteradmincommands.eco." + action)) {
            return true;
        }
        Msg.noPermission(sender);
        return false;
    }

    private OfflinePlayer target(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.error(sender, "Name the player it is about.");
            return null;
        }
        return Targets.offline(sender, args[1]);
    }

    private Double amount(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.error(sender, "You have to give an amount.");
            return null;
        }
        Double amount = Targets.parseDouble(args[2]);
        if (amount == null || amount < 0) {
            Msg.error(sender, "The amount has to be a positive number.");
            return null;
        }
        return amount;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], ACTIONS);
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (action.equals("top")) {
                return Targets.completeFrom(args[1], "1", "2", "3");
            }
            return Targets.complete(args[1]);
        }
        if (args.length == 3 && (action.equals("give") || action.equals("add")
                || action.equals("take") || action.equals("remove") || action.equals("reduce")
                || action.equals("set"))) {
            return Targets.completeFrom(args[2], "100", "1000", "10000");
        }
        return Collections.emptyList();
    }
}
