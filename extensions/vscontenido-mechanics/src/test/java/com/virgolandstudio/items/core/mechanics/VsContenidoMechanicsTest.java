package com.virgolandstudio.items.core.mechanics;

import com.virgolandstudio.items.api.ammo.*;
import com.virgolandstudio.items.api.input.ItemInputAction;
import com.virgolandstudio.items.api.ranged.FireMode;
import com.virgolandstudio.items.core.ammo.*;
import com.virgolandstudio.items.core.math.Vec3;
import com.virgolandstudio.items.core.ranged.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class VsContenidoMechanicsTest {
    @Test void pumpShotgunNeedsManualChamberAndConsumesOneShell() {
        var spec = new CompiledAmmoSystem("faro12:shell", AmmoContainerKind.INTERNAL_MAGAZINE,
                AmmoStorageMode.COUNT, 6, false, FeedMechanismKind.MANUAL_CHAMBER, 1, ReloadMechanismKind.TOP_OFF);
        var feed = new AmmoFeedRuntime(spec, AmmoContainerState.count(6, 6), 0);
        assertFalse(feed.fire());
        assertTrue(feed.feedOne());
        assertTrue(feed.fire());
        assertEquals(5.0, feed.container().available());
        assertEquals(0, feed.chambered());
    }

    @Test void automaticChamberFeedsNextShellAfterShot() {
        var spec = new CompiledAmmoSystem("faro12:shell", AmmoContainerKind.INTERNAL_MAGAZINE,
                AmmoStorageMode.COUNT, 6, false, FeedMechanismKind.AUTO_CHAMBER, 1, ReloadMechanismKind.TOP_OFF);
        var feed = new AmmoFeedRuntime(spec, AmmoContainerState.count(6, 6), 0);
        assertTrue(feed.fireAndCycle());
        assertEquals(4.0, feed.container().available());
        assertEquals(1, feed.chambered());
    }

    @Test void fireControlUsesFractionalCadenceAndNoPerWeaponScheduler() {
        var shots = new FireControlRuntime();
        var id = UUID.randomUUID();
        var semi = new CompiledFireControl(FireMode.SEMI, 120.0, 1, 2);
        assertTrue(shots.request(id, semi, 100).fireNow());
        assertFalse(shots.request(id, semi, 109).fireNow());
        assertTrue(shots.request(id, semi, 110).fireNow());
        assertEquals(0, shots.activeCount());
    }

    @Test void spreadIsNormalizedAndZeroSpreadPreservesDirection() {
        var forward = new Vec3(0, 0, 1);
        assertEquals(forward, SpreadSampler.sampleCone(forward, 0, () -> 0.5));
        assertEquals(1.0, SpreadSampler.sampleCone(forward, 8, () -> 0.5).length(), 1.0e-9);
    }

    @Test void controlsRemainSemanticAndIndependentOfClientKeycodes() {
        assertEquals(ItemInputAction.FIRE, ItemInputAction.parse("disparar").orElseThrow());
        assertEquals(ItemInputAction.RELOAD, ItemInputAction.parse("recargar").orElseThrow());
        assertEquals(ItemInputAction.FIRE_MODE, ItemInputAction.parse("modo_fuego").orElseThrow());
    }
}
