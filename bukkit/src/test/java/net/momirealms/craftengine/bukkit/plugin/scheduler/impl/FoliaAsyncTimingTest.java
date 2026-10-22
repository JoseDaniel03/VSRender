package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoliaAsyncTimingTest {
    @Test
    void convertsTicksToMilliseconds() {
        assertEquals(50L, FoliaExecutor.ticksToMillis(1L));
        assertEquals(250L, FoliaExecutor.ticksToMillis(5L));
    }

    @Test
    void treatsNegativeAndZeroDelayAsImmediate() {
        assertEquals(0L, FoliaExecutor.ticksToMillis(-1L));
        assertEquals(0L, FoliaExecutor.ticksToMillis(0L));
    }

    @Test
    void saturatesInsteadOfOverflowingLargeDelays() {
        long maxTicks = Long.MAX_VALUE / 50L;
        assertEquals(maxTicks * 50L, FoliaExecutor.ticksToMillis(maxTicks));
        assertEquals(Long.MAX_VALUE, FoliaExecutor.ticksToMillis(maxTicks + 1L));
        assertEquals(Long.MAX_VALUE, FoliaExecutor.ticksToMillis(Long.MAX_VALUE));
    }
}
