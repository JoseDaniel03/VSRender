package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.api.ranged.TrailPreset;

import java.util.Locale;
import java.util.Objects;

/** Lightweight visual trail metadata; actual rendering is platform-side and budgeted. */
public record CompiledTrail(
        TrailPreset preset,
        String particleId,
        int count,
        int intervalTicks,
        double offset,
        double extra,
        double sampleSpacing,
        int maxSamplesPerSegment
) {
    public CompiledTrail {
        preset = Objects.requireNonNull(preset, "preset");
        particleId = normalize(particleId);
        if (count < 1 || count > 64) throw new IllegalArgumentException("Trail.count must be between 1 and 64");
        if (intervalTicks < 1 || intervalTicks > 100) throw new IllegalArgumentException("Trail.intervalTicks must be between 1 and 100");
        if (offset < 0.0 || offset > 8.0 || !Double.isFinite(offset)) throw new IllegalArgumentException("Trail.offset must be finite and between 0 and 8");
        if (extra < 0.0 || extra > 8.0 || !Double.isFinite(extra)) throw new IllegalArgumentException("Trail.extra must be finite and between 0 and 8");
        if (sampleSpacing <= 0.0 || sampleSpacing > 16.0 || !Double.isFinite(sampleSpacing)) {
            throw new IllegalArgumentException("Trail.sampleSpacing must be finite and in (0, 16]");
        }
        if (maxSamplesPerSegment < 1 || maxSamplesPerSegment > 128) {
            throw new IllegalArgumentException("Trail.maxSamplesPerSegment must be between 1 and 128");
        }
    }

    private static String normalize(String raw) {
        String value = Objects.requireNonNull(raw, "particleId").trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException("Trail.particleId cannot be blank");
        return value.indexOf(':') >= 0 ? value : "minecraft:" + value;
    }
}
