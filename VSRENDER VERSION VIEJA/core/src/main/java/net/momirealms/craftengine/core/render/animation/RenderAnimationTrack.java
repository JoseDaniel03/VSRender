package net.momirealms.craftengine.core.render.animation;

import java.util.List;
import java.util.Objects;

/** Ordered immutable animation frames for one rig part. */
public record RenderAnimationTrack(String partId, List<RenderAnimationFrame> frames) {
    public RenderAnimationTrack {
        Objects.requireNonNull(partId, "partId");
        if (partId.isBlank()) throw new IllegalArgumentException("partId must not be blank");
        Objects.requireNonNull(frames, "frames");
        frames = List.copyOf(frames);
        if (frames.isEmpty()) throw new IllegalArgumentException("frames must not be empty");
        float previous = -1f;
        for (RenderAnimationFrame frame : frames) {
            Objects.requireNonNull(frame, "frame");
            if (frame.timeSeconds() <= previous) {
                throw new IllegalArgumentException("frame times must be strictly increasing");
            }
            previous = frame.timeSeconds();
        }
    }

    public float endTimeSeconds() {
        return frames.get(frames.size() - 1).timeSeconds();
    }
}
