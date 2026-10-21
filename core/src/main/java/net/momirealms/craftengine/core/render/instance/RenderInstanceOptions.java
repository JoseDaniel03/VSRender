package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.render.asset.RenderUsageContext;
import net.momirealms.craftengine.core.render.profile.RenderProfile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Explicit provider-independent customization for concrete render instances. */
public record RenderInstanceOptions(
        boolean sightTrace,
        @Nullable RenderProfile profile,
        List<RenderTintLayer> tintLayers,
        @Nullable RenderUsageContext usageContext
) {
    public RenderInstanceOptions {
        tintLayers = tintLayers == null ? List.of() : List.copyOf(tintLayers);
    }

    /** Source-compatible constructor for legacy callers that have no representation context. */
    public RenderInstanceOptions(boolean sightTrace, @Nullable RenderProfile profile, List<RenderTintLayer> tintLayers) {
        this(sightTrace, profile, tintLayers, null);
    }

    public static RenderInstanceOptions simple(boolean sightTrace) {
        return new RenderInstanceOptions(sightTrace, null, List.of(), null);
    }

    public static RenderInstanceOptions forContext(RenderUsageContext usageContext, boolean sightTrace) {
        if (usageContext == null) throw new NullPointerException("usageContext");
        return new RenderInstanceOptions(sightTrace, null, List.of(), usageContext);
    }

    public RenderInstanceOptions withUsageContext(RenderUsageContext usageContext) {
        if (usageContext == null) throw new NullPointerException("usageContext");
        return new RenderInstanceOptions(sightTrace, profile, tintLayers, usageContext);
    }
}
