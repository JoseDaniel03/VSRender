package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.api.ranged.FireMode;
import com.virgolandstudio.items.api.ranged.ReloadFirePolicy;

import java.util.LinkedHashSet;
import java.util.List;

public record CompiledFireControl(
        FireMode mode,
        List<FireMode> availableModes,
        String modeStateKey,
        double roundsPerMinute,
        int burstSize,
        int autoLeaseTicks,
        int patternResetTicks,
        ReloadFirePolicy reloadFirePolicy,
        FireMode secondaryMode,
        double secondaryRoundsPerMinute,
        int secondaryBurstSize
) {
    /** Source compatibility constructor retained for callers that do not author secondary fire. */
    public CompiledFireControl(
            FireMode mode,
            List<FireMode> availableModes,
            String modeStateKey,
            double roundsPerMinute,
            int burstSize,
            int autoLeaseTicks,
            int patternResetTicks,
            ReloadFirePolicy reloadFirePolicy
    ) {
        this(mode, availableModes, modeStateKey, roundsPerMinute, burstSize, autoLeaseTicks,
                patternResetTicks, reloadFirePolicy, null, roundsPerMinute, burstSize);
    }

    /** Binary/source compatibility constructor retained for callers built before 0.51.1. */
    public CompiledFireControl(
            FireMode mode,
            List<FireMode> availableModes,
            String modeStateKey,
            double roundsPerMinute,
            int burstSize,
            int autoLeaseTicks,
            int patternResetTicks
    ) {
        this(mode, availableModes, modeStateKey, roundsPerMinute, burstSize, autoLeaseTicks,
                patternResetTicks, ReloadFirePolicy.BLOCK);
    }

    public CompiledFireControl(FireMode mode, double roundsPerMinute, int burstSize, int autoLeaseTicks) {
        this(mode, List.of(mode), "", roundsPerMinute, burstSize, autoLeaseTicks, 8, ReloadFirePolicy.BLOCK);
    }

    public CompiledFireControl(FireMode mode, double roundsPerMinute, int burstSize, int autoLeaseTicks, int patternResetTicks) {
        this(mode, List.of(mode), "", roundsPerMinute, burstSize, autoLeaseTicks, patternResetTicks, ReloadFirePolicy.BLOCK);
    }

    public CompiledFireControl {
        if (mode == null) throw new IllegalArgumentException("mode cannot be null");
        if (availableModes == null || availableModes.isEmpty()) availableModes = List.of(mode);
        var unique = new LinkedHashSet<FireMode>();
        for (FireMode candidate : availableModes) {
            if (candidate == null) throw new IllegalArgumentException("availableModes cannot contain null");
            unique.add(candidate);
        }
        availableModes = List.copyOf(unique);
        if (!availableModes.contains(mode)) {
            throw new IllegalArgumentException("default fire mode must be included in availableModes");
        }
        modeStateKey = modeStateKey == null ? "" : modeStateKey.trim();
        if (availableModes.size() > 1 && modeStateKey.isBlank()) {
            throw new IllegalArgumentException("modeStateKey is required when multiple fire modes are available");
        }
        if (!(roundsPerMinute > 0.0) || !Double.isFinite(roundsPerMinute)) {
            throw new IllegalArgumentException("roundsPerMinute must be finite and > 0");
        }
        if (burstSize < 1) throw new IllegalArgumentException("burstSize must be >= 1");
        if (autoLeaseTicks < 1) throw new IllegalArgumentException("autoLeaseTicks must be >= 1");
        if (patternResetTicks < 1 || patternResetTicks > 1200) {
            throw new IllegalArgumentException("patternResetTicks must be between 1 and 1200");
        }
        if (reloadFirePolicy == null) reloadFirePolicy = ReloadFirePolicy.BLOCK;
        if (!(secondaryRoundsPerMinute > 0.0) || !Double.isFinite(secondaryRoundsPerMinute)) {
            throw new IllegalArgumentException("secondaryRoundsPerMinute must be finite and > 0");
        }
        if (secondaryBurstSize < 1) throw new IllegalArgumentException("secondaryBurstSize must be >= 1");
    }

    public boolean supports(FireMode candidate) { return candidate != null && availableModes.contains(candidate); }
    public boolean hasSecondaryFire() { return secondaryMode != null; }

    public CompiledFireControl withMode(FireMode selected) {
        if (!supports(selected)) throw new IllegalArgumentException("Unsupported fire mode: " + selected);
        if (selected == mode) return this;
        return new CompiledFireControl(
                selected, availableModes, modeStateKey,
                roundsPerMinute, burstSize, autoLeaseTicks, patternResetTicks, reloadFirePolicy,
                secondaryMode, secondaryRoundsPerMinute, secondaryBurstSize
        );
    }

    /** Builds an ephemeral cadence profile for a secondary input without changing persisted fire mode. */
    public CompiledFireControl secondaryControl() {
        if (secondaryMode == null) return null;
        return new CompiledFireControl(
                secondaryMode, List.of(secondaryMode), "",
                secondaryRoundsPerMinute, secondaryBurstSize, autoLeaseTicks, patternResetTicks,
                reloadFirePolicy, null, secondaryRoundsPerMinute, secondaryBurstSize
        );
    }

    /** 20 ticks/s * 60 s/min = 1200 ticks/minute. Fractional cadence is preserved. */
    public double intervalTicks() { return 1200.0 / roundsPerMinute; }
}
