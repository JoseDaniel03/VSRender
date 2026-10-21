package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationClip;
import net.momirealms.craftengine.core.render.animation.RenderAnimationFrame;
import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.animation.RenderAnimationTrack;
import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderInterpolation;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IndexedAnimationLayerStackTest {

    @Test
    void overrideClaimsTrackedSubtreeAndEqualPriorityUsesNewestInsertion() {
        CompiledRenderAsset asset = asset();
        IndexedAnimationLayerStack stack = new IndexedAnimationLayerStack(asset.rig());
        RenderAnimationBuffer out = asset.createAnimationBuffer();
        int root = asset.rig().indexOf("root");
        int arm = asset.rig().indexOf("arm");
        int hand = asset.rig().indexOf("hand");
        int leg = asset.rig().indexOf("leg");

        stack.put("base", asset.animation("base").orElseThrow(), RenderAnimationMode.LOOP, 0);
        stack.tick(.04f, out);
        assertEquals(.4f, out.translationX(root), .0001f);
        assertEquals(.8f, out.translationX(arm), .0001f);
        assertEquals(1.2f, out.translationX(hand), .0001f);
        assertEquals(1.6f, out.translationX(leg), .0001f);

        stack.put("event", asset.animation("event").orElseThrow(), RenderAnimationMode.ONCE, 0);
        stack.tick(.02f, out);
        assertEquals(.6f, out.translationX(root), .0001f);
        assertEquals(2.4f, out.translationX(leg), .0001f);
        assertEquals(2f, out.translationX(arm), .0001f);
        assertEquals(0f, out.translationX(hand), .0001f,
                "descendant without a track must be identity-masked by override subtree");

        stack.put("high", asset.animation("high").orElseThrow(), RenderAnimationMode.ONCE, 0);
        stack.tick(.02f, out);
        assertEquals(4f, out.translationX(arm), .0001f);
        assertEquals(4f, out.translationX(hand), .0001f,
                "newer equal-priority layer must win on the intersecting part");
    }

    @Test
    void preemptionStateIsPerPartNotGlobal() {
        CompiledRenderAsset asset = asset();
        IndexedAnimationLayerStack stack = new IndexedAnimationLayerStack(asset.rig());
        RenderAnimationBuffer out = asset.createAnimationBuffer();
        int root = asset.rig().indexOf("root");
        int arm = asset.rig().indexOf("arm");
        int hand = asset.rig().indexOf("hand");

        stack.put("base", asset.animation("base").orElseThrow(), RenderAnimationMode.LOOP, 0);
        stack.tick(.04f, out);
        stack.put("event", asset.animation("event").orElseThrow(), RenderAnimationMode.ONCE, 0);
        stack.tick(.02f, out);
        stack.put("high", asset.animation("high").orElseThrow(), RenderAnimationMode.ONCE, 0);
        stack.tick(.02f, out);
        assertTrue(stack.remove("high"));

        // The ONCE event branch on hand was cleared by preemption and must not resume.
        stack.tick(.02f, out);
        assertEquals(.6f, out.translationX(hand), .0001f);

        stack.tick(.06f, out);
        assertTrue(stack.completed("event"));
        assertTrue(stack.remove("event"));
        stack.tick(.01f, out);

        // Arm's LOOP base was preempted for the whole event and restarts; hand's base already resumed
        // when the higher hand-only layer killed that ONCE branch. Root was never preempted.
        assertEquals(.2f, out.translationX(arm), .0001f);
        assertEquals(2.7f, out.translationX(hand), .0001f);
        assertNotEquals(out.translationX(root), out.translationX(arm), .05f);
    }

    private static RenderAnimationTrack track(String id, float endX) {
        return new RenderAnimationTrack(id, List.of(
                new RenderAnimationFrame(0f, RenderBonePose.IDENTITY, RenderInterpolation.LINEAR),
                new RenderAnimationFrame(.1f,
                        new RenderBonePose(new RenderVec3(endX, 0, 0), RenderVec3.ONE, RenderVec3.ZERO),
                        RenderInterpolation.LINEAR)
        ));
    }

    private static CompiledRenderAsset asset() {
        RenderPart root = new RenderPart("root", null, List.of("arm", "leg"), RenderPartKind.GROUP, RenderBonePose.IDENTITY);
        RenderPart arm = new RenderPart("arm", "root", List.of("hand"), RenderPartKind.GROUP, RenderBonePose.IDENTITY);
        RenderPart hand = new RenderPart("hand", "arm", List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY);
        RenderPart leg = new RenderPart("leg", "root", List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY);
        RenderRig rig = new RenderRig("native", "rig", RenderRigType.MODEL,
                List.of(root, arm, hand, leg), Set.of("base", "event", "high"));

        Map<String, RenderAnimationTrack> base = new LinkedHashMap<>();
        base.put("root", track("root", 1));
        base.put("arm", track("arm", 2));
        base.put("hand", track("hand", 3));
        base.put("leg", track("leg", 4));
        return CompiledRenderAsset.compile("asset", rig, List.of(
                new RenderAnimationClip("base", .1f, RenderAnimationMode.LOOP, false, base),
                new RenderAnimationClip("event", .1f, RenderAnimationMode.ONCE, true,
                        Map.of("arm", track("arm", 10))),
                new RenderAnimationClip("high", .1f, RenderAnimationMode.ONCE, true,
                        Map.of("hand", track("hand", 20)))
        ));
    }
}
