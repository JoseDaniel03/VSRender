package net.momirealms.craftengine.core.render.animation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Deterministic sampler for native VsRender animation data. */
public final class RenderAnimationSampler {
    private RenderAnimationSampler() {
    }

    public static RenderBonePose sample(RenderAnimationTrack track, float timeSeconds) {
        return sampleState(track, timeSeconds).pose();
    }

    public static RenderAnimationSample sampleState(RenderAnimationTrack track, float timeSeconds) {
        Objects.requireNonNull(track, "track");
        if (!Float.isFinite(timeSeconds)) throw new IllegalArgumentException("timeSeconds must be finite");
        var frames = track.frames();
        if (timeSeconds <= frames.get(0).timeSeconds()) {
            RenderAnimationFrame frame = frames.get(0);
            return new RenderAnimationSample(frame.pose(), frame.globalRotation());
        }
        int last = frames.size() - 1;
        if (timeSeconds >= frames.get(last).timeSeconds()) {
            RenderAnimationFrame frame = frames.get(last);
            return new RenderAnimationSample(frame.pose(), frame.globalRotation());
        }

        int low = 0;
        int high = last;
        while (low + 1 < high) {
            int mid = (low + high) >>> 1;
            if (frames.get(mid).timeSeconds() <= timeSeconds) low = mid;
            else high = mid;
        }

        RenderAnimationFrame from = frames.get(low);
        RenderAnimationFrame to = frames.get(high);
        RenderBonePose pose;
        if (from.interpolation() == RenderInterpolation.STEP) {
            pose = from.pose();
        } else {
            float span = to.timeSeconds() - from.timeSeconds();
            float alpha = (timeSeconds - from.timeSeconds()) / span;
            pose = from.pose().lerp(to.pose(), alpha);
        }
        return new RenderAnimationSample(pose, from.globalRotation());
    }

    public static Map<String, RenderBonePose> sample(RenderAnimationClip clip, float timeSeconds, RenderAnimationMode mode) {
        Objects.requireNonNull(clip, "clip");
        Objects.requireNonNull(mode, "mode");
        if (!Float.isFinite(timeSeconds)) throw new IllegalArgumentException("timeSeconds must be finite");
        float t = normalizeTime(clip.lengthSeconds(), timeSeconds, mode);
        LinkedHashMap<String, RenderBonePose> result = new LinkedHashMap<>();
        clip.tracks().forEach((id, track) -> result.put(id, sample(track, t)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, RenderAnimationSample> sampleState(RenderAnimationClip clip, float timeSeconds, RenderAnimationMode mode) {
        Objects.requireNonNull(clip, "clip");
        Objects.requireNonNull(mode, "mode");
        if (!Float.isFinite(timeSeconds)) throw new IllegalArgumentException("timeSeconds must be finite");
        float t = normalizeTime(clip.lengthSeconds(), timeSeconds, mode);
        LinkedHashMap<String, RenderAnimationSample> result = new LinkedHashMap<>();
        clip.tracks().forEach((id, track) -> result.put(id, sampleState(track, t)));
        return Collections.unmodifiableMap(result);
    }

    static float normalizeTime(float lengthSeconds, float timeSeconds, RenderAnimationMode mode) {
        if (lengthSeconds == 0f) return 0f;
        if (mode == RenderAnimationMode.LOOP) {
            float wrapped = timeSeconds % lengthSeconds;
            return wrapped < 0f ? wrapped + lengthSeconds : wrapped;
        }
        if (timeSeconds <= 0f) return 0f;
        return Math.min(timeSeconds, lengthSeconds);
    }
}
