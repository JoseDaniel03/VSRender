package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.render.asset.RenderUsageContext;
import net.momirealms.craftengine.core.render.model.RenderModelReference;
import net.momirealms.craftengine.core.render.profile.RenderProfile;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/** Provider-independent request for a render instance whose anchor is owned by a live entity. */
public record RenderEntityInstanceRequest(
        RenderModelReference model,
        Entity owner,
        RenderInstanceOptions options
) {
    public RenderEntityInstanceRequest {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(options, "options");
    }

    public RenderEntityInstanceRequest(RenderModelReference model, Entity owner) {
        this(model, owner, RenderInstanceOptions.simple(false));
    }

    public boolean sightTrace() { return options.sightTrace(); }
    public @Nullable RenderProfile profile() { return options.profile(); }
    public List<RenderTintLayer> tintLayers() { return options.tintLayers(); }
    public @Nullable RenderUsageContext usageContext() { return options.usageContext(); }
}
