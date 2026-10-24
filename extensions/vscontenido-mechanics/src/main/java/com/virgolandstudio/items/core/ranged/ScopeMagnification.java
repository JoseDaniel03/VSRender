package com.virgolandstudio.items.core.ranged;

/** Pure conversion used by the Paper FOV transport for vanilla-client scope magnification. */
public final class ScopeMagnification {
    private ScopeMagnification() {}

    /**
     * Converts Minecraft's practical 1x..10x zoom scale into the walk-speed value consumed by
     * the client-side FOV calculation in the player abilities packet. Infinity at exactly 2x is
     * intentional and understood by the vanilla client. The real server movement speed is not mutated.
     */
    public static float clientAbilityWalkSpeed(double magnification) {
        if (!Double.isFinite(magnification) || magnification < 1.0 || magnification > 10.0) {
            throw new IllegalArgumentException("Scope magnification must be finite and within [1,10]");
        }
        return (float) (1.0 / (20.0 / magnification - 10.0));
    }
}
