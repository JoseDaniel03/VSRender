package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;

import java.util.Objects;

/**
 * Minimal mutable state owned by one rendered instance.
 * Shared model/rig/clip data remains in {@link CompiledRenderAsset}; this object only owns playback and pose buffers.
 */
public final class CompiledRenderState {
    private final CompiledRenderAsset asset;
    private final RenderAnimationBuffer animationBuffer;
    private final RenderPoseBuffer poseBuffer;
    private final RenderHitBoxBuffer hitBoxBuffer;
    private final RenderMountBuffer mountBuffer;
    private final IndexedAnimationLayerStack animationLayers;
    private IndexedAnimationClip activeClip;
    private RenderAnimationMode mode;
    private float timeSeconds;
    private boolean completed;
    private boolean holding;
    private boolean layered;
    private float ownerHeadQx, ownerHeadQy, ownerHeadQz, ownerHeadQw = 1f;

    public CompiledRenderState(CompiledRenderAsset asset) {
        this.asset = Objects.requireNonNull(asset, "asset");
        animationBuffer = asset.createAnimationBuffer();
        poseBuffer = asset.createPoseBuffer();
        hitBoxBuffer = asset.createHitBoxBuffer();
        mountBuffer = asset.createMountBuffer();
        animationLayers = new IndexedAnimationLayerStack(asset.rig());
        // Prime the initial rest pose. First renderer consumption sees every part dirty.
        resolvePose();
    }

    public CompiledRenderAsset asset() { return asset; }
    public RenderPoseBuffer poses() { return poseBuffer; }
    public RenderHitBoxBuffer hitBoxes() { return hitBoxBuffer; }
    public RenderMountBuffer mounts() { return mountBuffer; }
    public boolean playing() { return layered ? animationLayers.size() > 0 : activeClip != null; }
    public String animationId() { return layered ? animationLayers.topAnimationId() : activeClip == null ? null : activeClip.animationId(); }
    public RenderAnimationMode mode() { return layered ? animationLayers.topMode() : mode; }
    public float timeSeconds() { return layered ? animationLayers.topTimeSeconds() : timeSeconds; }
    public boolean completed() { return layered ? animationLayers.topCompleted() : completed; }
    public boolean holding() { return layered ? animationLayers.topHolding() : holding; }
    public boolean layered() { return layered; }

    /** True when advancing the animation clock can still change at least one selected local pose. */
    public boolean needsAnimationTick() {
        if (layered) return animationLayers.needsTick();
        return activeClip != null && !completed && !holding;
    }

    /** Updates the owner-head modifier without resolving immediately. Returns true only when it changed. */
    public boolean ownerHeadRotation(float x, float y, float z, float w) {
        if (!asset.rig().hasOwnerHeadRotation()) return false;
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(w)) {
            throw new IllegalArgumentException("owner head quaternion must be finite");
        }
        double lenSq = (double) x * x + (double) y * y + (double) z * z + (double) w * w;
        if (!(lenSq > 0d) || !Double.isFinite(lenSq)) {
            throw new IllegalArgumentException("owner head quaternion must have non-zero finite length");
        }
        float inv = (float) (1d / Math.sqrt(lenSq));
        x *= inv; y *= inv; z *= inv; w *= inv;
        float dot = Math.abs(ownerHeadQx * x + ownerHeadQy * y + ownerHeadQz * z + ownerHeadQw * w);
        if (1f - dot <= 1e-5f) return false;
        ownerHeadQx = x; ownerHeadQy = y; ownerHeadQz = z; ownerHeadQw = w;
        return true;
    }

    public boolean play(String animationId) {
        IndexedAnimationClip clip = asset.animationOrNull(animationId);
        return clip != null && play(clip, clip.defaultMode());
    }

    public boolean play(String animationId, RenderAnimationMode mode) {
        IndexedAnimationClip clip = asset.animationOrNull(animationId);
        return clip != null && play(clip, mode);
    }

    private boolean play(IndexedAnimationClip clip, RenderAnimationMode mode) {
        if (layered) {
            animationLayers.clear();
            layered = false;
        }
        this.activeClip = clip;
        this.mode = Objects.requireNonNull(mode, "mode");
        timeSeconds = 0f;
        completed = mode == RenderAnimationMode.ONCE && clip.lengthSeconds() == 0f;
        holding = mode == RenderAnimationMode.HOLD && clip.lengthSeconds() == 0f;
        IndexedAnimationSampler.sampleNormalized(clip, timeSeconds, animationBuffer);
        resolvePose();
        return true;
    }

    /**
     * Adds/retriggers one indexed animation layer. Layered playback and the legacy single-playback
     * clock are mutually exclusive; the single clock is discarded only after a valid layer was
     * accepted by the stack.
     */
    public boolean putLayer(String layerId, String animationId, RenderAnimationMode mode, int priority) {
        Objects.requireNonNull(layerId, "layerId");
        Objects.requireNonNull(animationId, "animationId");
        Objects.requireNonNull(mode, "mode");
        IndexedAnimationClip clip = asset.animationOrNull(animationId);
        if (clip == null) return false;

        boolean enteringLayered = !layered;
        if (enteringLayered) animationLayers.clear();
        if (!animationLayers.put(layerId, clip, mode, priority)) {
            if (enteringLayered) animationLayers.clear();
            return false;
        }
        if (enteringLayered) {
            activeClip = null;
            this.mode = null;
            timeSeconds = 0f;
            completed = false;
            holding = false;
            layered = true;
        }
        animationLayers.tick(0f, animationBuffer);
        resolvePose();
        return true;
    }

    public boolean removeLayer(String layerId) {
        Objects.requireNonNull(layerId, "layerId");
        if (!layered || !animationLayers.remove(layerId)) return false;
        if (animationLayers.size() == 0) {
            animationLayers.clear();
            layered = false;
            animationBuffer.reset();
        } else {
            animationLayers.tick(0f, animationBuffer);
        }
        resolvePose();
        return true;
    }

    public boolean layerCompleted(String layerId) {
        return layered && animationLayers.completed(layerId);
    }

    public boolean hasLayer(String layerId) {
        return layered && animationLayers.contains(layerId);
    }

    public boolean clearLayers() {
        if (!layered && animationLayers.size() == 0) return false;
        animationLayers.clear();
        layered = false;
        activeClip = null;
        mode = null;
        timeSeconds = 0f;
        completed = false;
        holding = false;
        animationBuffer.reset();
        resolvePose();
        return true;
    }

    /** Returns true when an active animation was stopped. Rest pose becomes the next current pose immediately. */
    public boolean stop() {
        if (layered) return clearLayers();
        if (activeClip == null) return false;
        activeClip = null;
        mode = null;
        timeSeconds = 0f;
        completed = false;
        holding = false;
        animationBuffer.reset();
        resolvePose();
        return true;
    }

    /**
     * Advances native animation state and resolves the current pose. No scheduling or packets are performed here.
     * The returned buffer belongs to this state and is reused every tick.
     */
    public RenderPoseBuffer tick(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and >= 0");
        }
        if (layered) {
            animationLayers.tick(deltaSeconds, animationBuffer);
            resolvePose();
            return poseBuffer;
        }

        IndexedAnimationClip clip = activeClip;
        if (clip == null) {
            resolvePose();
            return poseBuffer;
        }
        float length = clip.lengthSeconds();
        switch (mode) {
            case ONCE -> {
                if (!completed) {
                    timeSeconds = Math.min(length, timeSeconds + deltaSeconds);
                    completed = timeSeconds >= length;
                }
            }
            case LOOP -> {
                if (length == 0f) timeSeconds = 0f;
                else {
                    float next = (timeSeconds + deltaSeconds) % length;
                    timeSeconds = next < 0f ? next + length : next;
                }
            }
            case HOLD -> {
                if (!holding) {
                    timeSeconds = Math.min(length, timeSeconds + deltaSeconds);
                    holding = timeSeconds >= length;
                }
            }
        }
        IndexedAnimationSampler.sampleNormalized(clip, timeSeconds, animationBuffer);
        resolvePose();
        return poseBuffer;
    }

    private void resolvePose() {
        IndexedRigPoseResolver.resolve(asset.rig(), animationBuffer, poseBuffer,
                ownerHeadQx, ownerHeadQy, ownerHeadQz, ownerHeadQw);
        IndexedHitBoxPoseResolver.resolve(asset.rig(), poseBuffer, hitBoxBuffer);
        IndexedMountPoseResolver.resolve(asset.rig(), poseBuffer, mountBuffer);
    }
}
