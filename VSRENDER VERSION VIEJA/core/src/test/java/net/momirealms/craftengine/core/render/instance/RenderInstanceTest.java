package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.entity.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderInstanceTest {
    @Test
    void refreshUsesProviderIndependentHideThenShowLifecycle() {
        var calls = new StringBuilder();
        RenderInstance instance = new RenderInstance() {
            @Override public void show(Player viewer) { calls.append("show"); }
            @Override public void hide(Player viewer) { calls.append("hide>"); }
            @Override public void close() {}
        };

        instance.refresh(null);

        assertEquals("hide>show", calls.toString());
    }

    @Test
    void capabilitiesAreOptionalForLegacyInstances() {
        RenderInstance instance = new RenderInstance() {
            @Override public void show(Player viewer) {}
            @Override public void hide(Player viewer) {}
            @Override public void close() {}
        };

        assertTrue(instance.attachment("socket").isEmpty());
        assertTrue(instance.animations().isEmpty());
    }
}

