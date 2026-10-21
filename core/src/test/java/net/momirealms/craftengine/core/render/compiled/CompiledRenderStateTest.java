package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.*;
import net.momirealms.craftengine.core.render.rig.*;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CompiledRenderStateTest {

    @Test
    void playTickStopUsesSharedAssetAndReusableBuffers() {
        RenderRig rig = new RenderRig("native", "demo", RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY)), Set.of("move"));
        RenderAnimationTrack track = new RenderAnimationTrack("root", List.of(
                new RenderAnimationFrame(0f, pose(0f), RenderInterpolation.LINEAR),
                new RenderAnimationFrame(1f, pose(10f), RenderInterpolation.LINEAR)
        ));
        LinkedHashMap<String, RenderAnimationTrack> tracks = new LinkedHashMap<>();
        tracks.put("root", track);
        RenderAnimationClip clip = new RenderAnimationClip("move", 1f, RenderAnimationMode.ONCE, false, tracks);
        CompiledRenderAsset asset = CompiledRenderAsset.compile("demo", rig, List.of(clip));
        CompiledRenderState state = new CompiledRenderState(asset);
        int root = asset.rig().indexOf("root");

        assertEquals(1, state.poses().dirtyCount());
        assertFalse(state.play("missing"));
        assertTrue(state.play("move"));
        RenderPoseBuffer sameBuffer = state.poses();
        state.tick(.5f);
        assertSame(sameBuffer, state.poses());
        assertEquals(5f, state.poses().translationX(root), 0.0001f);
        assertFalse(state.completed());
        state.tick(.5f);
        assertTrue(state.completed());
        assertEquals(10f, state.poses().translationX(root), 0.0001f);
        assertTrue(state.stop());
        assertEquals(0f, state.poses().translationX(root), 0.0001f);
        assertTrue(state.poses().dirty(root));
        assertFalse(state.stop());
    }

    @Test
    void loopWrapsWithoutStringLookupPerTick() {
        CompiledRenderAsset asset = asset();
        CompiledRenderState state = new CompiledRenderState(asset);
        assertTrue(state.play("move", RenderAnimationMode.LOOP));
        state.tick(1.25f);
        assertEquals(.25f, state.timeSeconds(), 0.0001f);
        assertEquals(2.5f, state.poses().translationX(0), 0.0001f);
    }

    private static CompiledRenderAsset asset() {
        RenderRig rig = new RenderRig("native", "demo", RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY)), Set.of("move"));
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
}
