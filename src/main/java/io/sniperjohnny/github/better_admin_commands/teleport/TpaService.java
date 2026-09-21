package io.sniperjohnny.github.better_admin_commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pending /tpa and /tpahere requests. Requests expire after the configured
 * amount of seconds and are keyed by the player who has to answer them.
 */
public class TpaService {

    /** A single pending request. */
    public record Request(UUID sender, UUID target, boolean targetTeleportsToSender, long expiresAt) {

        public boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<UUID, List<Request>> pending = new ConcurrentHashMap<>();
    private final long expireMillis;

    public TpaService(Better_Admin_Commands plugin) {
        this.expireMillis = Math.max(5, plugin.getConfig().getInt("teleport.request-expire-seconds", 60)) * 1000L;
    }

    /**
     * Sends the request notice to the player who has to answer it. The notice
     * carries clickable buttons that run {@code /tpaaccept} and {@code /tpadeny}
     * for the player who sent the request.
     */
    public static void sendNotice(Player target, String senderName, boolean targetTeleportsToSender) {
        Component notice = Msg.prefixed(Msg.component("&f" + senderName + " &7"
                        + (targetTeleportsToSender
                        ? "wants you to teleport to them."
                        : "wants to teleport to you."))
                .append(Component.space())
                .append(Msg.button("&a[Accept]", "/tpaaccept " + senderName,
                        "&7Click to accept the request from &f" + senderName))
                .append(Component.space())
                .append(Msg.button("&c[Deny]", "/tpadeny " + senderName,
                        "&7Click to deny the request from &f" + senderName)));
        target.sendMessage(notice);
    }

    /** Registers a new request, replacing any previous one from the same sender. */
    public Request add(UUID sender, UUID target, boolean targetTeleportsToSender) {
        Request request = new Request(sender, target, targetTeleportsToSender,
                System.currentTimeMillis() + expireMillis);
        List<Request> requests = pending.computeIfAbsent(target, ignored -> new ArrayList<>());
        synchronized (requests) {
            requests.removeIf(existing -> existing.sender().equals(sender));
            requests.add(request);
        }
        return request;
    }

    /** Removes requests that have expired. */
    public void purge() {
        pending.values().forEach(requests -> {
            synchronized (requests) {
                requests.removeIf(Request::expired);
            }
        });
    }

    /** All still valid requests a player has to answer. */
    public List<Request> forTarget(UUID target) {
        List<Request> requests = pending.get(target);
        if (requests == null) {
            return List.of();
        }
        synchronized (requests) {
            requests.removeIf(Request::expired);
            return new ArrayList<>(requests);
        }
    }

    /** Finds (without removing) a specific pending request. */
    public Optional<Request> find(UUID target, UUID sender) {
        List<Request> requests = pending.get(target);
        if (requests == null) {
            return Optional.empty();
        }
        synchronized (requests) {
            requests.removeIf(Request::expired);
            return requests.stream().filter(request -> request.sender().equals(sender)).findFirst();
        }
    }

    /** Finds and removes a specific pending request. */
    public Optional<Request> poll(UUID target, UUID sender) {
        List<Request> requests = pending.get(target);
        if (requests == null) {
            return Optional.empty();
        }
        synchronized (requests) {
            requests.removeIf(Request::expired);
            Optional<Request> found = requests.stream()
                    .filter(request -> request.sender().equals(sender))
                    .findFirst();
            found.ifPresent(requests::remove);
            return found;
        }
    }

    /** Removes every request a player is involved in, e.g. when they log out. */
    public void clear(UUID uuid) {
        pending.remove(uuid);
        pending.values().forEach(requests -> {
            synchronized (requests) {
                requests.removeIf(request -> request.sender().equals(uuid));
            }
        });
    }
}
