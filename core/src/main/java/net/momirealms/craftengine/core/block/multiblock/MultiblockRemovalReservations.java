package net.momirealms.craftengine.core.block.multiblock;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/** Keeps an anchor unavailable until every pending regional removal finishes. */
public final class MultiblockRemovalReservations {
    private final ConcurrentHashMap<String, Ticket> pending = new ConcurrentHashMap<>();
    public Ticket begin(String key, int parts) {
        Objects.requireNonNull(key);
        if (key.isEmpty() || parts < 1 || parts > 64) throw new IllegalArgumentException("Invalid reservation");
        Ticket ticket = new Ticket(key, parts);
        return pending.putIfAbsent(key, ticket) == null ? ticket : null;
    }
    public boolean isReserved(String key) { return pending.containsKey(key); }
    public final class Ticket {
        private final String key;
        private final AtomicInteger remaining;
        private final AtomicIntegerArray completed;
        private Ticket(String key, int parts) {
            this.key = key;
            this.remaining = new AtomicInteger(parts);
            this.completed = new AtomicIntegerArray(parts);
        }
        public void completePart(int part) {
            if (completed.compareAndSet(part, 0, 1) && remaining.decrementAndGet() == 0) {
                pending.remove(key, this);
            }
        }
    }
}
