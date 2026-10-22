package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.render.asset.NativeRenderAsset;
import net.momirealms.craftengine.core.render.asset.RenderUsageContext;
import net.momirealms.craftengine.core.render.instance.RenderEntityInstanceProvider;
import net.momirealms.craftengine.core.render.instance.RenderEntityInstanceRequest;
import net.momirealms.craftengine.core.render.instance.RenderInstance;
import net.momirealms.craftengine.core.render.instance.RenderInstanceProvider;
import net.momirealms.craftengine.core.render.instance.RenderInstanceRequest;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigProvider;
import net.momirealms.craftengine.core.render.rig.RenderRigType;

import net.momirealms.craftengine.core.util.Key;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/** First-party provider for assets imported from blueprint/native without BetterModel. */
public final class BukkitNativeAssetProvider implements RenderInstanceProvider, RenderEntityInstanceProvider, RenderRigProvider {
    private final BukkitCraftEngine plugin;
    private final ConcurrentHashMap<String, CachedDisplayAsset> displayAssets = new ConcurrentHashMap<>();

    public BukkitNativeAssetProvider(BukkitCraftEngine plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public String providerId() {
        return NativeRenderAsset.PROVIDER_ID;
    }

    @Override
    public RenderInstance createInstance(RenderInstanceRequest request) {
        Objects.requireNonNull(request, "request");
        RenderUsageContext context = request.usageContext();
        if (context == null || context.entityOwned() || request.profile() != null) return null;

        NativeRenderAsset asset = plugin.packManager().nativeRenderAssets()
                .resolve(request.model().modelId(), context)
                .orElse(null);
        if (asset == null) return null;

        BukkitNativeDisplayAsset displayAsset = displayAsset(asset).withTintLayers(request.tintLayers());
        return new BukkitNativeRenderInstance(
                displayAsset,
                request.position(),
                request.sightTrace(),
                5d,
                Double.POSITIVE_INFINITY,
                plugin.scheduler().platform()
        );
    }

    @Override
    public CompletionStage<RenderInstance> createEntityInstance(RenderEntityInstanceRequest request) {
        Objects.requireNonNull(request, "request");
        RenderUsageContext context = request.usageContext();
        if (context == null || !context.entityOwned() || request.profile() != null) {
            return CompletableFuture.completedFuture(null);
        }

        NativeRenderAsset asset = plugin.packManager().nativeRenderAssets()
                .resolve(request.model().modelId(), context)
                .orElse(null);
        if (asset == null) return CompletableFuture.completedFuture(null);

        BukkitNativeDisplayAsset displayAsset = displayAsset(asset).withTintLayers(request.tintLayers());
        boolean includeOwnerViewer = context == RenderUsageContext.PLAYER_ATTACHMENT;
        RenderInstance instance = new BukkitEntityNativeRenderInstance(
                displayAsset,
                request.owner(),
                includeOwnerViewer,
                true,
                true,
                request.sightTrace(),
                5d,
                Double.POSITIVE_INFINITY,
                plugin.scheduler().platform()
        );
        return CompletableFuture.completedFuture(instance);
    }

    @Override
    public RenderRig resolveRig(String rigId, RenderRigType type) {
        NativeRenderAsset asset = plugin.packManager().nativeRenderAssets().get(rigId).orElse(null);
        return asset != null && asset.rig().type() == type ? asset.rig() : null;
    }

    private BukkitNativeDisplayAsset displayAsset(NativeRenderAsset asset) {
        CachedDisplayAsset cached = displayAssets.get(asset.assetId());
        if (cached != null && cached.sourceSha256().equals(asset.sourceSha256())) return cached.asset();

        BukkitNativeDisplayAsset compiled = BukkitNativeAssetAdapter.adapt(asset, (carrierItem, itemModel) -> {
            Item item = Item.byId(Key.of(carrierItem));
            if (item == null) {
                throw new IllegalArgumentException("unknown native render carrier item: " + carrierItem);
            }
            item.itemModel(itemModel);
            return item.minecraftItem();
        });
        displayAssets.put(asset.assetId(), new CachedDisplayAsset(asset.sourceSha256(), compiled));
        return compiled;
    }

    private record CachedDisplayAsset(String sourceSha256, BukkitNativeDisplayAsset asset) {
    }
}
