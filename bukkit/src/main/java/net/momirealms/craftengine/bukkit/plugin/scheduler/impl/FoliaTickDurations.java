package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

/**
 * Converts Minecraft tick durations to wall-clock delays for Folia's async scheduler.
 * Deliberately free of Bukkit/Paper classes so edge cases can be tested independently.
 */
final class FoliaTickDurations {
    private FoliaTickDurations() {
    }

    static long toMillis(long ticks) {
        if (ticks <= 0L) return 0L;
        return ticks > Long.MAX_VALUE / 50L ? Long.MAX_VALUE : ticks * 50L;
    }

    static long repeatingPeriodMillis(long ticks) {
        return toMillis(Math.max(1L, ticks));
    }
}
