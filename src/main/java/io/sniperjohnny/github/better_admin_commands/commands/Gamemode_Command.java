package io.sniperjohnny.github.better_admin_commands.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Gamemode_Command implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(!(sender instanceof final Player p))
        {
            sender.sendMessage("You need to be an Player to use this Command.");

            return true;

        }
        if(args.length < 1) {
            p.sendMessage("Pls use a number or the corresponding name to set you gamemode!");
            return true;
        }

        String wanted_Gamemode = args[0].toLowerCase();
        if(args.length == 1) {
        switch(args[0]) {
            case "0":
                p.setGameMode(GameMode.SURVIVAL);
                break;

            case "1":
                p.setGameMode(GameMode.CREATIVE);
                break;

            case "2":
                p.setGameMode(GameMode.ADVENTURE);
                break;
            case "3":
                p.setGameMode(GameMode.SPECTATOR);
                break;
            case "survival":
                p.setGameMode(GameMode.SURVIVAL);
                break;
            case "creative":
                p.setGameMode(GameMode.CREATIVE);
                break;
            case "adventure":
                p.setGameMode(GameMode.ADVENTURE);
                break;
            case "spectator":
                p.setGameMode(GameMode.SPECTATOR);
                break;


        }

        return true;
        }
        String playerName = args[1];
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        switch(args[0]) {
            case "0":
                target.setGameMode(GameMode.SURVIVAL);
                break;

            case "1":
                target.setGameMode(GameMode.CREATIVE);
                break;

            case "2":
                target.setGameMode(GameMode.ADVENTURE);
                break;
            case "3":
                target.setGameMode(GameMode.SPECTATOR);
                break;
            case "survival":
                target.setGameMode(GameMode.SURVIVAL);
                break;
            case "creative":
                target.setGameMode(GameMode.CREATIVE);
                break;
            case "adventure":
                target.setGameMode(GameMode.ADVENTURE);
                break;
            case "spectator":
                target.setGameMode(GameMode.SPECTATOR);
                break;


        }
        return true;


    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if(args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("0", "1","2",
                    "3", "survival", "creative", "adventure", "spectator"), completions);
            return completions;
        }
        if(args.length == 2) {

            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[1], players, completions);
            return completions;
        }
        return Collections.emptyList();
    }
}
