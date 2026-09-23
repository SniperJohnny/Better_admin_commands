package io.sniperjohnny.github.better_admin_commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The balance menu behind {@code /balance}: your balance, a shortcut to the
 * richest players and a "send money" flow that picks the receiver from the
 * online players instead of making you type a name.
 *
 * <p>Every transfer goes through {@link EconomyService#transfer}, so the menu
 * enforces exactly the same rules as {@code /pay}.</p>
 */
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

    /* ------------------------------------------------------- send money --- */

    private void openReceivers(Player player, int page) {
        List<Player> receivers = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
                receivers.add(online);
            }
        }
        if (receivers.isEmpty()) {
            Msg.error(player, "There is nobody else online to pay.");
            open(player);
            return;
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
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        menu.button(nav + 6, Items.of(Material.BARRIER, "&cBack"), event -> open(player));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openReceivers(player, current + 1));
        }
        menu.open(player);
    }

    private void promptAmount(Player player, Player receiver) {
        double minimum = plugin.economy().minimumPayment();
        plugin.chatPrompts().request(player,
                "&7How much do you want to send to &f" + receiver.getName() + "&7? &8(min "
                        + plugin.economy().format(minimum) + ")", answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
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

    private void pay(Player player, Player receiver, double amount) {
        EconomyService.TransferResult result =
                plugin.economy().transfer(player, receiver.getUniqueId(), amount);
        switch (result) {
            case SUCCESS -> {
                Msg.success(player, "You paid " + plugin.economy().format(amount)
                        + " to " + receiver.getName() + ".");
                Msg.send(receiver, "&7You received &a" + plugin.economy().format(amount)
                        + " &7from &f" + player.getName() + "&7.");
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
