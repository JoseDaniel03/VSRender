package net.momirealms.craftengine.core.render.attachment;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pull/tick-driven attachment bridge. Platform/runtime owners decide scheduling and thread affinity.
 * The executor owns the source binding, but never implicitly owns/closes the target resource.
 */
public final class RenderAttachmentExecutor implements AutoCloseable {
    private final RenderAttachmentBinding binding;
    private final RenderAttachmentTarget target;
    private final AtomicBoolean closed = new AtomicBoolean();

    public RenderAttachmentExecutor(RenderAttachmentBinding binding, RenderAttachmentTarget target) {
        this.binding = Objects.requireNonNull(binding, "binding");
        this.target = Objects.requireNonNull(target, "target");
    }

    public RenderAttachmentBinding binding() {
        return binding;
    }

    public RenderAttachmentTarget target() {
        return target;
    }

    /**
     * Applies the latest pose once. An in-flight tick may finish while close is racing;
     * after close returns, later ticks are rejected as CLOSED.
     */
    public TickResult tick() {
        if (closed.get()) return TickResult.CLOSED;
        Optional<RenderPartTransform> snapshot = binding.snapshot();
        if (snapshot.isEmpty()) return closed.get() ? TickResult.CLOSED : TickResult.UNAVAILABLE;
        if (closed.get()) return TickResult.CLOSED;

        RenderPartTransform transform = snapshot.get();
        if (!target.supports(transform.space())) return TickResult.UNSUPPORTED_SPACE;
        return target.apply(transform) ? TickResult.APPLIED : TickResult.REJECTED;
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) binding.close();
    }

    public enum TickResult {
        APPLIED,
        UNAVAILABLE,
        UNSUPPORTED_SPACE,
        REJECTED,
        CLOSED
    }
}
