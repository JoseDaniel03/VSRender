package net.momirealms.craftengine.core.render.runtime;

/** Fixed render update cadence. Interval is expressed in server ticks. */
public enum RenderLodLevel {
    FULL(1),
    HALF(2),
    QUARTER(4),
    EIGHTH(8),
    CULLED(0);

    private final int intervalTicks;

    RenderLodLevel(int intervalTicks) {
        this.intervalTicks = intervalTicks;
    }

    public int intervalTicks() {
        return intervalTicks;
    }

    public boolean culled() {
        return this == CULLED;
    }
}
