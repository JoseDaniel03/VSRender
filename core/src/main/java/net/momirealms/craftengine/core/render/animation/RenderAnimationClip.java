package net.momirealms.craftengine.core.render.animation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable provider-independent animation clip. */
public record RenderAnimationClip(
        String animationId,
        float lengthSeconds,
        RenderAnimationMode defaultMode,
        boolean override,
        Map<String, RenderAnimationTrack> tracks
) {
    public RenderAnimationClip {
        Objects.requireNonNull(animationId, "animationId");
        if (animationId.isBlank()) throw new IllegalArgumentException("animationId must not be blank");
        if (!Float.isFinite(lengthSeconds) || lengthSeconds < 0f) {
            throw new IllegalArgumentException("lengthSeconds must be finite and >= 0");
        }
        Objects.requireNonNull(defaultMode, "defaultMode");
        Objects.requireNonNull(tracks, "tracks");
        LinkedHashMap<String, RenderAnimationTrack> copy = new LinkedHashMap<>();
        for (Map.Entry<String, RenderAnimationTrack> entry : tracks.entrySet()) {
            String key = Objects.requireNonNull(entry.getKey(), "track id");
            RenderAnimationTrack track = Objects.requireNonNull(entry.getValue(), "track");
            if (!key.equals(track.partId())) {
                throw new IllegalArgumentException("track key does not match partId: " + key + " != " + track.partId());
            }
            if (track.endTimeSeconds() > lengthSeconds) {
                throw new IllegalArgumentException("track " + key + " extends past clip length");
            }
            copy.put(key, track);
        }
        tracks = Collections.unmodifiableMap(copy);
    }
}
