package net.momirealms.craftengine.core.render.rig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RenderPartSelectorTest {
    @Test
    void namedSelectionIsImmutableAndPreservesChildrenPolicy() {
        var selector = RenderPartSelector.named(List.of("body", "arm"), true);
        assertEquals(RenderPartSelector.Mode.NAMED, selector.mode());
        assertEquals(2, selector.names().size());
        assertTrue(selector.names().contains("body"));
        assertTrue(selector.includeChildren());
        assertThrows(UnsupportedOperationException.class, () -> selector.names().add("head"));
    }

    @Test
    void emptyNamedSelectionCollapsesToNone() {
        assertSame(RenderPartSelector.none(), RenderPartSelector.named(List.of(), true));
    }

    @Test
    void allAndNoneAreExplicitModes() {
        assertEquals(RenderPartSelector.Mode.ALL, RenderPartSelector.all().mode());
        assertEquals(RenderPartSelector.Mode.NONE, RenderPartSelector.none().mode());
    }
}
