package com.virgolandstudio.items.core.resource;

import com.virgolandstudio.items.core.state.StateSlot;

public record CompiledEnergy(
        StateSlot stateSlot,
        double capacity,
        double regenPerSecond,
        int regenDelayTicks
) {
    public CompiledEnergy {
        if (!Double.isFinite(capacity) || capacity <= 0.0) throw new IllegalArgumentException("Energy.capacity must be finite and > 0");
        if (!Double.isFinite(regenPerSecond) || regenPerSecond < 0.0 || regenPerSecond > 100000.0) {
            throw new IllegalArgumentException("Energy.regenPerSecond must be finite and >= 0");
        }
        if (regenDelayTicks < 0 || regenDelayTicks > 72000) throw new IllegalArgumentException("Energy.regenDelayTicks must be in [0, 72000]");
    }
}
