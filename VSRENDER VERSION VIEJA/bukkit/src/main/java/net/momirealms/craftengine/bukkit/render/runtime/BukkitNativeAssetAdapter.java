package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.render.asset.NativeRenderAsset;
import net.momirealms.craftengine.core.render.asset.NativeRenderVisualPlan;

import java.util.Arrays;
import java.util.Objects;

/** Converts one canonical native asset into the Bukkit Item Display payload sidecar. */
public final class BukkitNativeAssetAdapter {
    private BukkitNativeAssetAdapter() {
    }

    public static BukkitNativeDisplayAsset adapt(NativeRenderAsset asset, ItemPayloadFactory payloadFactory) {
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(payloadFactory, "payloadFactory");
        var compiled = asset.compile();
        BukkitDisplayPartPayload[] payloads = new BukkitDisplayPartPayload[compiled.rig().partCount()];
        Arrays.fill(payloads, BukkitDisplayPartPayload.transformOnly());

        NativeRenderVisualPlan visualPlan = NativeRenderVisualPlan.compile(asset);
        for (NativeRenderVisualPlan.VisualPart visual : visualPlan.visualParts()) {
            Object minecraftItem = Objects.requireNonNull(
                    payloadFactory.create(asset.representation().carrierItem(), visual.itemModel()),
                    "payloadFactory returned null"
            );
            payloads[visual.partIndex()] = BukkitDisplayPartPayload.item(
                    minecraftItem,
                    (byte) 0,
                    0f, 0f, 0f,
                    0f, 0f, 0f,
                    1f, 1f, 1f
            );
        }
        return new BukkitNativeDisplayAsset(asset.assetId(), compiled, payloads);
    }

    @FunctionalInterface
    public interface ItemPayloadFactory {
        Object create(String carrierItem, String itemModel);
    }
}
