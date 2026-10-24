package com.virgolandstudio.items.core.world;

public record CompiledBlockHold(double distance, double verticalOffset) {
    public CompiledBlockHold {
        if (!Double.isFinite(distance) || distance < 0.5 || distance > 12.0) {
            throw new IllegalArgumentException("BlockHold.distance must be in [0.5, 12]");
        }
        if (!Double.isFinite(verticalOffset) || verticalOffset < -6.0 || verticalOffset > 6.0) {
            throw new IllegalArgumentException("BlockHold.verticalOffset must be in [-6, 6]");
        }
    }
}
