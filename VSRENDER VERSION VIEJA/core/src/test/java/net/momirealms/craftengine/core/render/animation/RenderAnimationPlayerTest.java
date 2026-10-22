package net.momirealms.craftengine.core.render.animation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RenderAnimationPlayerTest {

    @Test
    void onceFinishesOnlyAfterFinalPoseIsAccepted() {
        AtomicInteger calls = new AtomicInteger();
        RenderAnimationPlayer player = new RenderAnimationPlayer(clip(RenderAnimationMode.ONCE), RenderAnimationMode.ONCE, samples -> calls.incrementAndGet() > 1);

        assertEquals(RenderAnimationPlayer.TickResult.REJECTED, player.tick(1f));
        assertFalse(player.isFinished());
        assertEquals(1f, player.timeSeconds());

        assertEquals(RenderAnimationPlayer.TickResult.FINISHED, player.tick(0f));
        assertTrue(player.isFinished());
        assertEquals(2, calls.get());
        assertEquals(RenderAnimationPlayer.TickResult.FINISHED, player.tick(1f));
        assertEquals(2, calls.get());
    }

    @Test
    void loopWrapsWithoutFinishing() {
        AtomicReference<Float> x = new AtomicReference<>();
        RenderAnimationPlayer player = new RenderAnimationPlayer(clip(RenderAnimationMode.LOOP), samples -> {
            x.set(samples.get("arm").pose().translation().x());
            return true;
        });

        assertEquals(RenderAnimationPlayer.TickResult.APPLIED, player.tick(1.25f));
        assertEquals(0.25f, player.timeSeconds(), 0.0001f);
        assertEquals(2.5f, x.get(), 0.0001f);
        assertFalse(player.isFinished());
    }

    @Test
    void holdAppliesFinalPoseOnceAndKeepsIt() {
        AtomicInteger calls = new AtomicInteger();
        RenderAnimationPlayer player = new RenderAnimationPlayer(clip(RenderAnimationMode.HOLD), samples -> {
            calls.incrementAndGet();
            return true;
        });

        assertEquals(RenderAnimationPlayer.TickResult.HOLDING, player.tick(1f));
        assertTrue(player.isHolding());
        assertEquals(RenderAnimationPlayer.TickResult.HOLDING, player.tick(1f));
        assertEquals(1, calls.get());
    }

    @Test
    void closeSerializesAgainstLaterTicksAndDoesNotOwnTarget() {
        AtomicInteger calls = new AtomicInteger();
        RenderAnimationPlayer player = new RenderAnimationPlayer(clip(RenderAnimationMode.LOOP), samples -> {
            calls.incrementAndGet();
            return true;
        });
        player.close();
        player.close();

        assertTrue(player.isClosed());
        assertEquals(RenderAnimationPlayer.TickResult.CLOSED, player.tick(0.1f));
        assertEquals(0, calls.get());
        assertFalse(player.reset());
    }

    @Test
    void sampledStatePreservesGlobalRotationAndTrackOrder() {
        RenderAnimationTrack arm = new RenderAnimationTrack("arm", List.of(
                new RenderAnimationFrame(0f, pose(0f), RenderInterpolation.LINEAR, true),
                new RenderAnimationFrame(1f, pose(10f), RenderInterpolation.LINEAR, false)
        ));
        RenderAnimationTrack hand = new RenderAnimationTrack("hand", List.of(
                new RenderAnimationFrame(0f, pose(1f), RenderInterpolation.LINEAR, false)
        ));
        LinkedHashMap<String, RenderAnimationTrack> tracks = new LinkedHashMap<>();
        tracks.put("arm", arm);
        tracks.put("hand", hand);
        RenderAnimationClip clip = new RenderAnimationClip("test", 1f, RenderAnimationMode.ONCE, false, tracks);

        Map<String, RenderAnimationSample> sample = RenderAnimationSampler.sampleState(clip, 0.5f, RenderAnimationMode.ONCE);
        assertEquals(List.of("arm", "hand"), sample.keySet().stream().toList());
        assertTrue(sample.get("arm").globalRotation());
        assertEquals(5f, sample.get("arm").pose().translation().x(), 0.0001f);
        assertThrows(UnsupportedOperationException.class, () -> sample.clear());
    }

    private static RenderAnimationClip clip(RenderAnimationMode mode) {
        RenderAnimationTrack track = new RenderAnimationTrack("arm", List.of(
                new RenderAnimationFrame(0f, pose(0f), RenderInterpolation.LINEAR),
                new RenderAnimationFrame(1f, pose(10f), RenderInterpolation.LINEAR)
        ));
        return new RenderAnimationClip("walk", 1f, mode, false, Map.of("arm", track));
    }

    private static RenderBonePose pose(float x) {
        return new RenderBonePose(new RenderVec3(x, 0, 0), RenderVec3.ONE, RenderVec3.ZERO);
    }
}
