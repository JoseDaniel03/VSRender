package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

public final class NativeModelBlockEntityElementConfig implements BlockEntityElementConfig<NativeModelBlockEntityElement> {
    public static final BlockEntityElementConfigFactory<NativeModelBlockEntityElement> FACTORY = new Factory();

    final String model;
    final Vector3f position;
    final float yaw;
    final float pitch;
    final boolean sightTrace;
    final Predicate<PlayerContext> predicate;
    final boolean hasCondition;

    private NativeModelBlockEntityElementConfig(String model,
                                                Vector3f position,
                                                float yaw,
                                                float pitch,
                                                boolean sightTrace,
                                                Predicate<PlayerContext> predicate,
                                                boolean hasCondition) {
        this.model = model;
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.sightTrace = sightTrace;
        this.predicate = predicate;
        this.hasCondition = hasCondition;
    }

    @Override
    public NativeModelBlockEntityElement create(CEChunk chunk, BlockPos pos) {
        return new NativeModelBlockEntityElement(this, chunk.world.world, pos);
    }

    @Override
    public Class<NativeModelBlockEntityElement> elementClass() {
        return NativeModelBlockEntityElement.class;
    }

    private static final class Factory implements BlockEntityElementConfigFactory<NativeModelBlockEntityElement> {
        private static final String[] SIGHT_TRACE = ConfigKeys.of("sight_trace");

        @Override
        public NativeModelBlockEntityElementConfig create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList(
                    ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            return new NativeModelBlockEntityElementConfig(
                    section.getNonEmptyString("model"),
                    section.getVector3f("position", ConfigConstants.CENTER_VECTOR3),
                    section.getFloat("yaw"),
                    section.getFloat("pitch"),
                    section.getBoolean(SIGHT_TRACE, true),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
