"""Regression checks for Folia/Canvas region ownership during 26.3 porting.

This lightweight source-contract test can run before private compile-only JARs
are available. It does not replace Java integration tests on a live server.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEDULER = ROOT / (
    "bukkit/src/main/java/net/momirealms/craftengine/"
    "bukkit/plugin/scheduler/impl/AbstractBukkitExecutor.java"
)


class FoliaRegionForwardingTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = SCHEDULER.read_text(encoding="utf-8")

    def test_delayed_core_world_task_keeps_requested_region_coordinates(self):
        method = re.search(
            r"public SchedulerTask runLater\(Runnable r, long delay, "
            r"net\.momirealms\.craftengine\.core\.world\.World world, int x, int z\)"
            r"\s*\{(?P<body>.*?)\}",
            self.source,
            re.DOTALL,
        )
        self.assertIsNotNone(method, "Missing core world delayed scheduler override")
        forwarded = re.sub(r"\s+", " ", method.group("body"))
        self.assertIn(
            "runLater(r, delay, world == null ? null : (World) world.platformWorld(), x, z)",
            forwarded,
            "Delayed tasks must run in the region owning the original chunk, not (0, 0)",
        )

    def test_async_scheduler_saturates_tick_to_millisecond_conversion(self):
        folia = (ROOT / (
            "bukkit/src/main/java/net/momirealms/craftengine/"
            "bukkit/plugin/scheduler/impl/FoliaExecutor.java"
        )).read_text(encoding="utf-8")
        durations = (ROOT / (
            "bukkit/src/main/java/net/momirealms/craftengine/"
            "bukkit/plugin/scheduler/impl/FoliaTickDurations.java"
        )).read_text(encoding="utf-8")
        self.assertIn("ticks > Long.MAX_VALUE / 50L ? Long.MAX_VALUE : ticks * 50L", durations)
        self.assertIn("FoliaTickDurations.toMillis(delayTicks)", folia)
        self.assertIn("FoliaTickDurations.repeatingPeriodMillis(periodTicks)", folia)
        self.assertIn("return toMillis(Math.max(1L, ticks))", durations)
        self.assertNotIn("delayTicks * 50,", folia)
        self.assertNotIn("periodTicks * 50,", folia)

    def test_location_overload_passes_block_to_chunk_coordinates(self):
        self.assertIn(
            "runLater(r, delay, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4)",
            self.source,
        )


if __name__ == "__main__":
    unittest.main()
