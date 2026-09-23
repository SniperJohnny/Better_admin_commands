package io.sniperjohnny.github.better_admin_commands.report;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The report system.
 *
 * <ul>
 *   <li>{@code /report} - the menu: create a ticket, browse your own tickets and,
 *       for staff, all open and closed tickets.</li>
 *   <li>{@code /reports} - staff shortcut straight to the open tickets.</li>
 *   <li>{@code /report view <id>} - opens one ticket (used by the chat buttons).</li>
 * </ul>
 *
 * <p>Every ticket is a two-way thread: the player and any number of staff reply
 * into the same ticket, and everybody involved is told when a new message
 * arrives.</p>
 */
public class Report_Command implements TabExecutor {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int MESSAGE_SLOTS = 36;

    /** A configured report type. */
    private record Preset(String id, String display, Material icon, String description, boolean targetRequired) {
    }

    private final Better_Admin_Commands plugin;

    public Report_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (!player.hasPermission(ReportService.BASE_PERMISSION)) {
            Msg.noPermission(player);
            return true;
        }
        if (!plugin.reports().available()) {
            Msg.error(player, "The report system needs the database, which is currently unavailable.");
            return true;
        }

        boolean staffCommand = command.getName().equalsIgnoreCase("reports");
        if (staffCommand) {
            if (!player.hasPermission(ReportService.STAFF_PERMISSION)) {
                Msg.noPermission(player);
                return true;
            }
            openList(player, true, true, 0);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("view")) {
            ReportService.Report report = plugin.reports().find(args[1]);
            if (report == null) {
                Msg.error(player, "That ticket does not exist.");
                openMain(player);
                return true;
            }
            if (!report.reporterUuid().equals(player.getUniqueId())
                    && !player.hasPermission(ReportService.STAFF_PERMISSION)) {
                Msg.noPermission(player);
                return true;
            }
            openTicket(player, report.id(), 0);
            return true;
        }

        openMain(player);
        return true;
    }

    /* -------------------------------------------------------- main menu --- */

    private void openMain(Player player) {
        boolean staff = player.hasPermission(ReportService.STAFF_PERMISSION);
        Menu menu = new Menu("&8Reports", 5);
        menu.frame();

        int mine = plugin.reports().mine(player.getUniqueId()).size();
        int unread = plugin.reports().totalUnread(player.getUniqueId());
        int openCount = plugin.reports().all(true).size();
        int closedCount = plugin.reports().all(false).size() - openCount;

        menu.button(20, Items.of(Material.WRITABLE_BOOK, "&aCreate a report",
                        "&7Pick a category, then tell us what happened.",
                        "", "&eClick to start"),
                event -> openPresets(player, 0));
        menu.button(22, Items.of(Material.PAPER, "&6My tickets &7(" + mine + ")",
                        "&7Everything you reported, open and closed.",
                        unread > 0 ? "&c" + unread + " unread message(s)" : "&7No unread messages",
                        "", "&eClick to open"),
                event -> openList(player, false, false, 0));

        if (staff) {
            menu.button(24, Items.of(Material.CHEST, "&bAll open tickets &7(" + openCount + ")",
                            "&7Every ticket that still needs an answer.",
                            "", "&eClick to open"),
                    event -> openList(player, true, true, 0));
            menu.button(30, Items.of(Material.ENDER_CHEST, "&7All closed tickets &7(" + closedCount + ")",
                            "&7Tickets that were handled.",
                            "", "&eClick to open"),
                    event -> openList(player, true, false, 0));
        }

        menu.button(40, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        menu.open(player);
    }

    /* ------------------------------------------------------- preset menu --- */

    private void openPresets(Player player, int page) {
        List<Preset> presets = presets();
        int pageSize = 45;
        int pages = Math.max(1, (int) Math.ceil(presets.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Reports &8» &fNew report", 6);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < presets.size(); index++) {
            Preset preset = presets.get(start + index);
            List<String> lore = new ArrayList<>();
            if (preset.description() != null && !preset.description().isBlank()) {
                lore.add("&7" + preset.description());
            }
            lore.add("");
            lore.add(preset.targetRequired() ? "&7A player is picked next." : "&7No player is needed.");
            lore.add("&eClick to continue");
            menu.button(index, Items.of(preset.icon(), "&f" + preset.display(), lore), event -> {
                if (preset.targetRequired()) {
                    openTargets(player, preset, 0);
                } else {
                    startCreate(player, preset, null);
                }
            });
        }

        int nav = 45;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openPresets(player, current - 1));
        }
        menu.button(nav + 4, Items.of(Material.BARRIER, "&cBack"), event -> openMain(player));
        menu.button(nav + 6, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openPresets(player, current + 1));
        }
        menu.open(player);
    }

    /* ------------------------------------------------------- target menu --- */

    private void openTargets(Player player, Preset preset, int page) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.removeIf(other -> other.equals(player));
        online.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        int pageSize = 45;
        int pages = Math.max(1, (int) Math.ceil(online.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Reports &8» &fWho is it about?", 6);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < online.size(); index++) {
            Player target = online.get(start + index);
            menu.button(index, Items.of(Material.PLAYER_HEAD, "&f" + target.getName(),
                            "&7Report &f" + target.getName() + " &7for &f" + preset.display() + "&7.",
                            "", "&eClick to pick"),
                    event -> startCreate(player, preset, target.getName()));
        }

        int nav = 45;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openTargets(player, preset, current - 1));
        }
        menu.button(nav + 4, Items.of(Material.BARRIER, "&cBack"), event -> openPresets(player, 0));
        menu.button(nav + 6, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openTargets(player, preset, current + 1));
        }
        menu.open(player);
    }

    /* -------------------------------------------------------- ticket list -- */

    private void openList(Player player, boolean staffView, boolean openOnly, int page) {
        if (staffView && !player.hasPermission(ReportService.STAFF_PERMISSION)) {
            Msg.noPermission(player);
            return;
        }
        List<ReportService.Report> reports = staffView
                ? plugin.reports().all(openOnly)
                : plugin.reports().mine(player.getUniqueId());

        int pageSize = 45;
        int pages = Math.max(1, (int) Math.ceil(reports.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        String title = staffView
                ? (openOnly ? "&8Reports &8» &fAll open" : "&8Reports &8» &fAll closed")
                : "&8Reports &8» &fMy tickets";
        Menu menu = new Menu(title, 6);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < reports.size(); index++) {
            ReportService.Report report = reports.get(start + index);
            menu.button(index, describeTicket(player, report), event -> openTicket(player, report.id(), 0));
        }

        int nav = 45;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openList(player, staffView, openOnly, current - 1));
        }
        menu.button(nav + 4, Items.of(Material.BARRIER, "&cBack"), event -> openMain(player));
        if (staffView) {
            menu.button(nav + 5, Items.of(openOnly ? Material.ENDER_CHEST : Material.CHEST,
                            openOnly ? "&7Show closed" : "&bShow open",
                            "&7Switch between open and closed tickets."),
                    event -> openList(player, true, !openOnly, 0));
        }
        menu.button(nav + 6, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openList(player, staffView, openOnly, current + 1));
        }
        menu.open(player);
    }

    private ItemStack describeTicket(Player viewer, ReportService.Report report) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Category: &f" + categoryDisplay(report.category()));
        if (report.targetName() != null) {
            lore.add("&7About: &f" + report.targetName());
        }
        lore.add("&7Reporter: &f" + report.reporterName());
        lore.add("&7Status: " + (report.isOpen() ? "&aopen" : "&7closed"));

        ReportService.Message last = plugin.reports().lastMessage(report.id());
        if (last != null) {
            lore.add("");
            lore.add("&7Last message from &f" + last.authorName() + "&7:");
            for (String line : wrap(last.message(), 38)) {
                lore.add("&7" + line);
            }
        }
        lore.add("");
        lore.add("&7Ticket: &8#" + ReportService.shortId(report.id()));
        lore.add("&7Updated: &f" + date(report.updatedAt()));
        int unread = plugin.reports().unread(viewer.getUniqueId(), report.id());
        if (unread > 0) {
            lore.add("");
            lore.add("&c" + unread + " new message(s)");
        }
        lore.add("");
        lore.add("&eClick to open");

        Material icon = report.isOpen() ? Material.PAPER : Material.MAP;
        return Items.of(icon, (report.isOpen() ? "&a" : "&7") + categoryDisplay(report.category())
                + " &8#" + ReportService.shortId(report.id()), lore);
    }

    /* -------------------------------------------------------- ticket view - */

    private void openTicket(Player player, String reportId, int page) {
        ReportService.Report report = plugin.reports().find(reportId);
        if (report == null) {
            Msg.error(player, "That ticket does not exist.");
            openMain(player);
            return;
        }
        if (!report.reporterUuid().equals(player.getUniqueId())
                && !player.hasPermission(ReportService.STAFF_PERMISSION)) {
            Msg.noPermission(player);
            return;
        }
        // Opening the ticket counts as reading it and adds the player to the thread.
        plugin.reports().touch(player, report.id());

        List<ReportService.Message> thread = new ArrayList<>(plugin.reports().messages(report.id()));
        Collections.reverse(thread);
        int pages = Math.max(1, (int) Math.ceil(thread.size() / (double) MESSAGE_SLOTS));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Ticket &8#&f" + ReportService.shortId(report.id()), 6);
        menu.frame();

        Preset presetForCategory = presetById(report.category());
        Material icon = presetForCategory == null ? Material.PAPER : presetForCategory.icon();
        menu.button(0, Items.of(icon, "&f" + categoryDisplay(report.category()),
                "&7Ticket: &8#" + ReportService.shortId(report.id()),
                "&7Created: &f" + date(report.createdAt())));
        menu.button(1, Items.of(Material.PLAYER_HEAD, "&7Reporter: &f" + report.reporterName(),
                "&7Reported at &f" + date(report.createdAt())));
        if (report.targetName() != null) {
            menu.button(2, Items.of(Material.NAME_TAG, "&7About: &f" + report.targetName()));
        }
        menu.button(3, Items.of(report.isOpen() ? Material.LIME_DYE : Material.GRAY_DYE,
                report.isOpen() ? "&aOpen" : "&7Closed",
                "&7" + plugin.reports().participants(report.id()).size() + " participant(s)"));

        List<String> staffNames = new ArrayList<>();
        for (ReportService.Participant participant : plugin.reports().participants(report.id())) {
            if (participant.staff()) {
                staffNames.add(participant.name());
            }
        }
        menu.button(4, Items.of(Material.WRITABLE_BOOK, "&7Staff on this ticket",
                staffNames.isEmpty() ? "&7Nobody picked it up yet." : "&f" + String.join("&7, &f", staffNames)));

        int start = current * MESSAGE_SLOTS;
        for (int index = 0; index < MESSAGE_SLOTS && start + index < thread.size(); index++) {
            ReportService.Message message = thread.get(start + index);
            List<String> lore = new ArrayList<>();
            for (String line : wrap(message.message(), 38)) {
                lore.add("&7" + line);
            }
            lore.add("");
            lore.add("&8" + date(message.createdAt()));
            menu.button(9 + index, Items.of(message.staff() ? Material.BOOK : Material.PAPER,
                    (message.staff() ? "&b" : "&f") + message.authorName(), lore));
        }

        int nav = 45;
        menu.button(nav, Items.of(Material.WRITABLE_BOOK, "&aReply", "&7Answer in chat."),
                event -> startReply(player, report.id()));
        menu.button(nav + 1, report.isOpen()
                        ? Items.of(Material.RED_CONCRETE, "&cClose ticket", "&7Marks it as handled.")
                        : Items.of(Material.LIME_CONCRETE, "&aReopen ticket", "&7Moves it back to open."),
                event -> {
                    plugin.reports().setStatus(report.id(), report.isOpen() ? ReportService.CLOSED : ReportService.OPEN);
                    Msg.success(player, report.isOpen() ? "Ticket closed." : "Ticket reopened.");
                    openTicket(player, report.id(), 0);
                });
        menu.button(nav + 4, Items.of(Material.BARRIER, "&cBack"), event -> openMain(player));
        if (current > 0) {
            menu.button(nav + 6, Items.of(Material.ARROW, "&eNewer messages"), event -> openTicket(player, report.id(), current - 1));
        }
        menu.button(nav + 7, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.of(Material.ARROW, "&eOlder messages"), event -> openTicket(player, report.id(), current + 1));
        }
        menu.open(player);
    }

    /* ------------------------------------------------------------- flows --- */

    private void startCreate(Player player, Preset preset, String target) {
        plugin.dialogs().message(player, "Create a report",
                "&7Describe the &f" + preset.display() + "&7 report.",
                "Description", "", 256, 4, answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
                        Msg.send(player, "&7Report cancelled.");
                        openMain(player);
                        return;
                    }
                    if (answer.isBlank()) {
                        Msg.error(player, "The description is empty.");
                        openMain(player);
                        return;
                    }
                    ReportService.Report report = plugin.reports().create(player, preset.id(), target, answer);
                    if (report == null) {
                        Msg.error(player, "The report could not be created.");
                        openMain(player);
                        return;
                    }
                    Msg.success(player, "Report #" + ReportService.shortId(report.id())
                            + " created - staff can answer you there.");
                    openTicket(player, report.id(), 0);
                });
    }

    private void startReply(Player player, String reportId) {
        plugin.dialogs().message(player, "Report #" + ReportService.shortId(reportId),
                "&7Type your reply for ticket &f#" + ReportService.shortId(reportId) + "&7.",
                "Reply", "", 256, 4, answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
                        Msg.send(player, "&7Reply cancelled.");
                        openTicket(player, reportId, 0);
                        return;
                    }
                    if (answer.isBlank()) {
                        Msg.error(player, "The reply is empty.");
                        openTicket(player, reportId, 0);
                        return;
                    }
                    if (plugin.reports().reply(player, reportId, answer) == null) {
                        Msg.error(player, "That ticket does not exist.");
                    } else {
                        Msg.success(player, "Reply added.");
                    }
                    openTicket(player, reportId, 0);
                });
    }

    /* ----------------------------------------------------------- presets --- */

    private List<Preset> presets() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("report.presets");
        List<Preset> presets = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                presets.add(new Preset(key,
                        entry.getString("display", key),
                        material(entry.getString("icon", "PAPER")),
                        entry.getString("description", ""),
                        entry.getBoolean("target-required", true)));
            }
        }
        if (presets.isEmpty()) {
            presets.add(new Preset("hacking", "Hacking", Material.DIAMOND_SWORD, "Cheating or unfair advantages", true));
            presets.add(new Preset("rulebreaking", "Rule breaking", Material.WRITABLE_BOOK, "Breaking a server rule", true));
            presets.add(new Preset("harassment", "Harassment", Material.PLAYER_HEAD, "Insults, threats or bullying", true));
            presets.add(new Preset("scamming", "Scamming", Material.GOLD_INGOT, "Scams or stolen items", true));
            presets.add(new Preset("bugreport", "Bug report", Material.TNT, "Something on the server is broken", false));
        }
        return presets;
    }

    private Preset presetById(String id) {
        for (Preset preset : presets()) {
            if (preset.id().equalsIgnoreCase(id)) {
                return preset;
            }
        }
        return null;
    }

    private String categoryDisplay(String id) {
        Preset preset = presetById(id);
        return preset == null ? id : preset.display();
    }

    private static Material material(String name) {
        Material material = name == null ? null : Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return material == null ? Material.PAPER : material;
    }

    /* ----------------------------------------------------------- helpers --- */

    private static String date(long millis) {
        return DATE.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (remaining.length() > width) {
            int cut = remaining.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width;
            }
            lines.add(remaining.substring(0, cut));
            remaining = remaining.substring(Math.min(cut + 1, remaining.length())).trim();
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        if (lines.isEmpty()) {
            lines.add("(empty)");
        }
        return lines;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
