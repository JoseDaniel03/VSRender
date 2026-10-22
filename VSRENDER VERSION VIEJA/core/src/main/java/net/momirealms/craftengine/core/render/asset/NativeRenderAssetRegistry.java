package net.momirealms.craftengine.core.render.asset;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Small deterministic registry for canonical native assets shared by all representation contexts. */
public final class NativeRenderAssetRegistry {
    private final Map<String, NativeRenderAsset> assets = new LinkedHashMap<>();

    public synchronized void register(NativeRenderAsset asset) {
        Objects.requireNonNull(asset, "asset");
        if (assets.putIfAbsent(asset.assetId(), asset) != null) {
            throw new IllegalArgumentException("native asset already registered: " + asset.assetId());
        }
    }

    public synchronized void replace(NativeRenderAsset asset) {
        Objects.requireNonNull(asset, "asset");
        assets.put(asset.assetId(), asset);
    }

    public synchronized Optional<NativeRenderAsset> get(String assetId) {
        return Optional.ofNullable(assets.get(assetId));
    }

    public synchronized Optional<NativeRenderAsset> resolve(String assetId, RenderUsageContext context) {
        Objects.requireNonNull(context, "context");
        NativeRenderAsset asset = assets.get(assetId);
        if (asset == null || !asset.representation().supports(context)) return Optional.empty();
        return Optional.of(asset);
    }

    public synchronized Collection<NativeRenderAsset> assets() {
        return Collections.unmodifiableList(List.copyOf(assets.values()));
    }

    public synchronized void clear() {
        assets.clear();
    }
}
