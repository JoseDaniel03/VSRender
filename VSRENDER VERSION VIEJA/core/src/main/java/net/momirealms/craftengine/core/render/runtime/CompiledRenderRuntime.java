package net.momirealms.craftengine.core.render.runtime;

import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.compiled.CompiledRenderAsset;
import net.momirealms.craftengine.core.render.compiled.CompiledRenderState;
import net.momirealms.craftengine.core.render.compiled.RenderPoseBuffer;
import net.momirealms.craftengine.core.render.compiled.RenderHitBoxBuffer;
import net.momirealms.craftengine.core.render.compiled.RenderMountBuffer;

import java.util.Objects;

/**
 * Allocation-light instance runtime joining animation state, LOD cadence and dirty-frame publication.
 * It performs no scheduling and sends no packets itself.
 */
public final class CompiledRenderRuntime {
    private final CompiledRenderState state;
    private final RenderLodPolicy lodPolicy;
    private final RenderFrameBatch batch;
    private long visibleTicks;
    private float pendingDeltaSeconds;
    private boolean initialPublished;
    private boolean wasCulled = true;
    private boolean forceCurrentPose;
    private boolean dynamicPoseDirty;
    private boolean gameplayPoseAdvanced;

    public CompiledRenderRuntime(CompiledRenderAsset asset) {
        this(asset, RenderLodPolicy.DEFAULT);
    }

    public CompiledRenderRuntime(CompiledRenderAsset asset, RenderLodPolicy lodPolicy) {
        this.state = new CompiledRenderState(Objects.requireNonNull(asset, "asset"));
        this.lodPolicy = Objects.requireNonNull(lodPolicy, "lodPolicy");
        this.batch = new RenderFrameBatch(asset.rig().partCount());
    }

    public CompiledRenderState state() { return state; }
    public RenderPoseBuffer poses() { return state.poses(); }
    public RenderHitBoxBuffer hitBoxes() { return state.hitBoxes(); }
    public RenderMountBuffer mounts() { return state.mounts(); }

    /** True when advancing or flushing this runtime can still change what viewers should see. */
    public boolean needsTick() {
        return forceCurrentPose || dynamicPoseDirty || gameplayPoseAdvanced || state.needsAnimationTick();
    }
    /** Updates provider-independent owner head rotation. The latest value is sampled at the next LOD render cadence. */
    public boolean ownerHeadRotation(float x, float y, float z, float w) {
        boolean changed = state.ownerHeadRotation(x, y, z, w);
        if (changed) dynamicPoseDirty = true;
        return changed;
    }

    public boolean play(String animationId) {
        boolean started = state.play(animationId);
        if (started) {
            pendingDeltaSeconds = 0f;
            // A newer playback supersedes an unpublished rest frame left by stop().
            forceCurrentPose = false;
        }
        return started;
    }

    public boolean play(String animationId, RenderAnimationMode mode) {
        boolean started = state.play(animationId, mode);
        if (started) {
            pendingDeltaSeconds = 0f;
            // A newer playback supersedes an unpublished rest frame left by stop().
            forceCurrentPose = false;
        }
        return started;
    }

    public boolean stop() {
        boolean stopped = state.stop();
        if (stopped) {
            pendingDeltaSeconds = 0f;
            forceCurrentPose = true;
        }
        return stopped;
    }

    /** Adds/retriggers one controller-owned indexed layer and schedules its resolved pose for publication. */
    public boolean putLayer(String layerId, String animationId, RenderAnimationMode mode, int priority) {
        boolean added = state.putLayer(layerId, animationId, mode, priority);
        if (added) {
            pendingDeltaSeconds = 0f;
            forceCurrentPose = true;
        }
        return added;
    }

    /** Removes one controller-owned layer while preserving any lower selected layers. */
    public boolean removeLayer(String layerId) {
        boolean removed = state.removeLayer(layerId);
        if (removed) {
            pendingDeltaSeconds = 0f;
            forceCurrentPose = true;
        }
        return removed;
    }

    public boolean layerCompleted(String layerId) {
        return state.layerCompleted(layerId);
    }

    public boolean hasLayer(String layerId) {
        return state.hasLayer(layerId);
    }

    public boolean clearLayers() {
        boolean cleared = state.clearLayers();
        if (cleared) {
            pendingDeltaSeconds = 0f;
            forceCurrentPose = true;
        }
        return cleared;
    }


    /**
     * Advances the shared animation/pose state for gameplay consumers without publishing a visual frame.
     *
     * <p>This is intended for rigs with gameplay geometry such as hitboxes. Those rigs need an exact
     * server-tick pose even when visual LOD publishes less frequently. The same state/pose buffers are
     * used; no second animation clock is created. Visual publication later emits a full current pose at
     * the selected LOD cadence so dirty state cannot be lost between gameplay-only samples.</p>
     */
    public RenderPoseBuffer advanceForGameplay(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and >= 0");
        }
        pendingDeltaSeconds += deltaSeconds;
        if (state.needsAnimationTick() || dynamicPoseDirty) {
            state.tick(pendingDeltaSeconds);
            pendingDeltaSeconds = 0f;
            dynamicPoseDirty = false;
            gameplayPoseAdvanced = true;
        }
        return state.poses();
    }

    /**
     * Advances one scheduler tick. Hidden/culled time is accumulated without sampling, then caught up once visible.
     * Returns true only when a non-empty frame was synchronously published.
     */
    public boolean tick(float deltaSeconds, boolean hasViewers, float nearestViewerDistance, RenderFrameSink sink) {
        return tick(deltaSeconds, hasViewers, nearestViewerDistance, false, sink);
    }

    /**
     * Advances one scheduler tick with an optional lifecycle-only advance while culled.
     *
     * <p>{@code advanceWhenCulled} is intentionally opt-in. Normal rendering keeps the existing
     * zero-sampling culling behavior; terminal lifecycles such as a death animation may advance the
     * animation clock without viewers so cleanup is not indefinitely postponed.</p>
     */
    public boolean tick(float deltaSeconds, boolean hasViewers, float nearestViewerDistance,
                        boolean advanceWhenCulled, RenderFrameSink sink) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and >= 0");
        }
        Objects.requireNonNull(sink, "sink");
        pendingDeltaSeconds += deltaSeconds;
        RenderLodLevel lod = lodPolicy.select(hasViewers, nearestViewerDistance);
        if (lod.culled()) {
            wasCulled = true;
            if (advanceWhenCulled && needsTick()) {
                state.tick(pendingDeltaSeconds);
                pendingDeltaSeconds = 0f;
                dynamicPoseDirty = false;
            }
            return false;
        }

        visibleTicks++;
        if (!initialPublished || wasCulled) {
            if (pendingDeltaSeconds > 0f || dynamicPoseDirty) state.tick(pendingDeltaSeconds);
            pendingDeltaSeconds = 0f;
            batch.captureAll(state.poses(), lod);
            sink.publish(batch);
            initialPublished = true;
            wasCulled = false;
            forceCurrentPose = false;
            dynamicPoseDirty = false;
            gameplayPoseAdvanced = false;
            return !batch.empty();
        }

        wasCulled = false;
        if (forceCurrentPose) {
            // Mutations such as stop()/layer changes already resolve a new current pose immediately.
            // Do not re-resolve an idle rest pose merely because the scheduler supplied a delta: that
            // would clear the dirty bits produced by stop() before viewers receive them. If a live
            // animation/layer or a dynamic owner modifier still needs resolving, advance exactly once.
            if (dynamicPoseDirty || (pendingDeltaSeconds > 0f && state.needsAnimationTick())) {
                state.tick(pendingDeltaSeconds);
            }
            pendingDeltaSeconds = 0f;
            // A forced publication represents an ownership boundary, not a normal LOD sample.
            // Publish the complete resolved pose so a second resolve cannot lose dirty parts.
            batch.captureAll(state.poses(), lod);
            sink.publish(batch);
            forceCurrentPose = false;
            dynamicPoseDirty = false;
            gameplayPoseAdvanced = false;
            return !batch.empty();
        }
        int interval = lod.intervalTicks();
        if (visibleTicks % interval != 0) return false;

        RenderPoseBuffer poses;
        if (gameplayPoseAdvanced) {
            if (pendingDeltaSeconds > 0f || dynamicPoseDirty) {
                poses = state.tick(pendingDeltaSeconds);
            } else {
                poses = state.poses();
            }
            pendingDeltaSeconds = 0f;
            dynamicPoseDirty = false;
            batch.captureAll(poses, lod);
            gameplayPoseAdvanced = false;
        } else {
            poses = state.tick(pendingDeltaSeconds);
            pendingDeltaSeconds = 0f;
            dynamicPoseDirty = false;
            batch.captureDirty(poses, lod);
        }
        if (batch.empty()) return false;
        sink.publish(batch);
        return true;
    }
}
