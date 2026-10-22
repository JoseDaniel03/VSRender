package net.momirealms.craftengine.core.render.animation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RenderAnimationPlaybackTest {

    @Test
    void onceCompletesAndClamps() {
        RenderAnimationPlayback playback = new RenderAnimationPlayback(clip(2f), RenderAnimationMode.ONCE);
        playback.advance(3f);
        assertEquals(2f, playback.timeSeconds(), 0.0001f);
        assertTrue(playback.completed());
        assertFalse(playback.holding());
        assertEquals(20f, playback.sample().get("arm").translation().x(), 0.0001f);
    }

    @Test
    void loopWrapsAndNeverCompletes() {
        RenderAnimationPlayback playback = new RenderAnimationPlayback(clip(2f), RenderAnimationMode.LOOP);
        playback.advance(2.5f);
        assertEquals(0.5f, playback.timeSeconds(), 0.0001f);
        assertFalse(playback.completed());
        assertFalse(playback.holding());
    }

    @Test
    void holdKeepsLastPoseWithoutCompleting() {
        RenderAnimationPlayback playback = new RenderAnimationPlayback(clip(2f), RenderAnimationMode.HOLD);
        playback.advance(5f);
        assertEquals(2f, playback.timeSeconds(), 0.0001f);
        assertTrue(playback.holding());
        assertFalse(playback.completed());
        playback.advance(5f);
        assertEquals(2f, playback.timeSeconds(), 0.0001f);
    }

    @Test
    void zeroLengthHasExplicitState() {
        RenderAnimationPlayback once = new RenderAnimationPlayback(clip(0f), RenderAnimationMode.ONCE);
        RenderAnimationPlayback hold = new RenderAnimationPlayback(clip(0f), RenderAnimationMode.HOLD);
        RenderAnimationPlayback loop = new RenderAnimationPlayback(clip(0f), RenderAnimationMode.LOOP);
        assertTrue(once.completed());
        assertTrue(hold.holding());
        assertFalse(loop.completed());
        assertDoesNotThrow(() -> loop.advance(1f));
        assertEquals(0f, loop.timeSeconds(), 0.0001f);
    }

    @Test
    void resetRestoresInitialStateAndInvalidDeltaIsRejected() {
        RenderAnimationPlayback playback = new RenderAnimationPlayback(clip(2f), RenderAnimationMode.ONCE);
        playback.advance(2f);
        playback.reset();
        assertEquals(0f, playback.timeSeconds(), 0.0001f);
        assertFalse(playback.completed());
        assertThrows(IllegalArgumentException.class, () -> playback.advance(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> playback.advance(Float.NaN));
    }

    private static RenderAnimationClip clip(float length) {
        var start = new RenderAnimationFrame(0f, new RenderBonePose(new RenderVec3(0, 0, 0), RenderVec3.ONE, RenderVec3.ZERO), RenderInterpolation.LINEAR);
        java.util.List<RenderAnimationFrame> frames = length == 0f
                ? java.util.List.of(start)
                : java.util.List.of(start, new RenderAnimationFrame(length, new RenderBonePose(new RenderVec3(20, 0, 0), RenderVec3.ONE, RenderVec3.ZERO), RenderInterpolation.LINEAR));
        RenderAnimationTrack track = new RenderAnimationTrack("arm", frames);
        return new RenderAnimationClip("clip", length, RenderAnimationMode.ONCE, false, Map.of("arm", track));
    }
}
