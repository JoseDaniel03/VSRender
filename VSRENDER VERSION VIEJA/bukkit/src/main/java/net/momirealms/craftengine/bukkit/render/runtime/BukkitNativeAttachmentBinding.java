package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.render.attachment.RenderAttachmentBinding;
import net.momirealms.craftengine.core.render.attachment.RenderPartTransform;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Lightweight native locator binding. The host supplier owns thread-affinity and lifecycle locking. */
final class BukkitNativeAttachmentBinding implements RenderAttachmentBinding {
    private final String partId;
    private final Supplier<Optional<RenderPartTransform>> snapshotSupplier;
    private final AtomicBoolean closed = new AtomicBoolean();

    BukkitNativeAttachmentBinding(String partId, Supplier<Optional<RenderPartTransform>> snapshotSupplier) {
        this.partId = Objects.requireNonNull(partId, "partId");
        if (partId.isBlank()) throw new IllegalArgumentException("partId must not be blank");
        this.snapshotSupplier = Objects.requireNonNull(snapshotSupplier, "snapshotSupplier");
    }

    @Override
    public String partId() {
        return partId;
    }

    @Override
    public Optional<RenderPartTransform> snapshot() {
        return closed.get() ? Optional.empty() : snapshotSupplier.get();
    }

    @Override
    public void close() {
        closed.set(true);
    }
}
