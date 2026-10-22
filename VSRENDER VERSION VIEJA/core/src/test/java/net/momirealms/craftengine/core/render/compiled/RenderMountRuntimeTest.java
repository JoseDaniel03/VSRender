package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.rig.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RenderMountRuntimeTest {
    @Test
    void mountIndicesAndAnchorsUseResolvedPose() {
        RenderPart root = new RenderPart(
                "root", null, List.of("seat"), RenderPartKind.GROUP,
                RenderBonePose.IDENTITY, RenderPartRotationPolicy.NONE, null,
                RenderVec3.ZERO, RenderNameTagMode.NONE, RenderMountPolicy.DISABLED);
        RenderPart seat = new RenderPart(
                "seat", "root", List.of(), RenderPartKind.GROUP,
                new RenderBonePose(new RenderVec3(2, 3, 4), new RenderVec3(2, 2, 2), new RenderVec3(0, 90, 0)),
                RenderPartRotationPolicy.NONE, null,
                new RenderVec3(1, 0, 0), RenderNameTagMode.NONE, RenderMountPolicy.WALK);
        RenderRig rig = new RenderRig("native", "mount", RenderRigType.MODEL, List.of(root, seat), Set.of());
        CompiledRenderAsset asset = CompiledRenderAsset.compile("mount", rig, List.of());
        CompiledRenderState state = new CompiledRenderState(asset);

        int seatIndex = asset.rig().indexOf("seat");
        assertArrayEquals(new int[]{seatIndex}, asset.rig().mountIndices());
        assertEquals(RenderMountPolicy.WALK, state.mounts().policy(seatIndex));
        // local +X rotated +90° around Y becomes -Z, then scaled by 2 and added to translation.
        assertEquals(2f, state.mounts().x(seatIndex), 1e-4f);
        assertEquals(3f, state.mounts().y(seatIndex), 1e-4f);
        assertEquals(2f, state.mounts().z(seatIndex), 1e-4f);
    }

    @Test
    void builtInWalkAndFlyControlMatchesImportedSemantics() {
        RenderMountInput forward = new RenderMountInput(0f, 0f, 1f, false);
        RenderMountMotion walk = RenderMountController.resolve(RenderMountPolicy.WALK, forward, 0.4f, 0f, false);
        assertEquals(0f, walk.x(), 1e-5f);
        assertEquals(0f, walk.y(), 1e-5f);
        assertEquals(0.4f, walk.z(), 1e-5f);

        RenderMountInput reverseStrafe = new RenderMountInput(1f, 0f, -1f, false);
        RenderMountMotion normalized = RenderMountController.resolve(RenderMountPolicy.WALK, reverseStrafe, 1f, 0f, false);
        float invSqrt2 = (float) (1d / Math.sqrt(2d));
        assertEquals(invSqrt2 * .5f, normalized.x(), 1e-5f);
        assertEquals(-invSqrt2 * .25f, normalized.z(), 1e-5f);

        RenderMountInput flyUp = new RenderMountInput(0f, 1f, 0f, true);
        RenderMountMotion fly = RenderMountController.resolve(RenderMountPolicy.FLY, flyUp, 0.2f, 0f, true);
        assertEquals(0.3f, fly.y(), 1e-5f);

        // BetterModel still uses WALK's controller math while MoveType.FLY adds the 1.5 multiplier.
        RenderMountMotion noGravityWalk = RenderMountController.resolve(RenderMountPolicy.WALK,
                new RenderMountInput(0f, 1f, 1f, true), 0.2f, 0f, true);
        assertEquals(0f, noGravityWalk.y(), 1e-5f);
        assertEquals((float) (0.3d / Math.sqrt(2d)), noGravityWalk.z(), 1e-5f);
        assertTrue(RenderMountController.shouldJump(RenderMountPolicy.FLY, flyUp, true, 0f, 0));
        assertTrue(RenderMountController.shouldJump(RenderMountPolicy.WALK,
                new RenderMountInput(0f, 0f, 0f, true), false, 0.005f, 0));
        assertTrue(RenderMountController.shouldJump(RenderMountPolicy.WALK,
                RenderMountInput.ZERO, true, 0.005f, 0));
        assertFalse(RenderMountController.shouldJump(RenderMountPolicy.PASSENGER_ONLY,
                new RenderMountInput(0f, 0f, 0f, true), true, 0f, 0));
    }
}
