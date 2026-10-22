package net.momirealms.craftengine.core.render.attachment;

import java.util.Optional;

/**
 * Runtime binding to a named rig attachment point.
 * The binding never exposes provider tracker/bone objects.
 */
public interface RenderAttachmentBinding extends AutoCloseable {

    String partId();

    /** Returns the latest pose, or empty when the instance/part is no longer available. */
    Optional<RenderPartTransform> snapshot();

    /** Convenience bridge for runtimes that want to apply the latest pose to another resource. */
    default boolean apply(java.util.function.Consumer<RenderPartTransform> consumer) {
        java.util.Objects.requireNonNull(consumer, "consumer");
        Optional<RenderPartTransform> snapshot = snapshot();
        snapshot.ifPresent(consumer);
        return snapshot.isPresent();
    }

    @Override
    void close();
}
