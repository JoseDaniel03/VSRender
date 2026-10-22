package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;

import java.util.Objects;

/** Allocation-free sampler for compiled animation clips. */
public final class IndexedAnimationSampler {
    private IndexedAnimationSampler() {
    }

    public static void sample(IndexedAnimationClip clip,
                              float timeSeconds,
                              RenderAnimationMode mode,
                              RenderAnimationBuffer out) {
        Objects.requireNonNull(clip, "clip");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(out, "out");
        if (out.rig() != clip.rig()) throw new IllegalArgumentException("buffer/clip rig mismatch");
        if (!Float.isFinite(timeSeconds)) throw new IllegalArgumentException("timeSeconds must be finite");
        float t = normalizeTime(clip.lengthSeconds(), timeSeconds, mode);
        sampleNormalized(clip, t, out);
    }

    static void sampleNormalized(IndexedAnimationClip clip, float normalizedTimeSeconds, RenderAnimationBuffer out) {
        if (out.rig() != clip.rig()) throw new IllegalArgumentException("buffer/clip rig mismatch");
        if (!Float.isFinite(normalizedTimeSeconds)) throw new IllegalArgumentException("normalizedTimeSeconds must be finite");
        out.reset();
        for (IndexedAnimationTrack track : clip.tracks()) samplePart(track, normalizedTimeSeconds, out);
    }

    static void samplePart(IndexedAnimationTrack track, float time, RenderAnimationBuffer out) {
        int last = track.time.length - 1;
        if (time <= track.time[0]) {
            copyFrame(track, 0, out);
            return;
        }
        if (time >= track.time[last]) {
            copyFrame(track, last, out);
            return;
        }

        int low = 0;
        int high = last;
        while (low + 1 < high) {
            int mid = (low + high) >>> 1;
            if (track.time[mid] <= time) low = mid;
            else high = mid;
        }
        if (track.interpolation[low] == 0) {
            copyFrame(track, low, out);
            return;
        }
        float alpha = (time - track.time[low]) / (track.time[high] - track.time[low]);
        int p = track.partIndex;
        out.set(p,
                lerp(track.tx[low], track.tx[high], alpha),
                lerp(track.ty[low], track.ty[high], alpha),
                lerp(track.tz[low], track.tz[high], alpha),
                lerp(track.sx[low], track.sx[high], alpha),
                lerp(track.sy[low], track.sy[high], alpha),
                lerp(track.sz[low], track.sz[high], alpha),
                lerp(track.rx[low], track.rx[high], alpha),
                lerp(track.ry[low], track.ry[high], alpha),
                lerp(track.rz[low], track.rz[high], alpha),
                track.globalRotation[low]);
    }

    private static void copyFrame(IndexedAnimationTrack track, int frame, RenderAnimationBuffer out) {
        out.set(track.partIndex,
                track.tx[frame], track.ty[frame], track.tz[frame],
                track.sx[frame], track.sy[frame], track.sz[frame],
                track.rx[frame], track.ry[frame], track.rz[frame],
                track.globalRotation[frame]);
    }

    private static float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    private static float normalizeTime(float lengthSeconds, float timeSeconds, RenderAnimationMode mode) {
        if (lengthSeconds == 0f) return 0f;
        if (mode == RenderAnimationMode.LOOP) {
            float wrapped = timeSeconds % lengthSeconds;
            return wrapped < 0f ? wrapped + lengthSeconds : wrapped;
        }
        if (timeSeconds <= 0f) return 0f;
        return Math.min(timeSeconds, lengthSeconds);
    }
}
