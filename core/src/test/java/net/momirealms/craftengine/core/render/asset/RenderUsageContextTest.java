package net.momirealms.craftengine.core.render.asset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderUsageContextTest {
    @Test
    void contextsChooseTheirHostExplicitly() {
        assertFalse(RenderUsageContext.ITEM.entityOwned());
        assertFalse(RenderUsageContext.BLOCK.entityOwned());
        assertFalse(RenderUsageContext.FURNITURE.entityOwned());
        assertTrue(RenderUsageContext.ENTITY.entityOwned());
        assertTrue(RenderUsageContext.PLAYER_ATTACHMENT.entityOwned());
        assertTrue(RenderUsageContext.VEHICLE.entityOwned());
    }
}
