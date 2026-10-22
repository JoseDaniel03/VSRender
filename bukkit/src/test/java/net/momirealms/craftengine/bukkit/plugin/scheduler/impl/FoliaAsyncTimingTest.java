package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoliaAsyncTimingTest {
    @Test
    void convertsTicksToMilliseconds() {
        assertEquals(50L, FoliaTickDurations.toMillis(1L));
        assertEquals(250L, FoliaTickDurations.toMillis(5L));
    }

    @Test
    void treatsNegativeAndZeroDelayAsImmediate() {
        assertEquals(0L, FoliaTickDurations.toMillis(-1L));
        assertEquals(0L, FoliaTickDurations.toMillis(0L));
    }

    @Test
    void clampsNonpositiveRepeatPeriodsToOneTick() {
        assertEquals(50L, FoliaTickDurations.repeatingPeriodMillis(-5L));
        assertEquals(50L, FoliaTickDurations.repeatingPeriodMillis(0L));
        assertEquals(100L, FoliaTickDurations.repeatingPeriodMillis(2L));
    }

    @Test
    void saturatesInsteadOfOverflowingLargeDelays() {
        long maxTicks = Long.MAX_VALUE / 50L;
        assertEquals(maxTicks * 50L, FoliaTickDurations.toMillis(maxTicks));
        assertEquals(Long.MAX_VALUE, FoliaTickDurations.toMillis(maxTicks + 1L));
        assertEquals(Long.MAX_VALUE, FoliaTickDurations.toMillis(Long.MAX_VALUE));
    }
}
