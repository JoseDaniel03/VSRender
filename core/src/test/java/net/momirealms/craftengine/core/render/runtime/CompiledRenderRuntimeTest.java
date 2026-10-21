package net.momirealms.craftengine.core.render.runtime;

import net.momirealms.craftengine.core.render.animation.*;
import net.momirealms.craftengine.core.render.compiled.CompiledRenderAsset;
import net.momirealms.craftengine.core.render.rig.*;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CompiledRenderRuntimeTest {
    @Test
    void firstVisibleFramePublishesAllAtCaughtUpPose() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(asset(3), new RenderLodPolicy(10, 20, 30, 40));
        assertTrue(runtime.play("move"));
        Capture sink = new Capture();
        assertFalse(runtime.tick(.25f, false, 0f, sink));
        assertTrue(runtime.tick(.25f, true, 5f, sink));
        assertEquals(1, sink.frames);
        assertEquals(3, sink.lastSize);
        assertEquals(5f, runtime.poses().translationX(0), .0001f);
    }

    @Test
    void lodAccumulatesTimeAndDirtyOnlyPublishes() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(asset(2), new RenderLodPolicy(10, 20, 30, 40));
        runtime.play("move", RenderAnimationMode.LOOP);
        Capture sink = new Capture();
        runtime.tick(0f, true, 25f, sink); // initial all, QUARTER
        assertEquals(2, sink.lastSize);
        assertFalse(runtime.tick(.05f, true, 25f, sink));
        assertFalse(runtime.tick(.05f, true, 25f, sink));
        assertTrue(runtime.tick(.05f, true, 25f, sink)); // visible tick 4
        assertEquals(.15f, runtime.state().timeSeconds(), .0001f);
        assertEquals(2, sink.lastSize); // root movement changes descendant world pose too
        int frames = sink.frames;
        assertFalse(runtime.tick(0f, true, 25f, sink));
        assertEquals(frames, sink.frames);
    }

    @Test
    void stopPublishesResolvedRestPoseBeforeNextSamplingFrame() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(asset(1), new RenderLodPolicy(10, 20, 30, 40));
        Capture sink = new Capture();
        assertTrue(runtime.play("move"));
        assertTrue(runtime.tick(.5f, true, 5f, sink));
        assertEquals(5f, runtime.poses().translationX(0), .0001f);
        int frames = sink.frames;

        assertTrue(runtime.stop());
        assertEquals(0f, runtime.poses().translationX(0), .0001f);
        assertTrue(runtime.tick(.05f, true, 5f, sink));
        assertEquals(frames + 1, sink.frames);
        assertEquals(1, sink.lastSize);
        assertEquals(0f, runtime.poses().translationX(0), .0001f);
    }


    @Test
    void newPlaybackSupersedesPendingRestFrameFromStop() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(asset(1), new RenderLodPolicy(10, 20, 30, 40));
        Capture sink = new Capture();
        assertTrue(runtime.play("move"));
        assertTrue(runtime.tick(.5f, true, 5f, sink));
        assertTrue(runtime.stop());

        // Start a new playback before the rest frame queued by stop() is published.
        assertTrue(runtime.play("move"));
        runtime.tick(.1f, true, 5f, sink);
        assertEquals(.1f, runtime.state().timeSeconds(), .0001f);
        assertEquals(1f, runtime.poses().translationX(0), .0001f);
    }

    @Test
    void cullAndRestoreCatchUpImmediately() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(asset(1), new RenderLodPolicy(10, 20, 30, 40));
        runtime.play("move");
        Capture sink = new Capture();
        runtime.tick(0f, true, 5f, sink);
        runtime.tick(.4f, false, 0f, sink);
        int frames = sink.frames;
        assertTrue(runtime.tick(.1f, true, 5f, sink));
        assertEquals(frames + 1, sink.frames);
        assertEquals(1, sink.lastSize);
        assertEquals(5f, runtime.poses().translationX(0), .0001f);
    }

    private static CompiledRenderAsset asset(int parts) {
        java.util.ArrayList<RenderPart> rigParts = new java.util.ArrayList<>();
        for (int i = 0; i < parts; i++) {
            String id = i == 0 ? "root" : "p" + i;
            String parent = i == 0 ? null : "root";
            rigParts.add(new RenderPart(id, parent, List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY));
        }
        // root children must be declared consistently
        if (parts > 1) {
            java.util.ArrayList<String> children = new java.util.ArrayList<>();
            for (int i = 1; i < parts; i++) children.add("p" + i);
            rigParts.set(0, new RenderPart("root", null, children, RenderPartKind.GROUP, RenderBonePose.IDENTITY));
        }
        RenderRig rig = new RenderRig("native", "demo", RenderRigType.MODEL, rigParts, Set.of("move"));
        RenderAnimationTrack track = new RenderAnimationTrack("root", List.of(
                new RenderAnimationFrame(0f, pose(0f), RenderInterpolation.LINEAR),
                new RenderAnimationFrame(1f, pose(10f), RenderInterpolation.LINEAR)
        ));
        LinkedHashMap<String, RenderAnimationTrack> tracks = new LinkedHashMap<>();
        tracks.put("root", track);
        return CompiledRenderAsset.compile("demo", rig, List.of(new RenderAnimationClip("move", 1f, RenderAnimationMode.ONCE, false, tracks)));
    }

    private static RenderBonePose pose(float x) {
        return new RenderBonePose(new RenderVec3(x, 0, 0), RenderVec3.ONE, RenderVec3.ZERO);
    }

    private static final class Capture implements RenderFrameSink {
        int frames;
        int lastSize;
        @Override public void publish(RenderFrameBatch batch) {
            frames++;
            lastSize = batch.size();
        }
    }
}
