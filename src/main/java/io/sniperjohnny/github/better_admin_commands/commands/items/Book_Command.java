package io.sniperjohnny.github.better_admin_commands.commands.items;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Gives written books and edits their author and title. */
public class Book_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);

        if (action.equals("give")) {
            if (args.length < 2) {
                Msg.usage(sender, command);
                return true;
            }
            Player target = Targets.online(sender, args[1]);
            if (target == null) {
                return true;
            }
            String title = args.length >= 4 && args[2].equalsIgnoreCase("title")
                    ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length))
                    : "Book";
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
            BookMeta meta = (BookMeta) book.getItemMeta();
            if (meta != null) {
                meta.setTitle(title);
                meta.setAuthor(sender.getName());
                meta.addPage(" ");
                book.setItemMeta(meta);
            }
            target.getInventory().addItem(book).forEach((index, leftover) ->
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            Msg.success(sender, "Gave a book to " + target.getName() + ".");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!(held.getItemMeta() instanceof BookMeta meta)) {
            Msg.error(player, "You have to hold a written book.");
            return true;
        }
        switch (action) {
            case "author" -> {
                if (args.length < 2) {
                    Msg.usage(sender, command);
                    return true;
                }
                meta.setAuthor(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
            }
            case "title" -> {
                if (args.length < 2) {
                    Msg.usage(sender, command);
                    return true;
                }
                meta.setTitle(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
            }
            case "unlock" -> meta.setGeneration(BookMeta.Generation.ORIGINAL);
            default -> {
                Msg.error(player, "Use /book <give|author|title|unlock>.");
                return true;
            }
        }
        held.setItemMeta(meta);
        Msg.success(player, "Book updated.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "give", "author", "title", "unlock");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Targets.complete(args[1]);
        }
        return Collections.emptyList();
    }
}
