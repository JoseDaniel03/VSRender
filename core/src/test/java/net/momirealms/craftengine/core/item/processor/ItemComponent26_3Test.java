package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemComponent26_3Test {
    @Test void legacySwingIsPreservedOn26_2() {
        assertEquals(List.of(DataComponentKeys.SWING_ANIMATION),
            ComponentsProcessor.compatibleKeys(DataComponentKeys.SWING_ANIMATION, Set.of(), false));
    }
    @Test void legacySwingBecomesTwoNative26_3Animations() {
        assertEquals(List.of(DataComponentKeys.ATTACK_ANIMATION, DataComponentKeys.INTERACT_ANIMATION),
            ComponentsProcessor.compatibleKeys(DataComponentKeys.SWING_ANIMATION, Set.of(), true));
    }
    @Test void newAttackConfigOverridesOldSwing() {
        assertEquals(List.of(DataComponentKeys.INTERACT_ANIMATION),
            ComponentsProcessor.compatibleKeys(DataComponentKeys.SWING_ANIMATION,
                Set.of("minecraft:attack_animation"), true));
    }
    @Test void newInteractConfigOverridesOldSwing() {
        assertEquals(List.of(DataComponentKeys.ATTACK_ANIMATION),
            ComponentsProcessor.compatibleKeys(DataComponentKeys.SWING_ANIMATION,
                Set.of("interact_animation"), true));
    }
    @Test void bothNewAnimationsWinOverLegacySwing() {
        assertEquals(List.of(), ComponentsProcessor.compatibleKeys(DataComponentKeys.SWING_ANIMATION,
            Set.of("attack_animation","interact_animation"), true));
    }
    @Test void removedMapColorIsIgnoredOnlyOn26_3() {
        assertEquals(List.of(), ComponentsProcessor.compatibleKeys(DataComponentKeys.MAP_COLOR, Set.of(), true));
        assertEquals(List.of(DataComponentKeys.MAP_COLOR),
            ComponentsProcessor.compatibleKeys(DataComponentKeys.MAP_COLOR, Set.of(), false));
    }
    @Test void nativeComponentsAreUnaffected() {
        assertEquals(List.of(DataComponentKeys.MOB_VISIBILITY),
            ComponentsProcessor.compatibleKeys(DataComponentKeys.MOB_VISIBILITY, Set.of(), true));
    }
}
