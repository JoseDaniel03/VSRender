package net.momirealms.craftengine.core.render.animation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RenderAnimationSamplerTest {

    @Test
    void linearlySamplesBetweenFrames() {
        RenderAnimationTrack track = new RenderAnimationTrack("arm", List.of(
                frame(0f, 0f, RenderInterpolation.LINEAR),
                frame(1f, 10f, RenderInterpolation.LINEAR)
        ));
        RenderBonePose pose = RenderAnimationSampler.sample(track, 0.25f);
        assertEquals(2.5f, pose.translation().x(), 0.0001f);
        assertEquals(1f, pose.scale().x(), 0.0001f);
    }

    @Test
    void stepHoldsPreviousFrame() {
        RenderAnimationTrack track = new RenderAnimationTrack("arm", List.of(
                frame(0f, 2f, RenderInterpolation.STEP),
                frame(1f, 9f, RenderInterpolation.LINEAR)
        ));
        assertEquals(2f, RenderAnimationSampler.sample(track, 0.75f).translation().x(), 0.0001f);
    }

    @Test
    void clampsFinitePlaybackAndWrapsLoopPlayback() {
        RenderAnimationTrack track = new RenderAnimationTrack("arm", List.of(
                frame(0f, 0f, RenderInterpolation.LINEAR),
                frame(2f, 20f, RenderInterpolation.LINEAR)
        ));
        RenderAnimationClip clip = clip(2f, track);

        assertEquals(20f, RenderAnimationSampler.sample(clip, 5f, RenderAnimationMode.ONCE).get("arm").translation().x(), 0.0001f);
        assertEquals(5f, RenderAnimationSampler.sample(clip, 2.5f, RenderAnimationMode.LOOP).get("arm").translation().x(), 0.0001f);
    }

    @Test
    void rejectsUnsortedFramesAndTrackPastClipLength() {
        assertThrows(IllegalArgumentException.class, () -> new RenderAnimationTrack("arm", List.of(
                frame(1f, 1f, RenderInterpolation.LINEAR),
                frame(0f, 0f, RenderInterpolation.LINEAR)
        )));
        RenderAnimationTrack track = new RenderAnimationTrack("arm", List.of(frame(3f, 1f, RenderInterpolation.LINEAR)));
        assertThrows(IllegalArgumentException.class, () -> clip(2f, track));
    }

    @Test
    void snapshotsTrackCollections() {
        var frames = new java.util.ArrayList<RenderAnimationFrame>();
        frames.add(frame(0f, 0f, RenderInterpolation.LINEAR));
        RenderAnimationTrack track = new RenderAnimationTrack("arm", frames);
        frames.clear();
        assertEquals(1, track.frames().size());
        assertThrows(UnsupportedOperationException.class, () -> track.frames().clear());
    }

    private static RenderAnimationClip clip(float length, RenderAnimationTrack track) {
        Map<String, RenderAnimationTrack> tracks = new LinkedHashMap<>();
        tracks.put(track.partId(), track);
        return new RenderAnimationClip("walk", length, RenderAnimationMode.ONCE, false, tracks);
    }

    private static RenderAnimationFrame frame(float time, float x, RenderInterpolation interpolation) {
        return new RenderAnimationFrame(
                time,
                new RenderBonePose(new RenderVec3(x, 0, 0), RenderVec3.ONE, RenderVec3.ZERO),
                interpolation
        );
    }
}

