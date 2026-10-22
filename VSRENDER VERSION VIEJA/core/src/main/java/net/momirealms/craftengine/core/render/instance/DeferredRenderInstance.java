package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.animation.RenderAnimationController;
import net.momirealms.craftengine.core.render.attachment.RenderAttachmentBinding;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Provider-independent lifecycle proxy for a render instance that is materialized asynchronously.
 *
 * <p>The proxy owns viewer intent until a concrete delegate is bound. A close that wins the race
 * permanently retires the proxy; a delegate arriving afterwards is closed immediately and is never
 * exposed. Provider callbacks are serialized by a separate delegate gate and never run while the
 * lifecycle monitor is held.</p>
 */
public final class DeferredRenderInstance implements RenderInstance {
    private final Object lock = new Object();
    private final Object delegateGate = new Object();
    private final Set<Player> desiredViewers = new LinkedHashSet<>();
    private RenderInstance delegate;
    private boolean closed;

    /**
     * Binds the concrete instance exactly once.
     *
     * @return {@code true} when the delegate became live; {@code false} when this proxy was already
     * closed and the supplied delegate was closed instead.
     * @throws IllegalStateException if a delegate was already bound
     */
    public boolean bind(RenderInstance instance) {
        Objects.requireNonNull(instance, "instance");
        final Player[] replay;
        synchronized (lock) {
            if (delegate != null) throw new IllegalStateException("render instance already bound");
            if (closed) {
                replay = null;
            } else {
                delegate = instance;
                replay = desiredViewers.toArray(Player[]::new);
            }
        }
        if (replay == null) {
            synchronized (delegateGate) {
                instance.close();
            }
            return false;
        }
        for (Player viewer : replay) {
            synchronized (delegateGate) {
                boolean stillDesired;
                synchronized (lock) {
                    stillDesired = !closed && delegate == instance && desiredViewers.contains(viewer);
                }
                if (stillDesired) instance.show(viewer);
            }
        }
        return true;
    }

    /** Permanently retires an unresolved proxy without materializing a delegate. */
    public void fail() {
        close();
    }

    @Override
    public void show(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        final RenderInstance current;
        final boolean added;
        synchronized (lock) {
            if (closed) return;
            added = desiredViewers.add(viewer);
            current = delegate;
        }
        if (!added || current == null) return;
        synchronized (delegateGate) {
            synchronized (lock) {
                if (closed || delegate != current || !desiredViewers.contains(viewer)) return;
            }
            current.show(viewer);
        }
    }

    @Override
    public void hide(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        final RenderInstance current;
        final boolean removed;
        synchronized (lock) {
            removed = desiredViewers.remove(viewer);
            current = delegate;
        }
        if (!removed || current == null) return;
        synchronized (delegateGate) {
            synchronized (lock) {
                if (closed || delegate != current) return;
            }
            current.hide(viewer);
        }
    }

    @Override
    public void refresh(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        final RenderInstance current;
        synchronized (lock) {
            if (closed) return;
            desiredViewers.add(viewer);
            current = delegate;
        }
        if (current == null) return;
        synchronized (delegateGate) {
            synchronized (lock) {
                if (closed || delegate != current || !desiredViewers.contains(viewer)) return;
            }
            current.refresh(viewer);
        }
    }

    @Override
    public Optional<RenderAttachmentBinding> attachment(String partId) {
        Objects.requireNonNull(partId, "partId");
        final RenderInstance current;
        synchronized (lock) {
            if (closed) return Optional.empty();
            current = delegate;
        }
        if (current == null) return Optional.empty();
        synchronized (delegateGate) {
            synchronized (lock) {
                if (closed || delegate != current) return Optional.empty();
            }
            return current.attachment(partId);
        }
    }

    @Override
    public Optional<RenderAnimationController> animations() {
        final RenderInstance current;
        synchronized (lock) {
            if (closed) return Optional.empty();
            current = delegate;
        }
        if (current == null) return Optional.empty();
        synchronized (delegateGate) {
            synchronized (lock) {
                if (closed || delegate != current) return Optional.empty();
            }
            return current.animations();
        }
    }

    @Override
    public void close() {
        final RenderInstance current;
        synchronized (lock) {
            if (closed) return;
            closed = true;
            desiredViewers.clear();
            current = delegate;
            delegate = null;
        }
        if (current != null) {
            synchronized (delegateGate) {
                current.close();
            }
        }
    }

    public boolean resolved() {
        synchronized (lock) {
            return delegate != null;
        }
    }

    public boolean closed() {
        synchronized (lock) {
            return closed;
        }
    }

    public int desiredViewerCount() {
        synchronized (lock) {
            return desiredViewers.size();
        }
    }
}
