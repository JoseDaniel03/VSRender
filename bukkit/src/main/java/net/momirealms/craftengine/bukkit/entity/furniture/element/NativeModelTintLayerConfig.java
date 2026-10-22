package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataSourceConfig;
import net.momirealms.craftengine.core.render.rig.RenderPartSelector;
import net.momirealms.craftengine.core.util.Color;

record NativeModelTintLayerConfig(
        FurnitureDataSourceConfig<Color> source,
        RenderPartSelector parts
) {
}
