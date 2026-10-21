package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.render.asset.RenderUsageContext;
import net.momirealms.craftengine.core.render.model.RenderModelReference;
import net.momirealms.craftengine.core.render.profile.RenderProfile;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/** Provider-independent request for a concrete render instance. */
public record RenderInstanceRequest(
        RenderModelReference model,
        WorldPosition position,
        RenderInstanceOptions options
) {
    public RenderInstanceRequest {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(options, "options");
    }

    /** Backward-compatible simple request used by blocks and uncustomized instances. */
    public RenderInstanceRequest(RenderModelReference model, WorldPosition position, boolean sightTrace) {
        this(model, position, RenderInstanceOptions.simple(sightTrace));
    }

    public boolean sightTrace() {
        return options.sightTrace();
    }

    public @Nullable RenderProfile profile() {
        return options.profile();
    }

    public List<RenderTintLayer> tintLayers() {
        return options.tintLayers();
    }

    public @Nullable RenderUsageContext usageContext() {
        return options.usageContext();
    }
}
