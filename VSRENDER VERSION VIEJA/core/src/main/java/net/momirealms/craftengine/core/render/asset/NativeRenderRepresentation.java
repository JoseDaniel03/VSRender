package net.momirealms.craftengine.core.render.asset;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Provider-independent usage declaration for one native asset.
 * Geometry/rig data is shared; contexts only describe where the asset is allowed to be used.
 */
public record NativeRenderRepresentation(Set<RenderUsageContext> contexts, String carrierItem) {
    public static final String DEFAULT_CARRIER_ITEM = "minecraft:paper";

    public NativeRenderRepresentation {
        Objects.requireNonNull(contexts, "contexts");
        if (contexts.isEmpty()) throw new IllegalArgumentException("contexts must not be empty");
        contexts = Collections.unmodifiableSet(EnumSet.copyOf(contexts));
        carrierItem = requireId(carrierItem, "carrierItem");
    }

    public static NativeRenderRepresentation allContexts() {
        return new NativeRenderRepresentation(EnumSet.allOf(RenderUsageContext.class), DEFAULT_CARRIER_ITEM);
    }

    public static NativeRenderRepresentation of(RenderUsageContext first, RenderUsageContext... rest) {
        Objects.requireNonNull(first, "first");
        EnumSet<RenderUsageContext> contexts = EnumSet.of(first, rest);
        return new NativeRenderRepresentation(contexts, DEFAULT_CARRIER_ITEM);
    }

    public boolean supports(RenderUsageContext context) {
        return contexts.contains(Objects.requireNonNull(context, "context"));
    }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
