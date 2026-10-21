package net.momirealms.craftengine.core.render.attachment;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RenderAttachmentExecutorTest {

    @Test
    void appliesLatestSupportedSnapshot() {
        RenderPartTransform transform = transform(RenderTransformSpace.INSTANCE_LOCAL);
        AtomicReference<RenderPartTransform> applied = new AtomicReference<>();
        RenderAttachmentExecutor executor = new RenderAttachmentExecutor(
                binding(transform),
                target(RenderTransformSpace.INSTANCE_LOCAL, applied)
        );

        assertEquals(RenderAttachmentExecutor.TickResult.APPLIED, executor.tick());
        assertSame(transform, applied.get());
    }

    @Test
    void neverConvertsUnsupportedSpace() {
        AtomicReference<RenderPartTransform> applied = new AtomicReference<>();
        RenderAttachmentExecutor executor = new RenderAttachmentExecutor(
                binding(transform(RenderTransformSpace.INSTANCE_LOCAL)),
                target(RenderTransformSpace.WORLD, applied)
        );

        assertEquals(RenderAttachmentExecutor.TickResult.UNSUPPORTED_SPACE, executor.tick());
        assertNull(applied.get());
    }

    @Test
    void missingSnapshotDoesNotReuseOldPose() {
        AtomicInteger calls = new AtomicInteger();
        RenderAttachmentBinding binding = new RenderAttachmentBinding() {
            int reads;
            @Override public String partId() { return "socket"; }
            @Override public Optional<RenderPartTransform> snapshot() {
                return reads++ == 0 ? Optional.of(transform(RenderTransformSpace.INSTANCE_LOCAL)) : Optional.empty();
            }
            @Override public void close() {}
        };
        RenderAttachmentExecutor executor = new RenderAttachmentExecutor(binding, new RenderAttachmentTarget() {
            @Override public boolean supports(RenderTransformSpace space) { return true; }
            @Override public boolean apply(RenderPartTransform transform) { calls.incrementAndGet(); return true; }
        });

        assertEquals(RenderAttachmentExecutor.TickResult.APPLIED, executor.tick());
        assertEquals(RenderAttachmentExecutor.TickResult.UNAVAILABLE, executor.tick());
        assertEquals(1, calls.get());
    }

    @Test
    void closeOwnsBindingButNotTarget() {
        AtomicBoolean bindingClosed = new AtomicBoolean();
        AtomicInteger targetCalls = new AtomicInteger();
        RenderAttachmentBinding binding = new RenderAttachmentBinding() {
            @Override public String partId() { return "socket"; }
            @Override public Optional<RenderPartTransform> snapshot() { return Optional.of(transform(RenderTransformSpace.INSTANCE_LOCAL)); }
            @Override public void close() { bindingClosed.set(true); }
        };
        RenderAttachmentExecutor executor = new RenderAttachmentExecutor(binding, new RenderAttachmentTarget() {
            @Override public boolean supports(RenderTransformSpace space) { return true; }
            @Override public boolean apply(RenderPartTransform transform) { targetCalls.incrementAndGet(); return true; }
        });

        executor.close();
        executor.close();
        assertTrue(bindingClosed.get());
        assertTrue(executor.isClosed());
        assertEquals(RenderAttachmentExecutor.TickResult.CLOSED, executor.tick());
        assertEquals(0, targetCalls.get());
    }

    private static RenderAttachmentBinding binding(RenderPartTransform transform) {
        return new RenderAttachmentBinding() {
            @Override public String partId() { return "socket"; }
            @Override public Optional<RenderPartTransform> snapshot() { return Optional.of(transform); }
            @Override public void close() {}
        };
    }

    private static RenderAttachmentTarget target(RenderTransformSpace supported, AtomicReference<RenderPartTransform> applied) {
        return new RenderAttachmentTarget() {
            @Override public boolean supports(RenderTransformSpace space) { return space == supported; }
            @Override public boolean apply(RenderPartTransform transform) { applied.set(transform); return true; }
        };
    }

    private static RenderPartTransform transform(RenderTransformSpace space) {
        return new RenderPartTransform(space, 1, 2, 3, 4, 5, 6);
    }
}
