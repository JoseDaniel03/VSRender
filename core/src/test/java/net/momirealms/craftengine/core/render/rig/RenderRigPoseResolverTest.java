package net.momirealms.craftengine.core.render.rig;

import net.momirealms.craftengine.core.render.animation.RenderAnimationSample;
import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RenderRigPoseResolverTest {

    @Test
    void resolvesRestPoseAndParentRotation() {
        RenderPart root = part("root", null, List.of("child"), pose(vec(1, 0, 0), RenderVec3.ONE, vec(0, 0, 90)));
        RenderPart child = part("child", "root", List.of(), pose(vec(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO));
        RenderRig rig = rig(root, child);

        Map<String, RenderResolvedPose> resolved = RenderRigPoseResolver.resolve(rig, Map.of());
        assertVec(resolved.get("root").translation(), 1, 0, 0);
        assertVec(resolved.get("child").translation(), 1, 1, 0);
        assertVec(resolved.get("child").rotation().rotate(vec(1, 0, 0)), 0, 1, 0);
    }

    @Test
    void parentScaleMatchesBetterModelOrderRotateThenScale() {
        RenderPart root = part("root", null, List.of("child"), pose(RenderVec3.ZERO, vec(2, 3, 1), vec(0, 0, 90)));
        RenderPart child = part("child", "root", List.of(), pose(vec(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO));
        RenderResolvedPose childPose = RenderRigPoseResolver.resolve(rig(root, child), Map.of()).get("child");

        assertVec(childPose.translation(), 0, 3, 0);
        assertVec(childPose.scale(), 2, 3, 1);
    }

    @Test
    void globalRotationDoesNotInheritParentButPositionStillDoes() {
        RenderPart root = part("root", null, List.of("child"), pose(RenderVec3.ZERO, RenderVec3.ONE, vec(0, 0, 90)));
        RenderPart child = part("child", "root", List.of(), pose(vec(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO));
        Map<String, RenderAnimationSample> samples = Map.of(
                "child", new RenderAnimationSample(RenderBonePose.IDENTITY, true)
        );
        RenderResolvedPose childPose = RenderRigPoseResolver.resolve(rig(root, child), samples).get("child");

        assertVec(childPose.translation(), 0, 1, 0);
        assertVec(childPose.rotation().rotate(vec(1, 0, 0)), 1, 0, 0);
    }

    @Test
    void animationPoseComposesWithRestPoseAndMissingTracksUseIdentity() {
        RenderPart root = part("root", null, List.of("child"), pose(vec(1, 0, 0), vec(2, 2, 2), RenderVec3.ZERO));
        RenderPart child = part("child", "root", List.of(), pose(vec(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO));
        Map<String, RenderAnimationSample> samples = Map.of(
                "root", new RenderAnimationSample(pose(vec(2, 0, 0), vec(.5f, 1, 1), RenderVec3.ZERO), false)
        );
        Map<String, RenderResolvedPose> resolved = RenderRigPoseResolver.resolve(rig(root, child), samples);

        assertVec(resolved.get("root").translation(), 3, 0, 0);
        assertVec(resolved.get("root").scale(), 1, 2, 2);
        assertVec(resolved.get("child").translation(), 4, 0, 0);
        assertVec(resolved.get("child").scale(), 1, 2, 2);
    }

    @Test
    void legacyPartConstructorKeepsIdentityRestPose() {
        RenderPart part = new RenderPart("root", null, List.of());
        assertEquals(RenderBonePose.IDENTITY, part.restPose());
    }

    private static RenderPart part(String id, String parent, List<String> children, RenderBonePose rest) {
        return new RenderPart(id, parent, children, RenderPartKind.GROUP, rest);
    }

    private static RenderRig rig(RenderPart... parts) {
        return new RenderRig("native", "demo", RenderRigType.MODEL, List.of(parts), Set.of());
    }

    private static RenderBonePose pose(RenderVec3 t, RenderVec3 s, RenderVec3 r) {
        return new RenderBonePose(t, s, r);
    }

    private static RenderVec3 vec(float x, float y, float z) {
        return new RenderVec3(x, y, z);
    }

    private static void assertVec(RenderVec3 actual, float x, float y, float z) {
        assertEquals(x, actual.x(), 0.0001f);
        assertEquals(y, actual.y(), 0.0001f);
        assertEquals(z, actual.z(), 0.0001f);
    }
}
