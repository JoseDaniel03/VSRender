package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.render.profile.RenderProfile;
import net.momirealms.craftengine.core.render.rig.RenderPartSelector;
import net.momirealms.craftengine.core.util.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RenderInstanceOptionsTest {

    @Test
    void snapshotsTintLayers() {
        ArrayList<RenderTintLayer> source = new ArrayList<>();
        source.add(new RenderTintLayer(new Color(0x123456), RenderPartSelector.all()));
        RenderProfile profile = new RenderProfile(null, "Player", null);

        RenderInstanceOptions options = new RenderInstanceOptions(true, profile, source);
        source.clear();

        assertTrue(options.sightTrace());
        assertSame(profile, options.profile());
        assertEquals(1, options.tintLayers().size());
        assertThrows(UnsupportedOperationException.class, () -> options.tintLayers().clear());
    }

    @Test
    void simpleRequestRemainsBackwardCompatible() {
        RenderInstanceOptions options = RenderInstanceOptions.simple(false);
        assertFalse(options.sightTrace());
        assertNull(options.profile());
        assertEquals(List.of(), options.tintLayers());
    }
}
