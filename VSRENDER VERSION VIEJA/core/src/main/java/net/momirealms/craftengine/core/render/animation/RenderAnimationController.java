package net.momirealms.craftengine.core.render.animation;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Instance-scoped animation capability owned by VsRender. */
public interface RenderAnimationController extends AutoCloseable {

    String rigId();

    Set<String> animations();

    default boolean supports(RenderAnimationKey key) {
        Objects.requireNonNull(key, "key");
        return rigId().equals(key.rigId()) && animations().contains(key.animationId());
    }

    /** Creates, but does not start, a session. The caller owns the returned session lifecycle. */
    Optional<RenderAnimationSession> open(RenderAnimationKey key, RenderAnimationMode mode);

    @Override
    void close();
}

