package net.momirealms.craftengine.core.render;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Tracks futures that must not survive an owning service shutdown.
 * Registration and close share one lock so a future is either registered
 * before shutdown and completed by it, or registered afterwards already closed.
 */
public final class PendingFutureRegistry<T> implements AutoCloseable {
    private final Object lock = new Object();
    private final Set<CompletableFuture<T>> pending = new HashSet<>();
    private final T closedResult;
    private boolean closed;

    public PendingFutureRegistry(T closedResult) {
        this.closedResult = closedResult;
    }

    public CompletableFuture<T> register() {
        var future = new CompletableFuture<T>();
        synchronized (lock) {
            if (closed) {
                future.complete(closedResult);
                return future;
            }
            pending.add(future);
        }
        future.whenComplete((result, failure) -> remove(future));
        return future;
    }

    private void remove(CompletableFuture<T> future) {
        synchronized (lock) {
            pending.remove(future);
        }
    }

    public int size() {
        synchronized (lock) {
            return pending.size();
        }
    }

    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    @Override
    public void close() {
        List<CompletableFuture<T>> outstanding;
        synchronized (lock) {
            if (closed) return;
            closed = true;
            outstanding = List.copyOf(pending);
            pending.clear();
        }
        for (var future : outstanding) future.complete(closedResult);
    }
}
