package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataResolver;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.asset.NativeRenderAsset;
import net.momirealms.craftengine.core.render.asset.RenderUsageContext;
import net.momirealms.craftengine.core.render.instance.RenderInstance;
import net.momirealms.craftengine.core.render.instance.RenderInstanceOptions;
import net.momirealms.craftengine.core.render.instance.RenderInstanceRequest;
import net.momirealms.craftengine.core.render.instance.RenderTintLayer;
import net.momirealms.craftengine.core.render.model.RenderModelReference;
import net.momirealms.craftengine.core.render.rig.RenderPartSelector;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class NativeModelFurnitureElement extends AbstractConditionalFurnitureElement {
    private final Furniture furniture;
    private final NativeModelFurnitureElementConfig config;
    private final WorldPosition position;
    private final List<TintLayer> tintLayers;
    private RenderInstance renderInstance;

    NativeModelFurnitureElement(Furniture furniture, NativeModelFurnitureElementConfig config) {
        super(config.predicate);
        this.furniture = furniture;
        this.config = config;
        WorldPosition furniturePos = furniture.position();
        Vec3d relative = furniture.getRelativePosition(config.position);
        this.position = new WorldPosition(
                furniturePos.world,
                relative.x,
                relative.y,
                relative.z,
                furniturePos.xRot + config.pitch,
                furniturePos.yRot + config.yaw
        );
        this.tintLayers = new ArrayList<>(config.tintLayers.size());
        for (NativeModelTintLayerConfig layer : config.tintLayers) {
            this.tintLayers.add(new TintLayer(layer.source().bind(furniture), layer.parts()));
        }
        resolveTints();
        this.renderInstance = createRenderInstance();
    }

    private RenderInstance createRenderInstance() {
        List<RenderTintLayer> resolved = new ArrayList<>(tintLayers.size());
        for (TintLayer layer : tintLayers) {
            if (layer.value != null) resolved.add(new RenderTintLayer(layer.value, layer.parts));
        }
        return BukkitCraftEngine.instance().compatibilityManager().createRenderInstance(
                new RenderInstanceRequest(
                        RenderModelReference.of(NativeRenderAsset.PROVIDER_ID, config.model),
                        position,
                        new RenderInstanceOptions(
                                config.sightTrace,
                                null,
                                resolved,
                                RenderUsageContext.FURNITURE
                        )
                )
        );
    }

    private void resolveTints() {
        for (TintLayer layer : tintLayers) layer.value = layer.source.resolve();
    }

    @Override
    public @NotNull Furniture furniture() {
        return furniture;
    }

    @Override
    public void showInternal(Player player) {
        if (renderInstance != null) renderInstance.show(player);
    }

    @Override
    public void hide(Player player) {
        if (renderInstance != null) renderInstance.hide(player);
    }

    @Override
    public void update(Player player) {
        hide(player);
        show(player);
    }

    @Override
    public void deactivate() {
        if (renderInstance != null) {
            renderInstance.close();
            renderInstance = null;
        }
    }

    @Override
    public void activate() {
        if (renderInstance == null) {
            resolveTints();
            renderInstance = createRenderInstance();
        }
    }

    @Override
    public void gatherInteractableEntityId(IntConsumer collector) {
    }

    private static final class TintLayer {
        private final FurnitureDataResolver<Color> source;
        private final RenderPartSelector parts;
        private Color value;

        private TintLayer(FurnitureDataResolver<Color> source, RenderPartSelector parts) {
            this.source = source;
            this.parts = parts;
        }
    }
}
