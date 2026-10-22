package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.block.entity.render.element.AbstractConstantBlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.asset.NativeRenderAsset;
import net.momirealms.craftengine.core.render.asset.RenderUsageContext;
import net.momirealms.craftengine.core.render.instance.RenderInstance;
import net.momirealms.craftengine.core.render.instance.RenderInstanceOptions;
import net.momirealms.craftengine.core.render.instance.RenderInstanceRequest;
import net.momirealms.craftengine.core.render.model.RenderModelReference;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class NativeModelBlockEntityElement extends AbstractConstantBlockEntityElement {
    private final NativeModelBlockEntityElementConfig config;
    private final WorldPosition position;
    private RenderInstance renderInstance;

    NativeModelBlockEntityElement(NativeModelBlockEntityElementConfig config, World world, BlockPos pos) {
        super(config.predicate, config.hasCondition);
        this.config = config;
        Vector3f offset = config.position;
        this.position = new WorldPosition(
                world,
                pos.x() + (double) offset.x,
                pos.y() + (double) offset.y,
                pos.z() + (double) offset.z,
                config.pitch,
                config.yaw
        );
        this.renderInstance = createRenderInstance();
    }

    private RenderInstance createRenderInstance() {
        return BukkitCraftEngine.instance().compatibilityManager().createRenderInstance(
                new RenderInstanceRequest(
                        RenderModelReference.of(NativeRenderAsset.PROVIDER_ID, config.model),
                        position,
                        RenderInstanceOptions.forContext(RenderUsageContext.BLOCK, config.sightTrace)
                )
        );
    }

    @Override
    public void hide(@NotNull Player player) {
        if (renderInstance != null) renderInstance.hide(player);
    }

    @Override
    public void showInternal(@NotNull Player player) {
        if (renderInstance != null) renderInstance.show(player);
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
        if (renderInstance == null) renderInstance = createRenderInstance();
    }
}
