package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.*;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;
import net.momirealms.craftengine.core.render.runtime.CompiledRenderRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CompiledRenderAnimationControllerTest {

    @Test
    void onceFinishesExactlyOnceAndCloseRestoresPose() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(asset());
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);
        AtomicInteger finishes = new AtomicInteger();
        RenderAnimationSession session = controller.open(
                new RenderAnimationKey("rig", "once"), RenderAnimationMode.ONCE).orElseThrow();

        assertTrue(session.start(finishes::incrementAndGet));
        assertFalse(session.start(finishes::incrementAndGet));
        runtime.tick(.05f, true, 0f, batch -> {});
        controller.afterTick();
        assertEquals(0, finishes.get());
        runtime.tick(.05f, true, 0f, batch -> {});
        controller.afterTick();
        controller.afterTick();
        assertEquals(1, finishes.get());

        session.close();
        assertFalse(runtime.state().playing());
        assertEquals(0f, runtime.poses().translationX(0), .0001f);
    }

    @Test
    void loopAndHoldDoNotAutoFinish() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(asset());
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);
        AtomicInteger finishes = new AtomicInteger();

        RenderAnimationSession loop = controller.open(
                new RenderAnimationKey("rig", "loop"), RenderAnimationMode.LOOP).orElseThrow();
        assertTrue(loop.start(finishes::incrementAndGet));
        runtime.tick(.3f, true, 0f, batch -> {});
        controller.afterTick();
        assertEquals(0, finishes.get());
        loop.close();

        RenderAnimationSession hold = controller.open(
                new RenderAnimationKey("rig", "hold"), RenderAnimationMode.HOLD).orElseThrow();
        assertTrue(hold.start(finishes::incrementAndGet));
        runtime.tick(.3f, true, 0f, batch -> {});
        controller.afterTick();
        assertEquals(0, finishes.get());
        controller.close();
        assertFalse(runtime.state().playing());
    }


    @Test
    void fullRigDamageInterruptRestartsLoopAndTerminatesOnce() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(assetWithDamage(false));
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);
        AtomicInteger finishes = new AtomicInteger();

        RenderAnimationSession loop = controller.open(
                new RenderAnimationKey("rig", "loop"), RenderAnimationMode.LOOP).orElseThrow();
        assertTrue(loop.start(finishes::incrementAndGet));
        runtime.tick(.07f, true, 0f, batch -> {});
        controller.afterTick();
        assertTrue(controller.triggerFullRigEventOnce("damage"));
        runtime.tick(.1f, true, 0f, batch -> {});
        controller.afterTick();
        assertEquals("loop", runtime.state().animationId());
        assertEquals(0f, runtime.state().timeSeconds(), .0001f);
        assertEquals(0, finishes.get());
        loop.close();

        RenderAnimationSession once = controller.open(
                new RenderAnimationKey("rig", "once"), RenderAnimationMode.ONCE).orElseThrow();
        assertTrue(once.start(finishes::incrementAndGet));
        assertTrue(controller.triggerFullRigEventOnce("damage"));
        runtime.tick(.1f, true, 0f, batch -> {});
        controller.afterTick();
        assertEquals(1, finishes.get());
        assertFalse(runtime.state().playing());
    }


    @Test
    void transientJumpReplacesDamageAndRestoresSuspendedLoop() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(assetWithEvents(false, false));
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);

        RenderAnimationSession loop = controller.open(
                new RenderAnimationKey("rig", "loop"), RenderAnimationMode.LOOP).orElseThrow();
        assertTrue(loop.start(() -> {}));
        runtime.tick(.04f, true, 0f, batch -> {});
        controller.afterTick();

        assertTrue(controller.triggerFullRigEventOnce("damage"));
        runtime.tick(.04f, true, 0f, batch -> {});
        controller.afterTick();
        assertTrue(controller.triggerFullRigEventOnce("jump"));
        assertEquals("jump", runtime.state().animationId());
        assertEquals(0f, runtime.state().timeSeconds(), .0001f);

        runtime.tick(.1f, true, 0f, batch -> {});
        controller.afterTick();
        assertEquals("loop", runtime.state().animationId());
        assertEquals(0f, runtime.state().timeSeconds(), .0001f);
    }

    @Test
    void closingSuspendedSessionLetsTransientFinishThenRest() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(assetWithEvents(false, false));
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);

        RenderAnimationSession loop = controller.open(
                new RenderAnimationKey("rig", "loop"), RenderAnimationMode.LOOP).orElseThrow();
        assertTrue(loop.start(() -> {}));
        assertTrue(controller.triggerFullRigEventOnce("damage"));
        loop.close();
        assertEquals("damage", runtime.state().animationId());

        runtime.tick(.1f, true, 0f, batch -> {});
        controller.afterTick();
        assertFalse(runtime.state().playing());
        assertEquals(0f, runtime.poses().translationX(0), .0001f);
    }

    @Test
    void overrideDamageInterruptUsesIndexedPerBoneRouting() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(assetWithDamage(true));
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);
        assertTrue(controller.triggerFullRigEventOnce("damage"));
        runtime.tick(.1f, true, 0f, batch -> {});
        controller.afterTick();
        assertFalse(runtime.state().playing());
    }

    @Test
    void terminalDeathOwnsPlaybackUntilControllerClose() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(assetWithDeath(false));
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);
        AtomicInteger finishes = new AtomicInteger();

        RenderAnimationSession loop = controller.open(
                new RenderAnimationKey("rig", "loop"), RenderAnimationMode.LOOP).orElseThrow();
        assertTrue(loop.start(() -> {}));
        assertTrue(controller.triggerTerminalFullRigEventOnce("death", finishes::incrementAndGet));
        assertTrue(controller.terminalEventActive());
        assertTrue(controller.open(new RenderAnimationKey("rig", "once"), RenderAnimationMode.ONCE).isEmpty());
        assertFalse(controller.triggerFullRigEventOnce("damage"));

        // Same terminal request is idempotent and must not restart the death clock/callback.
        assertTrue(controller.triggerTerminalFullRigEventOnce("death", () -> fail("retrigger replaced callback")));
        runtime.tick(.1f, false, Float.POSITIVE_INFINITY, true, batch -> fail("culled terminal published"));
        controller.afterTick();
        controller.afterTick();
        assertEquals(1, finishes.get());
        assertTrue(controller.terminalEventActive());
        assertTrue(runtime.state().completed());

        controller.close();
        assertFalse(runtime.state().playing());
    }

    @Test
    void overrideDeathTerminalUsesIndexedPerBoneRouting() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(assetWithDeath(true));
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);
        AtomicInteger finishes = new AtomicInteger();
        assertTrue(controller.triggerTerminalFullRigEventOnce("death", finishes::incrementAndGet));
        runtime.tick(.1f, false, Float.POSITIVE_INFINITY, true, batch -> fail("culled terminal published"));
        controller.afterTick();
        assertEquals(1, finishes.get());
    }

    @Test
    void rejectsWrongRigAndClosedController() {
        CompiledRenderRuntime runtime = new CompiledRenderRuntime(asset());
        CompiledRenderAnimationController controller = new CompiledRenderAnimationController("rig", runtime);
        assertTrue(controller.open(new RenderAnimationKey("other", "once"), RenderAnimationMode.ONCE).isEmpty());
        controller.close();
        assertTrue(controller.open(new RenderAnimationKey("rig", "once"), RenderAnimationMode.ONCE).isEmpty());
    }



    private static CompiledRenderAsset assetWithEvents(boolean damageOverride, boolean jumpOverride) {
        RenderRig rig = new RenderRig("native", "rig", RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY)),
                Set.of("once", "loop", "hold", "damage", "jump"));
        RenderAnimationTrack track = new RenderAnimationTrack("root", List.of(
                new RenderAnimationFrame(0f, RenderBonePose.IDENTITY, RenderInterpolation.LINEAR),
                new RenderAnimationFrame(.1f,
                        new RenderBonePose(new RenderVec3(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO),
                        RenderInterpolation.LINEAR)
        ));
        Map<String, RenderAnimationTrack> tracks = Map.of("root", track);
        return CompiledRenderAsset.compile("asset", rig, List.of(
                new RenderAnimationClip("once", .1f, RenderAnimationMode.ONCE, false, tracks),
                new RenderAnimationClip("loop", .1f, RenderAnimationMode.LOOP, false, tracks),
                new RenderAnimationClip("hold", .1f, RenderAnimationMode.HOLD, false, tracks),
                new RenderAnimationClip("damage", .1f, RenderAnimationMode.ONCE, damageOverride, tracks),
                new RenderAnimationClip("jump", .1f, RenderAnimationMode.ONCE, jumpOverride, tracks)
        ));
    }

    private static CompiledRenderAsset assetWithDamage(boolean damageOverride) {
        RenderRig rig = new RenderRig("native", "rig", RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY)),
                Set.of("once", "loop", "hold", "damage"));
        RenderAnimationTrack track = new RenderAnimationTrack("root", List.of(
                new RenderAnimationFrame(0f, RenderBonePose.IDENTITY, RenderInterpolation.LINEAR),
                new RenderAnimationFrame(.1f,
                        new RenderBonePose(new RenderVec3(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO),
                        RenderInterpolation.LINEAR)
        ));
        Map<String, RenderAnimationTrack> tracks = Map.of("root", track);
        return CompiledRenderAsset.compile("asset", rig, List.of(
                new RenderAnimationClip("once", .1f, RenderAnimationMode.ONCE, false, tracks),
                new RenderAnimationClip("loop", .1f, RenderAnimationMode.LOOP, false, tracks),
                new RenderAnimationClip("hold", .1f, RenderAnimationMode.HOLD, false, tracks),
                new RenderAnimationClip("damage", .1f, RenderAnimationMode.ONCE, damageOverride, tracks)
        ));
    }

    private static CompiledRenderAsset assetWithDeath(boolean deathOverride) {
        RenderRig rig = new RenderRig("native", "rig", RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY)),
                Set.of("once", "loop", "damage", "death"));
        RenderAnimationTrack track = new RenderAnimationTrack("root", List.of(
                new RenderAnimationFrame(0f, RenderBonePose.IDENTITY, RenderInterpolation.LINEAR),
                new RenderAnimationFrame(.1f,
                        new RenderBonePose(new RenderVec3(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO),
                        RenderInterpolation.LINEAR)
        ));
        Map<String, RenderAnimationTrack> tracks = Map.of("root", track);
        return CompiledRenderAsset.compile("asset", rig, List.of(
                new RenderAnimationClip("once", .1f, RenderAnimationMode.ONCE, false, tracks),
                new RenderAnimationClip("loop", .1f, RenderAnimationMode.LOOP, false, tracks),
                new RenderAnimationClip("damage", .1f, RenderAnimationMode.ONCE, false, tracks),
                new RenderAnimationClip("death", .1f, RenderAnimationMode.ONCE, deathOverride, tracks)
        ));
    }

    private static CompiledRenderAsset asset() {
        RenderRig rig = new RenderRig("native", "rig", RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY)),
                Set.of("once", "loop", "hold"));
        RenderAnimationTrack track = new RenderAnimationTrack("root", List.of(
                new RenderAnimationFrame(0f, RenderBonePose.IDENTITY, RenderInterpolation.LINEAR),
                new RenderAnimationFrame(.1f,
                        new RenderBonePose(new RenderVec3(1, 0, 0), RenderVec3.ONE, RenderVec3.ZERO),
                        RenderInterpolation.LINEAR)
        ));
        Map<String, RenderAnimationTrack> tracks = Map.of("root", track);
        return CompiledRenderAsset.compile("asset", rig, List.of(
                new RenderAnimationClip("once", .1f, RenderAnimationMode.ONCE, false, tracks),
                new RenderAnimationClip("loop", .1f, RenderAnimationMode.LOOP, false, tracks),
                new RenderAnimationClip("hold", .1f, RenderAnimationMode.HOLD, false, tracks)
        ));
    }
}
