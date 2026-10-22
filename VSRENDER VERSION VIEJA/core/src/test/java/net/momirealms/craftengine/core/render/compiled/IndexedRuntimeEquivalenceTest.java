package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.*;
import net.momirealms.craftengine.core.render.rig.*;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IndexedRuntimeEquivalenceTest {

    @Test
    void indexedRuntimeMatchesDescriptiveResolver() {
        RenderPart root = part("root", null, List.of("arm"), pose(vec(1, 0, 0), vec(2, 2, 2), vec(0, 0, 30)));
        RenderPart arm = part("arm", "root", List.of(), pose(vec(1, 0, 0), RenderVec3.ONE, vec(0, 10, 0)));
        RenderRig rig = new RenderRig("native", "demo", RenderRigType.MODEL, List.of(root, arm), Set.of("wave"));
        RenderAnimationTrack armTrack = new RenderAnimationTrack("arm", List.of(
                frame(0f, pose(RenderVec3.ZERO, RenderVec3.ONE, RenderVec3.ZERO), false),
                frame(1f, pose(vec(0, 1, 0), vec(1.2f, .8f, 1), vec(20, 0, 40)), false)
        ));
        RenderAnimationClip clip = clip("wave", armTrack);

        Map<String, RenderAnimationSample> descriptiveSamples = RenderAnimationSampler.sampleState(clip, .5f, RenderAnimationMode.ONCE);
        Map<String, RenderResolvedPose> descriptive = RenderRigPoseResolver.resolve(rig, descriptiveSamples);

        CompiledRenderAsset asset = CompiledRenderAsset.compile("demo", rig, List.of(clip));
        RenderAnimationBuffer animation = asset.createAnimationBuffer();
        RenderPoseBuffer poses = asset.createPoseBuffer();
        IndexedAnimationSampler.sample(asset.animation("wave").orElseThrow(), .5f, RenderAnimationMode.ONCE, animation);
        IndexedRigPoseResolver.resolve(asset.rig(), animation, poses);

        for (String id : List.of("root", "arm")) {
            int index = asset.rig().indexOf(id);
            assertPose(descriptive.get(id), poses.pose(index));
        }
    }

    @Test
    void dirtyMaskOnlyMarksChangedIndependentBranch() {
        RenderPart left = part("left", null, List.of(), RenderBonePose.IDENTITY);
        RenderPart right = part("right", null, List.of(), RenderBonePose.IDENTITY);
        RenderRig rig = new RenderRig("native", "demo", RenderRigType.MODEL, List.of(left, right), Set.of());
        IndexedRenderRig indexed = IndexedRenderRig.compile(rig);
        RenderAnimationBuffer animation = new RenderAnimationBuffer(indexed);
        RenderPoseBuffer poses = new RenderPoseBuffer(indexed);

        IndexedRigPoseResolver.resolve(indexed, animation, poses);
        assertEquals(2, poses.dirtyCount());
        IndexedRigPoseResolver.resolve(indexed, animation, poses);
        assertEquals(0, poses.dirtyCount());

        animation.set(indexed.indexOf("right"), 2f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f, false);
        IndexedRigPoseResolver.resolve(indexed, animation, poses);
        assertEquals(1, poses.dirtyCount());
        assertFalse(poses.dirty(indexed.indexOf("left")));
        assertTrue(poses.dirty(indexed.indexOf("right")));
    }

    @Test
    void samplerClearsStaleTracksWhenBufferIsReused() {
        RenderRig rig = new RenderRig("native", "demo", RenderRigType.MODEL,
                List.of(part("left", null, List.of(), RenderBonePose.IDENTITY), part("right", null, List.of(), RenderBonePose.IDENTITY)), Set.of());
        RenderAnimationClip leftClip = clip("left_anim", new RenderAnimationTrack("left", List.of(frame(0f, pose(vec(5, 0, 0), RenderVec3.ONE, RenderVec3.ZERO), false))));
        RenderAnimationClip rightClip = clip("right_anim", new RenderAnimationTrack("right", List.of(frame(0f, pose(vec(7, 0, 0), RenderVec3.ONE, RenderVec3.ZERO), false))));
        CompiledRenderAsset asset = CompiledRenderAsset.compile("demo", rig, List.of(leftClip, rightClip));
        RenderAnimationBuffer buffer = asset.createAnimationBuffer();

        IndexedAnimationSampler.sample(asset.animation("left_anim").orElseThrow(), 0f, RenderAnimationMode.ONCE, buffer);
        assertTrue(buffer.active(asset.rig().indexOf("left")));
        IndexedAnimationSampler.sample(asset.animation("right_anim").orElseThrow(), 0f, RenderAnimationMode.ONCE, buffer);
        assertFalse(buffer.active(asset.rig().indexOf("left")));
        assertTrue(buffer.active(asset.rig().indexOf("right")));
    }

    @Test
    void globalRotationMatchesDescriptiveResolver() {
        RenderPart root = part("root", null, List.of("child"), pose(RenderVec3.ZERO, RenderVec3.ONE, vec(0, 0, 90)));
        RenderPart child = part("child", "root", List.of(), pose(vec(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO));
        RenderRig rig = new RenderRig("native", "demo", RenderRigType.MODEL, List.of(root, child), Set.of("global"));
        RenderAnimationTrack childTrack = new RenderAnimationTrack("child", List.of(frame(0f, RenderBonePose.IDENTITY, true)));
        RenderAnimationClip clip = clip("global", childTrack);

        Map<String, RenderResolvedPose> descriptive = RenderRigPoseResolver.resolve(rig,
                RenderAnimationSampler.sampleState(clip, 0f, RenderAnimationMode.ONCE));
        CompiledRenderAsset asset = CompiledRenderAsset.compile("demo", rig, List.of(clip));
        RenderAnimationBuffer animation = asset.createAnimationBuffer();
        RenderPoseBuffer poses = asset.createPoseBuffer();
        IndexedAnimationSampler.sample(asset.animation("global").orElseThrow(), 0f, RenderAnimationMode.ONCE, animation);
        IndexedRigPoseResolver.resolve(asset.rig(), animation, poses);

        assertPose(descriptive.get("child"), poses.pose(asset.rig().indexOf("child")));
    }

    private static RenderAnimationClip clip(String id, RenderAnimationTrack... tracks) {
        LinkedHashMap<String, RenderAnimationTrack> map = new LinkedHashMap<>();
        for (RenderAnimationTrack track : tracks) map.put(track.partId(), track);
        return new RenderAnimationClip(id, 1f, RenderAnimationMode.ONCE, false, map);
    }

    private static RenderAnimationFrame frame(float time, RenderBonePose pose, boolean globalRotation) {
        return new RenderAnimationFrame(time, pose, RenderInterpolation.LINEAR, globalRotation);
    }

    private static RenderPart part(String id, String parent, List<String> children, RenderBonePose rest) {
        return new RenderPart(id, parent, children, RenderPartKind.GROUP, rest);
    }

    private static RenderBonePose pose(RenderVec3 t, RenderVec3 s, RenderVec3 r) {
        return new RenderBonePose(t, s, r);
    }

    private static RenderVec3 vec(float x, float y, float z) {
        return new RenderVec3(x, y, z);
    }

    private static void assertPose(RenderResolvedPose expected, RenderResolvedPose actual) {
        assertEquals(expected.translation().x(), actual.translation().x(), 0.0002f);
        assertEquals(expected.translation().y(), actual.translation().y(), 0.0002f);
        assertEquals(expected.translation().z(), actual.translation().z(), 0.0002f);
        assertEquals(expected.scale().x(), actual.scale().x(), 0.0002f);
        assertEquals(expected.scale().y(), actual.scale().y(), 0.0002f);
        assertEquals(expected.scale().z(), actual.scale().z(), 0.0002f);
        float dot = Math.abs(expected.rotation().x() * actual.rotation().x()
                + expected.rotation().y() * actual.rotation().y()
                + expected.rotation().z() * actual.rotation().z()
                + expected.rotation().w() * actual.rotation().w());
        assertEquals(1f, dot, 0.0002f);
    }
}
