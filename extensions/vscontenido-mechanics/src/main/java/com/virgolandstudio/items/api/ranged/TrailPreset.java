package com.virgolandstudio.items.api.ranged;

/** Reusable projectile-trail sampling presets. CUSTOM uses authored spacing/sample limits. */
public enum TrailPreset {
    LOW(1.00, 8),
    MEDIUM(0.50, 16),
    HIGH(0.25, 24),
    CUSTOM(0.50, 16);

    private final double sampleSpacing;
    private final int maxSamplesPerSegment;

    TrailPreset(double sampleSpacing, int maxSamplesPerSegment) {
        this.sampleSpacing = sampleSpacing;
        this.maxSamplesPerSegment = maxSamplesPerSegment;
    }

    public double sampleSpacing() { return sampleSpacing; }
    public int maxSamplesPerSegment() { return maxSamplesPerSegment; }
}
