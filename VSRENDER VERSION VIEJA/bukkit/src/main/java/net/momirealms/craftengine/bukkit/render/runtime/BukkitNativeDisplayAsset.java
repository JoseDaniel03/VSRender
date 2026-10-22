package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.render.compiled.CompiledRenderAsset;
import net.momirealms.craftengine.core.render.compiled.IndexedRenderRig;
import net.momirealms.craftengine.core.render.instance.RenderTintLayer;
import net.momirealms.craftengine.core.render.rig.RenderPartSelector;
import net.momirealms.craftengine.core.util.Color;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Platform sidecar joining one provider-independent compiled asset with Bukkit display payloads. */
public final class BukkitNativeDisplayAsset {
    public static final int NO_TINT = -1;

    private final CompiledRenderAsset asset;
    private final String rigId;
    private final BukkitDisplayPartPayload[] payloads;
    private final BukkitDisplayPayloadBinding[] bindings;
    private final int[] tintRgbByPart;
    private final int visualPartCount;
    private final int dynamicBindingCount;

    public BukkitNativeDisplayAsset(CompiledRenderAsset asset, BukkitDisplayPartPayload[] payloads) {
        this(asset.assetId(), asset, payloads, null, null);
    }

    public BukkitNativeDisplayAsset(String rigId, CompiledRenderAsset asset, BukkitDisplayPartPayload[] payloads) {
        this(rigId, asset, payloads, null, null);
    }

    public BukkitNativeDisplayAsset(String rigId, CompiledRenderAsset asset, BukkitDisplayPartPayload[] payloads,
                                    BukkitDisplayPayloadBinding[] bindings) {
        this(rigId, asset, payloads, bindings, null);
    }

    private BukkitNativeDisplayAsset(String rigId, CompiledRenderAsset asset, BukkitDisplayPartPayload[] payloads,
                                     BukkitDisplayPayloadBinding[] bindings, int[] tintRgbByPart) {
        this.rigId = Objects.requireNonNull(rigId, "rigId");
        if (rigId.isBlank()) throw new IllegalArgumentException("rigId must not be blank");
        this.asset = Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(payloads, "payloads");
        if (payloads.length != asset.rig().partCount()) {
            throw new IllegalArgumentException("payload/rig part count mismatch");
        }
        this.payloads = Arrays.copyOf(payloads, payloads.length);
        if (bindings != null && bindings.length != payloads.length) {
            throw new IllegalArgumentException("binding/rig part count mismatch");
        }
        if (tintRgbByPart != null && tintRgbByPart.length != payloads.length) {
            throw new IllegalArgumentException("tint/rig part count mismatch");
        }
        this.bindings = new BukkitDisplayPayloadBinding[payloads.length];
        this.tintRgbByPart = new int[payloads.length];
        Arrays.fill(this.tintRgbByPart, NO_TINT);
        if (tintRgbByPart != null) System.arraycopy(tintRgbByPart, 0, this.tintRgbByPart, 0, tintRgbByPart.length);
        int count = 0;
        int dynamic = 0;
        for (int i = 0; i < this.payloads.length; i++) {
            BukkitDisplayPartPayload payload = this.payloads[i];
            if (payload == null) payload = BukkitDisplayPartPayload.transformOnly();
            this.payloads[i] = payload;
            BukkitDisplayPayloadBinding binding = bindings == null || bindings[i] == null
                    ? BukkitDisplayPayloadBinding.STATIC : bindings[i];
            this.bindings[i] = binding;
            if (binding.dynamic()) dynamic++;
            if (payload.hasItem()) count++;
        }
        visualPartCount = count;
        dynamicBindingCount = dynamic;
    }

    /**
     * Returns an immutable sidecar with instance tint policy resolved to dense part indices.
     * Later overlapping layers win, matching BetterModel's sequential TrackerUpdateAction tint behavior.
     */
    public BukkitNativeDisplayAsset withTintLayers(List<RenderTintLayer> layers) {
        if (layers == null || layers.isEmpty()) return this;
        int[] resolved = new int[asset.rig().partCount()];
        Arrays.fill(resolved, NO_TINT);
        IndexedRenderRig rig = asset.rig();
        for (RenderTintLayer layer : layers) {
            Objects.requireNonNull(layer, "tint layer");
            boolean[] selection = resolveSelection(rig, layer.parts());
            int rgb = Color.transparent(layer.color().color());
            for (int part = 0; part < resolved.length; part++) {
                if (selection[part]) resolved[part] = rgb;
            }
        }
        return new BukkitNativeDisplayAsset(rigId, asset, payloads, bindings, resolved);
    }

    private static boolean[] resolveSelection(IndexedRenderRig rig, RenderPartSelector selector) {
        boolean[] selected = new boolean[rig.partCount()];
        switch (selector.mode()) {
            case ALL -> Arrays.fill(selected, true);
            case NONE -> { }
            case NAMED -> {
                Set<String> names = selector.names();
                for (int part = 0; part < selected.length; part++) {
                    int cursor = part;
                    while (cursor >= 0) {
                        if (names.contains(rig.partId(cursor))) {
                            selected[part] = true;
                            break;
                        }
                        if (!selector.includeChildren()) break;
                        cursor = rig.parentIndex(cursor);
                    }
                }
            }
        }
        return selected;
    }

    public String rigId() { return rigId; }
    public CompiledRenderAsset asset() { return asset; }
    public int partCount() { return payloads.length; }
    public int visualPartCount() { return visualPartCount; }
    public int dynamicBindingCount() { return dynamicBindingCount; }
    public BukkitDisplayPartPayload payload(int partIndex) { return payloads[partIndex]; }
    public BukkitDisplayPayloadBinding binding(int partIndex) { return bindings[partIndex]; }
    public int tintRgb(int partIndex) { return tintRgbByPart[partIndex]; }
    BukkitDisplayPartPayload[] payloadsUnsafe() { return payloads; }
}
