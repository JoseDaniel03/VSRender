package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.render.rig.RenderPartSelector;
import net.momirealms.craftengine.core.util.Color;

import java.util.Objects;

/** Provider-independent tint applied to selected rig parts when an instance is materialized. */
public record RenderTintLayer(Color color, RenderPartSelector parts) {
    public RenderTintLayer {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(parts, "parts");
    }
}
