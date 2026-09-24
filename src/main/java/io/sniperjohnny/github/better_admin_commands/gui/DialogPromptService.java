package io.sniperjohnny.github.better_admin_commands.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Asks a player for one value and hands the answer to a callback.
 *
 * <p>The question is shown as a real dialog (Paper's dialog API), so the player
 * types the value into a field in the window instead of into chat. A client that
 * cannot show a dialog (anything older than 1.21.6, for example through
 * ViaVersion) silently ignores the packet, so the same question is printed in
 * chat as well and the answer is picked up there - both paths run through the
 * same once-only callback, first answer wins.</p>
 *
 * <p>The callback always runs on the server thread. An escape (or a click outside
 * the dialog) answers nothing: the dialog just closes, and the chat prompt that was
 * armed for the fallback expires on its own (see {@link ChatPromptService}), so a
 * dismissed window can never swallow the player's next chat line.</p>
 */
public class DialogPromptService {

    /** Key of the single input every prompt dialog carries. */
    private static final String VALUE = "value";

    /** Answer handed to the callback when the player picks the cancel button. */
    public static final String CANCEL = "cancel";

    /**
     * Whether an answer means "the player backed out". Every caller that has a
     * cancel path checks it through this, so the cancel button, the escape key and
     * a typed {@code cancel} can never drift apart.
     */
    public static boolean isCancel(String answer) {
        return answer == null || answer.equalsIgnoreCase(CANCEL);
    }

    /** Line under the question, pointing players at the chat fallback. */
    private static final String FALLBACK_HINT = "&8» &7Enter it in the window"
            + " &8(or type it in chat, &fcancel&8 to abort&7).";

    /** Shorter reminder for a client that really does see the window. */
    private static final String WINDOW_HINT = " &8(or type it here)";

    /** First client protocol that can render a dialog (Minecraft 1.21.6). */
    private static final int DIALOG_PROTOCOL = 771;

    private final Better_Admin_Commands plugin;
    private final ChatPromptService chat;

    public DialogPromptService(Better_Admin_Commands plugin, ChatPromptService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    /* ------------------------------------------------------------------ text --- */

    /**
     * A single-line text field. Used for names, permissions and search terms.
     */
    public void text(Player player, String title, String question, String label, String initial,
                     int maxLength, Consumer<String> onAnswer) {
        TextDialogInput input = DialogInput.text(VALUE, Msg.component(label))
                .width(200)
                .labelVisible(true)
                .initial(initial == null ? "" : initial)
                .maxLength(Math.max(1, Math.min(256, maxLength)))
                .build();
        ask(player, title, question, input, view -> view.getText(VALUE), onAnswer);
    }

    /**
     * A multi-line text field. Used for things that used to be one chat line but
     * read better in a window, like mail and report messages. Line breaks are
     * collapsed into spaces, so the answer still fits the one-line storage and
     * rendering every flow uses today.
     */
    public void message(Player player, String title, String question, String label, String initial,
                        int maxLength, int lines, Consumer<String> onAnswer) {
        TextDialogInput input = DialogInput.text(VALUE, Msg.component(label))
                .width(300)
                .labelVisible(true)
                .initial(initial == null ? "" : initial)
                .maxLength(Math.max(1, Math.min(256, maxLength)))
                .multiline(TextDialogInput.MultilineOptions.create(
                        Math.max(1, Math.min(8, lines)), null))
                .build();
        ask(player, title, question, input,
                view -> joinLines(view.getText(VALUE)), onAnswer);
    }

    private static String joinLines(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    /* ---------------------------------------------------------------- numbers --- */

    /** A number the player types into a field. Words like `cancel` or `off` still reach the callback untouched. */
    public void number(Player player, String title, String question, String label, String initial,
                       int maxLength, Consumer<String> onAnswer) {
        TextDialogInput input = DialogInput.text(VALUE, Msg.component(label))
                .width(200)
                .labelVisible(true)
                .initial(initial == null ? "" : initial)
                .maxLength(Math.max(1, Math.min(32, maxLength)))
                .build();
        ask(player, title, question, input, view -> {
            String raw = view.getText(VALUE);
            return raw == null ? "" : raw.trim();
        }, onAnswer);
    }

    /**
     * A slider between two bounds. Used for values with a fixed range the player
     * usually picks by dragging - personal time, a per-purchase item count. The
     * callback receives the position as a plain number string, so the flows that
     * used to read the same words from chat can stay untouched.
     */
    public void slider(Player player, String title, String question, String label,
                       float start, float end, float step, float initial,
                       Consumer<String> onAnswer) {
        float low = Math.min(start, end);
        float high = Math.max(start, end);
        ask(player, title, question,
                DialogInput.numberRange(VALUE, Msg.component(label), low, high)
                        .width(300)
                        .labelFormat("%s: %s")
                        .initial(Math.max(low, Math.min(high, initial)))
                        .step(Math.max(0.01f, step))
                        .build(),
                view -> formatNumber(view.getFloat(VALUE)), onAnswer);
    }

    private static String formatNumber(Float value) {
        if (value == null) {
            return "";
        }
        long rounded = Math.round(value);
        return Math.abs(value - rounded) < 0.0001 ? Long.toString(rounded) : value.toString();
    }

    /** A bare yes/no dialog. */
    public void confirm(Player player, String title, String question, String yes, String no,
                        Consumer<Boolean> onAnswer) {
        UUID uuid = player.getUniqueId();
        AtomicBoolean answered = new AtomicBoolean();
        DialogAction yesButton = confirmAction(player, true, answered, onAnswer);
        DialogAction noButton = confirmAction(player, false, answered, onAnswer);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Msg.component(title))
                        .canCloseWithEscape(true)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Msg.component(question))))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(Msg.component(yes), null, 100, yesButton),
                        ActionButton.create(Msg.component(no), null, 100, noButton))));
        // A client that cannot show the dialog answers the same question in chat.
        chat.arm(uuid, text -> {
            Boolean value = confirmationAnswer(text);
            if (value == null) {
                chat.clear(uuid);
                return;
            }
            if (answered.compareAndSet(false, true)) {
                chat.clear(uuid);
                onAnswer.accept(value);
            }
        });
        show(player, dialog);
        askInChat(player, question);
    }

    /**
     * Reads a typed confirmation, in the shapes a player is likely to type.
     *
     * @return the answer, or {@code null} when the line was neither a yes nor a no
     */
    private static Boolean confirmationAnswer(String text) {
        String value = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "yes", "y", "confirm", "true", "on", "ok" -> Boolean.TRUE;
            case "no", "n", "deny", "false", "off", "cancel" -> Boolean.FALSE;
            default -> null;
        };
    }

    private DialogAction confirmAction(Player player, boolean value, AtomicBoolean answered,
                                       Consumer<Boolean> onAnswer) {
        UUID uuid = player.getUniqueId();
        return DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player responder && responder.getUniqueId().equals(uuid)
                    && answered.compareAndSet(false, true)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> onAnswer.accept(value));
            }
        }, uses(1));
    }

    /* ------------------------------------------------------------------- core --- */

    private void ask(Player player, String title, String question, DialogInput input,
                     Function<io.papermc.paper.dialog.DialogResponseView, String> reader,
                     Consumer<String> onAnswer) {
        UUID uuid = player.getUniqueId();
        AtomicBoolean answered = new AtomicBoolean();
        Consumer<String> once = answer -> {
            if (answer != null && answered.compareAndSet(false, true)) {
                chat.clear(uuid);
                onAnswer.accept(answer);
            }
        };
        DialogAction submit = DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player responder && responder.getUniqueId().equals(uuid)) {
                String value = view == null ? null : reader.apply(view);
                if (value != null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> once.accept(value));
                }
            }
        }, uses(1));
        DialogAction cancel = DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player responder && responder.getUniqueId().equals(uuid)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> once.accept(CANCEL));
            }
        }, uses(1));
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Msg.component(title))
                        .canCloseWithEscape(true)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Msg.component(question))))
                        .inputs(List.of(input))
                        .build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.create(Msg.component("&aConfirm"), null, 100, submit),
                        ActionButton.create(Msg.component("&cCancel"), null, 100, cancel)), null, 2)));
        // Arm the chat fallback first: a client that cannot show the dialog never
        // opens it, and the player answers the printed question in chat instead.
        chat.arm(uuid, text -> plugin.getServer().getScheduler()
                .runTask(plugin, () -> once.accept(text)));
        show(player, dialog);
        askInChat(player, question);
    }

    /**
     * Prints the question in chat, so the same flow works for a client that never
     * sees the window. Which wording is used depends on whether the client can show
     * a dialog at all: for a modern client this is only a one-line reminder that the
     * same answer may be typed here, an older client needs the question itself.
     */
    private void askInChat(Player player, String question) {
        if (supportsDialogs(player)) {
            Msg.send(player, question + WINDOW_HINT);
        } else {
            Msg.send(player, question);
            Msg.send(player, FALLBACK_HINT);
        }
    }

    /**
     * Whether the player's client is new enough to render a dialog (1.21.6+). An
     * unknown client counts as "cannot", because a missing answer would leave that
     * player with no way to reply at all.
     */
    private static boolean supportsDialogs(Player player) {
        try {
            return player.getProtocolVersion() >= DIALOG_PROTOCOL;
        } catch (Throwable e) {
            return false;
        }
    }

    private void show(Player player, Dialog dialog) {
        try {
            player.showDialog(dialog);
        } catch (Throwable e) {
            plugin.getLogger().warning("Could not open an input dialog for "
                    + player.getName() + " (" + e.getMessage() + ") - answer in chat instead.");
        }
    }

    private static ClickCallback.Options uses(int uses) {
        return ClickCallback.Options.builder().uses(uses).build();
    }
}
