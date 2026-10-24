package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.api.ranged.ZoomPreset;

/** Compiled ADS/zoom settings. Platform adapters decide how the visual zoom is represented. */
public record CompiledZoom(
        ZoomPreset preset,
        double magnification,
        double movementMultiplier,
        double spreadMultiplier,
        boolean sneakToZoom,
        boolean vanillaSpyglass,
        int activationDelayTicks,
        int reentryDelayTicks
) {
    public CompiledZoom {
        if (preset == null) throw new IllegalArgumentException("Zoom preset is required");
        if (!Double.isFinite(magnification) || magnification < 1.0 || magnification > 10.0) {
            throw new IllegalArgumentException("Zoom magnification must be finite and within [1,10]");
        }
        if (!Double.isFinite(movementMultiplier) || movementMultiplier <= 0.0 || movementMultiplier > 1.0) {
            throw new IllegalArgumentException("Zoom movementMultiplier must be within (0,1]");
        }
        if (!Double.isFinite(spreadMultiplier) || spreadMultiplier <= 0.0 || spreadMultiplier > 1.0) {
            throw new IllegalArgumentException("Zoom spreadMultiplier must be within (0,1]");
        }
        if (activationDelayTicks < 0 || activationDelayTicks > 200) {
            throw new IllegalArgumentException("Zoom activationDelayTicks must be within [0,200]");
        }
        if (reentryDelayTicks < 0 || reentryDelayTicks > 200) {
            throw new IllegalArgumentException("Zoom reentryDelayTicks must be within [0,200]");
        }
    }
}
