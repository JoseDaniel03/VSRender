package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.scheduler.PlatformExecutor;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.render.animation.RenderAnimationController;
import net.momirealms.craftengine.core.render.attachment.RenderAttachmentBinding;
import net.momirealms.craftengine.core.render.attachment.RenderPartTransform;
import net.momirealms.craftengine.core.render.compiled.CompiledRenderAnimationController;
import net.momirealms.craftengine.core.render.instance.RenderInstance;
import net.momirealms.craftengine.core.render.runtime.RenderSightVisibility;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owner-region host for a native packet-backed VsRender instance.
 * Static assets allocate no repeating task; animated assets tick at the platform's 20 Hz owner cadence.
 */
public final class BukkitNativeRenderInstance implements RenderInstance {
    private static final float TICK_SECONDS = 1f / 20f;

    private final BukkitNativeDisplayInstance display;
    private final CompiledRenderAnimationController animations;
    private final SchedulerTask tickTask;
    private final WorldPosition position;
    private final boolean sightTrace;
    private final double minSight;
    private final double maxSight;
    private final CopyOnWriteArraySet<Player> requestedViewers = new CopyOnWriteArraySet<>();
    private final Object lifecycleLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();

    public BukkitNativeRenderInstance(BukkitNativeDisplayAsset asset, WorldPosition position) {
        this(asset, position, false, 5d, Double.POSITIVE_INFINITY, BukkitCraftEngine.instance().scheduler().platform());
    }

    /** Injection-friendly constructor used by validation and alternate platform hosts. */
    public BukkitNativeRenderInstance(BukkitNativeDisplayAsset asset, WorldPosition position, PlatformExecutor scheduler) {
        this(asset, position, false, 5d, Double.POSITIVE_INFINITY, scheduler);
    }

    public BukkitNativeRenderInstance(BukkitNativeDisplayAsset asset, WorldPosition position,
                                      boolean sightTrace, double minSight, double maxSight, PlatformExecutor scheduler) {
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(scheduler, "scheduler");
        if (!(minSight >= 0d) || Double.isNaN(minSight)) throw new IllegalArgumentException("minSight");
        if (!(maxSight >= minSight) || Double.isNaN(maxSight)) throw new IllegalArgumentException("maxSight");
        this.position = position;
        this.sightTrace = sightTrace;
        this.minSight = minSight;
        this.maxSight = maxSight;
        this.display = new BukkitNativeDisplayInstance(asset, position);
        this.animations = asset.asset().animations().isEmpty()
                ? null
                : new CompiledRenderAnimationController(asset.rigId(), display.runtime());
        SchedulerTask scheduled = null;
        try {
            if (animations != null || sightTrace) {
                int chunkX = MiscUtils.floor(position.x) >> 4;
                int chunkZ = MiscUtils.floor(position.z) >> 4;
                scheduled = Objects.requireNonNull(
                        scheduler.runRepeating(this::tickOwnerRegion, 1L, 1L, position.world, chunkX, chunkZ),
                        "scheduler returned null task"
                );
            }
        } catch (RuntimeException | Error failure) {
            try {
                if (animations != null) animations.close();
            } catch (RuntimeException | Error cleanup) {
                failure.addSuppressed(cleanup);
            }
            try {
                display.close();
            } catch (RuntimeException | Error cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
        this.tickTask = scheduled;
    }

    private void tickOwnerRegion() {
        synchronized (lifecycleLock) {
            if (closed.get()) return;
            if (sightTrace) {
                LinkedHashSet<Player> visible = new LinkedHashSet<>();
                for (Player viewer : requestedViewers) if (canSee(viewer)) visible.add(viewer);
                display.syncViewers(visible);
            }
            if (display.runtime().needsTick()) {
                display.tick(TICK_SECONDS, display.nearestViewerDistance());
                if (animations != null) animations.afterTick();
            }
        }
    }

    @Override
    public void show(Player viewer) {
        synchronized (lifecycleLock) {
            if (closed.get()) return;
            if (!sightTrace) display.show(viewer);
            else {
                requestedViewers.add(viewer);
                if (canSee(viewer)) display.show(viewer);
            }
        }
    }

    @Override
    public void hide(Player viewer) {
        synchronized (lifecycleLock) {
            if (closed.get()) return;
            requestedViewers.remove(viewer);
            display.hide(viewer);
        }
    }

    private boolean canSee(Player viewer) {
        return RenderSightVisibility.canSee(
                Objects.equals(viewer.world(), position.world),
                viewer.x(), viewer.y(), viewer.z(), viewer.yRot(), viewer.xRot(),
                position.x, position.y, position.z, minSight, maxSight);
    }

    @Override
    public Optional<RenderAnimationController> animations() {
        return Optional.ofNullable(animations);
    }

    @Override
    public Optional<RenderAttachmentBinding> attachment(String partId) {
        Objects.requireNonNull(partId, "partId");
        int partIndex = display.asset().asset().rig().indexOf(partId);
        if (partIndex < 0 || !isAttachmentIndex(partIndex)) return Optional.empty();
        return Optional.of(new BukkitNativeAttachmentBinding(partId, () -> attachmentSnapshot(partIndex)));
    }

    private boolean isAttachmentIndex(int partIndex) {
        for (int index : display.asset().asset().rig().attachmentIndices()) if (index == partIndex) return true;
        return false;
    }

    private Optional<RenderPartTransform> attachmentSnapshot(int partIndex) {
        synchronized (lifecycleLock) {
            if (closed.get()) return Optional.empty();
            return Optional.of(BukkitNativeAttachmentSnapshots.local(display.runtime().poses(), partIndex));
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        RuntimeException failure = null;
        if (tickTask != null) {
            try {
                tickTask.cancel();
            } catch (RuntimeException error) {
                failure = error;
            }
        }
        synchronized (lifecycleLock) {
            requestedViewers.clear();
            if (animations != null) {
                try {
                    animations.close();
                } catch (RuntimeException error) {
                    if (failure == null) failure = error;
                    else failure.addSuppressed(error);
                }
            }
            try {
                display.close();
            } catch (RuntimeException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

    public boolean closed() {
        return closed.get();
    }

    public BukkitNativeDisplayInstance display() {
        return display;
    }
}
