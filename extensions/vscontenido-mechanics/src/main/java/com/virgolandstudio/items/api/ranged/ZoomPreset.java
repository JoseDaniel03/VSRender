package com.virgolandstudio.items.api.ranged;

/** Reusable authored zoom presets. CUSTOM uses the values stored directly on the Zoom component. */
public enum ZoomPreset {
    IRON_SIGHTS(1.15, 0.90, 0.85),
    RED_DOT(1.35, 0.85, 0.72),
    SCOPE_2X(2.00, 0.65, 0.52),
    SCOPE_4X(4.00, 0.45, 0.34),
    SCOPE_6X(6.00, 0.35, 0.26),
    SCOPE_8X(8.00, 0.28, 0.20),
    CUSTOM(1.00, 1.00, 1.00);

    private final double magnification;
    private final double movementMultiplier;
    private final double spreadMultiplier;

    ZoomPreset(double magnification, double movementMultiplier, double spreadMultiplier) {
        this.magnification = magnification;
        this.movementMultiplier = movementMultiplier;
        this.spreadMultiplier = spreadMultiplier;
    }

    public double magnification() { return magnification; }
    public double movementMultiplier() { return movementMultiplier; }
    public double spreadMultiplier() { return spreadMultiplier; }
}
