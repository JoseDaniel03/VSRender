package net.momirealms.craftengine.core.render;

import net.momirealms.craftengine.core.render.animation.RenderAnimationSession;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Owns only animation sessions created for VsRender. */
public final class AnimationSessionBook implements AutoCloseable {
    /** @deprecated use {@link RenderAnimationSession}; kept to avoid breaking adapters mid-migration. */
    @Deprecated(forRemoval = false)
    public interface Session extends RenderAnimationSession {}

    private final Map<UUID, RenderAnimationSession> sessions = new HashMap<>();

    public synchronized boolean start(UUID id, RenderAnimationSession session) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(session);
        RenderAnimationSession previous = sessions.put(id, session);
        try {
            if (previous != null) previous.close();
            boolean started = session.start(() -> finish(id, session));
            if (!started) finish(id, session);
            return started;
        } catch (RuntimeException | Error failure) {
            try { finish(id, session); } catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private synchronized void finish(UUID id, RenderAnimationSession expected) {
        if (sessions.remove(id, expected)) expected.close();
    }

    public synchronized boolean stop(UUID id) {
        RenderAnimationSession session = sessions.remove(id);
        if (session == null) return false;
        session.close();
        return true;
    }

    public synchronized int size() {
        return sessions.size();
    }

    public synchronized Optional<RenderAnimationSession> session(UUID id) {
        Objects.requireNonNull(id);
        return Optional.ofNullable(sessions.get(id));
    }

    public synchronized void close() {
        var owned = java.util.List.copyOf(sessions.values());
        sessions.clear();
        RuntimeException failure = null;
        for (RenderAnimationSession session : owned) {
            try {
                session.close();
            } catch (RuntimeException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }
}
