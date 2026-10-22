package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataSourceConfig;
import net.momirealms.craftengine.core.entity.furniture.data.SourceItemColorDataSourceConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.render.rig.RenderPartSelector;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class NativeModelFurnitureElementConfig implements FurnitureElementConfig<NativeModelFurnitureElement> {
    public static final FurnitureElementConfigFactory<NativeModelFurnitureElement> FACTORY = new Factory();

    final String model;
    final Vector3f position;
    final float yaw;
    final float pitch;
    final boolean sightTrace;
    final List<NativeModelTintLayerConfig> tintLayers;
    final Predicate<PlayerContext> predicate;

    private NativeModelFurnitureElementConfig(String model,
                                              Vector3f position,
                                              float yaw,
                                              float pitch,
                                              boolean sightTrace,
                                              List<NativeModelTintLayerConfig> tintLayers,
                                              Predicate<PlayerContext> predicate) {
        this.model = model;
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.sightTrace = sightTrace;
        this.tintLayers = List.copyOf(tintLayers);
        this.predicate = predicate;
    }

    @Override
    public NativeModelFurnitureElement create(@NotNull Furniture furniture) {
        return new NativeModelFurnitureElement(furniture, this);
    }

    @Override
    public Class<NativeModelFurnitureElement> elementClass() {
        return NativeModelFurnitureElement.class;
    }

    private static final class Factory implements FurnitureElementConfigFactory<NativeModelFurnitureElement> {
        private static final String[] SIGHT_TRACE = ConfigKeys.of("sight_trace");
        private static final String[] TINTS = ConfigKeys.of("tint(s)");
        private static final String[] TINT_SOURCE = ConfigKeys.of("tint_source");
        private static final String[] TINT_BONES = ConfigKeys.of("tint_bone(s)");
        private static final String[] TINT_CHILDREN = ConfigKeys.of("tint_children");
        private static final String[] LAYER_SOURCE = ConfigKeys.of("source", "tint_source");
        private static final String[] LAYER_BONES = ConfigKeys.of("bone(s)", "tint_bone(s)");
        private static final String[] LAYER_CHILDREN = ConfigKeys.of("children", "tint_children");

        @Override
        public FurnitureElementConfig<NativeModelFurnitureElement> create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList(
                    ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            return new NativeModelFurnitureElementConfig(
                    section.getNonEmptyString("model"),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getFloat("yaw"),
                    section.getFloat("pitch"),
                    section.getBoolean(SIGHT_TRACE, true),
                    tintLayers(section),
                    MiscUtils.allOf(conditions)
            );
        }

        private static List<NativeModelTintLayerConfig> tintLayers(ConfigSection section) {
            if (section.getValue(TINTS) != null) {
                return section.getSectionList(TINTS, Factory::tintLayer);
            }
            FurnitureDataSourceConfig<Color> source = section.getValue(
                    TINT_SOURCE, SourceItemColorDataSourceConfig::fromConfig);
            if (source == null) return List.of();
            return List.of(new NativeModelTintLayerConfig(
                    source,
                    partSelector(section, TINT_BONES, TINT_CHILDREN)
            ));
        }

        private static NativeModelTintLayerConfig tintLayer(ConfigSection section) {
            FurnitureDataSourceConfig<Color> source = section.getNonNullValue(
                    LAYER_SOURCE,
                    ConfigConstants.ARGUMENT_ANY,
                    SourceItemColorDataSourceConfig::fromConfig
            );
            return new NativeModelTintLayerConfig(
                    source,
                    partSelector(section, LAYER_BONES, LAYER_CHILDREN)
            );
        }

        private static RenderPartSelector partSelector(ConfigSection section, String[] partKeys, String[] childrenKeys) {
            ConfigValue value = section.getValue(partKeys);
            if (value == null) return RenderPartSelector.all();
            Set<String> parts = Set.copyOf(value.getAsStringList());
            return RenderPartSelector.named(parts, section.getBoolean(childrenKeys, true));
        }
    }
}
