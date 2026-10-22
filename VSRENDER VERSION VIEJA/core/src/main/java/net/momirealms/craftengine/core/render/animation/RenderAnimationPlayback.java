package net.momirealms.craftengine.core.render.animation;

import java.util.Map;
import java.util.Objects;

/**
 * Tick-driven native animation clock. It owns playback time only; rendering/scheduling remains external.
 */
public final class RenderAnimationPlayback {
    private final RenderAnimationClip clip;
    private final RenderAnimationMode mode;
    private float timeSeconds;
    private boolean completed;
    private boolean holding;

    public RenderAnimationPlayback(RenderAnimationClip clip) {
        this(clip, clip.defaultMode());
    }

    public RenderAnimationPlayback(RenderAnimationClip clip, RenderAnimationMode mode) {
        this.clip = Objects.requireNonNull(clip, "clip");
        this.mode = Objects.requireNonNull(mode, "mode");
        reset();
    }

    public RenderAnimationClip clip() {
        return clip;
    }

    public RenderAnimationMode mode() {
        return mode;
    }

    public float timeSeconds() {
        return timeSeconds;
    }

    public boolean completed() {
        return completed;
    }

    public boolean holding() {
        return holding;
    }

    public Map<String, RenderBonePose> sample() {
        return RenderAnimationSampler.sample(clip, timeSeconds, mode);
    }

    public Map<String, RenderAnimationSample> sampleState() {
        return RenderAnimationSampler.sampleState(clip, timeSeconds, mode);
    }

    public Map<String, RenderBonePose> advance(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and >= 0");
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
                if (length == 0f) {
                    timeSeconds = 0f;
                } else {
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
        return sample();
    }

    public Map<String, RenderAnimationSample> advanceState(float deltaSeconds) {
        advance(deltaSeconds);
        return sampleState();
    }

    public void reset() {
        timeSeconds = 0f;
        completed = mode == RenderAnimationMode.ONCE && clip.lengthSeconds() == 0f;
        holding = mode == RenderAnimationMode.HOLD && clip.lengthSeconds() == 0f;
    }
}
