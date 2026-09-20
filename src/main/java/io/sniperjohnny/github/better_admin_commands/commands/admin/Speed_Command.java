package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Changes the walking or flying speed of a player. */
public class Speed_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 2) {
            Msg.usage(sender, command);
            return true;
        }
        String type = args[0].toLowerCase(Locale.ROOT);
        Double value = Targets.parseDouble(args[1]);
        if (value == null) {
            Msg.error(sender, "The speed has to be a number between 0 and 10.");
            return true;
        }
        float speed = (float) Math.max(0.0, Math.min(1.0, value / 10.0));

        switch (type) {
            case "walk", "walking" -> {
                self.setWalkSpeed(speed);
                Msg.success(sender, "Walk speed set to " + value + ".");
            }
            case "fly", "flying" -> {
                self.setFlySpeed(speed);
                Msg.success(sender, "Fly speed set to " + value + ".");
            }
            default -> Msg.error(sender, "Use /speed <walk|fly> <0-10>.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "walk", "fly");
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        }
        return Collections.emptyList();
    }
}
