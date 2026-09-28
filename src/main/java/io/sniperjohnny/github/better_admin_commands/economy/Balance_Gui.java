package io.sniperjohnny.github.better_admin_commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.DialogPromptService;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Balance_Gui {

    private final Better_Admin_Commands plugin;

    public Balance_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Menu menu = new Menu("&8Your balance", 3);
        menu.frame();

        menu.button(13, Items.of(Material.SUNFLOWER, "&6Your balance",
                "&7You have &a" + plugin.economy().format(plugin.economy().getBalance(player.getUniqueId())),
                "",
                "&8Name: &7" + player.getName()));

        if (plugin.economy().paymentsAllowed() && player.hasPermission("betteradmincommands.pay")) {
            menu.button(11, Items.of(Material.GOLD_INGOT, "&aSend money",
                            "&7Pick a player and type the amount.",
                            "&7Online and offline players both work.",
                            "",
                            "&eClick to choose a player"),
                    event -> openReceivers(player, 0));
        } else {
            menu.button(11, Items.of(Material.GRAY_DYE, "&7Payments are disabled"));
        }

        menu.button(15, Items.of(Material.DIAMOND, "&bRichest players",
                        "&7See how you compare with the server.",
                        "",
                        "&eClick to open"),
                event -> plugin.baltopGui().open(player, 0));
        menu.button(22, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        menu.open(player);
    }

    private void openReceivers(Player player, int page) {
        List<Player> receivers = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
                receivers.add(online);
            }
        }
        receivers.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));

        int rows = 4;
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(receivers.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Send money to...", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < receivers.size(); index++) {
            Player receiver = receivers.get(start + index);
            menu.button(index, Items.head(receiver.getUniqueId(), "&f" + receiver.getName(),
                            "&7Balance: &a" + plugin.economy().format(
                                    plugin.economy().getBalance(receiver.getUniqueId())),
                            "",
                            "&eClick to send them money"),
                    event -> promptAmount(player, receiver));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openReceivers(player, current - 1));
        }
        // Paying someone who is offline: the receiver is typed in by name.
        menu.button(nav + 2, Items.of(Material.NAME_TAG, "&aPay an offline player",
                        "&7Someone who is not online right now.",
                        "",
                        "&eClick to type their name"),
                event -> promptName(player));
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages,
                receivers.isEmpty() ? "&7Nobody else is online - pay an offline player instead."
                        : "&7Pick a player from the heads above."));
        menu.button(nav + 6, Items.of(Material.BARRIER, "&cBack"), event -> open(player));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openReceivers(player, current + 1));
        }
        menu.open(player);
    }

    private void promptName(Player player) {
        plugin.dialogs().text(player, "Balance » Send money",
                "&7Who do you want to pay? Type their name - they may be offline.",
                "Player", "", 16, answer -> {
                    if (DialogPromptService.isCancel(answer)) {
                        Msg.send(player, "&7Payment cancelled.");
                        openReceivers(player, 0);
                        return;
                    }
                    OfflinePlayer target = Targets.offline(player, answer.trim());
                    if (target == null) {
                        openReceivers(player, 0);
                        return;
                    }
                    promptAmount(player, target);
                });
    }

    private void promptAmount(Player player, OfflinePlayer receiver) {
        double minimum = plugin.economy().minimumPayment();
        plugin.dialogs().number(player, "Balance » Send money",
                "&7How much do you want to send to &f" + Targets.displayName(receiver) + "&7? &8(min "
                        + plugin.economy().format(minimum) + ")", "Amount", "", 16, answer -> {
                    if (DialogPromptService.isCancel(answer)) {
                        Msg.send(player, "&7Payment cancelled.");
                        open(player);
                        return;
                    }
                    Double amount = Targets.parseDouble(answer);
                    if (amount == null || amount <= 0.0) {
                        Msg.error(player, "That is not a valid amount.");
                        open(player);
                        return;
                    }
                    pay(player, receiver, amount);
                });
    }

    private void pay(Player player, OfflinePlayer receiver, double amount) {
        EconomyService.TransferResult result =
                plugin.economy().transfer(player, receiver.getUniqueId(), amount);
        switch (result) {
            case SUCCESS -> {
                Msg.success(player, "You paid " + plugin.economy().format(amount)
                        + " to " + Targets.displayName(receiver) + ".");
                // The receiver only hears about it while they are online; an
                // offline payment is simply waiting in their balance.
                if (receiver.isOnline() && receiver.getPlayer() != null) {
                    Msg.send(receiver.getPlayer(), "&7You received &a" + plugin.economy().format(amount)
                            + " &7from &f" + player.getName() + "&7.");
                }
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.4f);
            }
            case PAYMENTS_DISABLED -> Msg.error(player, "Payments are disabled on this server.");
            case SELF -> Msg.error(player, "You cannot pay yourself.");
            case TOO_SMALL -> Msg.error(player, "The amount has to be at least "
                    + plugin.economy().format(plugin.economy().minimumPayment()) + ".");
            case TOO_POOR -> Msg.error(player, "You do not have enough money. Your balance is "
                    + plugin.economy().format(plugin.economy().getBalance(player.getUniqueId())) + ".");
        }
        open(player);
    }
}
