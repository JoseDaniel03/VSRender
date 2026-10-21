package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.render.compiled.CompiledRenderAsset;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provider-independent visual payload plan for one native asset.
 * Logical/locator-only parts remain in the compiled rig but receive no Item Display payload.
 */
public final class NativeRenderVisualPlan {
    private final List<VisualPart> visualParts;

    private NativeRenderVisualPlan(List<VisualPart> visualParts) {
        this.visualParts = List.copyOf(visualParts);
    }

    public static NativeRenderVisualPlan compile(NativeRenderAsset asset) {
        Objects.requireNonNull(asset, "asset");
        CompiledRenderAsset compiled = asset.compile();
        ArrayList<VisualPart> visual = new ArrayList<>();
        for (int partIndex = 0; partIndex < compiled.rig().partCount(); partIndex++) {
            String partId = compiled.rig().partId(partIndex);
            if (asset.geometry(partId).isEmpty()) continue;
            visual.add(new VisualPart(
                    partIndex,
                    partId,
                    NativeResourcePackCompiler.itemModelLocation(asset.assetId(), partId)
            ));
        }
        return new NativeRenderVisualPlan(visual);
    }

    public List<VisualPart> visualParts() {
        return visualParts;
    }

    public record VisualPart(int partIndex, String partId, String itemModel) {
        public VisualPart {
            if (partIndex < 0) throw new IllegalArgumentException("partIndex must be >= 0");
            Objects.requireNonNull(partId, "partId");
            Objects.requireNonNull(itemModel, "itemModel");
            if (partId.isBlank()) throw new IllegalArgumentException("partId must not be blank");
            if (itemModel.isBlank()) throw new IllegalArgumentException("itemModel must not be blank");
        }
    }
}
