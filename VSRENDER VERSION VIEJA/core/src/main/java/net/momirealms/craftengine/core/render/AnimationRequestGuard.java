package net.momirealms.craftengine.core.render;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/** Guards scheduled requests against equipment changes and player retirement. */
public final class AnimationRequestGuard {
    public record Ticket(UUID player, long revision) {}
    private final ConcurrentHashMap<UUID, Long> revisions = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    public Ticket capture(UUID player) { return new Ticket(player, revisions.computeIfAbsent(player, key -> sequence.incrementAndGet())); }
    public boolean valid(Ticket ticket) { return java.util.Objects.equals(revisions.get(ticket.player()), ticket.revision()); }
    public void invalidate(UUID player) { revisions.put(player, sequence.incrementAndGet()); }
    public void retire(UUID player) { revisions.remove(player); }
}
