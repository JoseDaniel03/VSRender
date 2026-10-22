package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.property.type.SofaShape;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

import java.util.Optional;

public final class SofaBlockBehavior extends WaterloggedBlockBehavior {
    public static final BlockBehaviorFactory<SofaBlockBehavior> FACTORY = new Factory();
    public final Property<Direction> facingProperty;
    public final Property<SofaShape> shapeProperty;
    public final Property<Boolean> leftProperty;
    public final Property<Boolean> rightProperty;
    private final boolean facePlayer;

    private SofaBlockBehavior(BlockDefinition block,
                              Property<Direction> facing,
                              Property<SofaShape> shape,
                              Property<Boolean> waterlogged,
                              Property<Boolean> left,
                              Property<Boolean> right,
                              boolean facePlayer) {
        super(block, waterlogged);
        this.facingProperty = facing;
        this.shapeProperty = shape;
        this.leftProperty = left;
        this.rightProperty = right;
        this.facePlayer = facePlayer;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        BlockPos clickedPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();
        if (this.facePlayer) facing = facing.opposite();
        ImmutableBlockState blockState = state.owner().value().defaultState()
                .with(this.facingProperty, facing);
        if (super.waterloggedProperty != null) {
            Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(context.getLevel().minecraftWorld(), LocationUtils.toBlockPos(clickedPos));
            blockState = blockState.with(this.waterloggedProperty, FluidStateProxy.INSTANCE.getType(fluidState) == FluidsProxy.WATER);
        }
        return updateConnections(blockState, context.getLevel().minecraftWorld(), clickedPos);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return blockState;
        ImmutableBlockState customState = optionalCustomState.get();
        if (super.waterloggedProperty != null && customState.get(this.waterloggedProperty)) {
            LevelAccessorProxy.INSTANCE.scheduleTick$1(args[updateShape$level], args[updateShape$blockPos], FluidsProxy.WATER, 5);
        }
        Direction direction = DirectionUtils.fromNMSDirection(VersionHelper.isOrAbove1_21_2 ? args[4] : args[1]);
        ImmutableBlockState updated = updateConnections(customState, level, LocationUtils.fromBlockPos(blockPos));
        return direction.axis().isHorizontal()
                ? updated.customBlockState().minecraftState()
                : blockState;
    }

    /** Optional lateral properties keep existing sofa configurations unchanged. */
    private ImmutableBlockState updateConnections(ImmutableBlockState state, Object level, BlockPos pos) {
        ImmutableBlockState result = state.with(this.shapeProperty, getSofaShape(state, level, pos));
        Direction facing = state.get(this.facingProperty);
        if (this.leftProperty != null) {
            result = result.with(this.leftProperty, connectsTo(state, level, pos.relative(facing.counterClockWise())));
        }
        if (this.rightProperty != null) {
            result = result.with(this.rightProperty, connectsTo(state, level, pos.relative(facing.clockWise())));
        }
        return result;
    }

    private boolean connectsTo(ImmutableBlockState state, Object level, BlockPos pos) {
        Object neighbor = BlockGetterProxy.INSTANCE.getBlockState(level, LocationUtils.toBlockPos(pos));
        return BlockStateUtils.getOptionalCustomBlockState(neighbor)
                .filter(other -> other.owner().value() == state.owner().value())
                .map(other -> other.get(this.facingProperty) == state.get(this.facingProperty))
                .orElse(false);
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args) {
        if (this.leftProperty == null && this.rightProperty == null) return;
        BlockStateUtils.getOptionalCustomBlockState(args[0]).ifPresent(state -> {
            ImmutableBlockState updated = updateConnections(state, args[1], LocationUtils.fromBlockPos(args[2]));
            if (updated != state) {
                LevelWriterProxy.INSTANCE.setBlock(args[1], args[2], updated.customBlockState().minecraftState(), 3);
            }
        });
    }

    private SofaShape getSofaShape(ImmutableBlockState state, Object level, BlockPos pos) {
        Direction direction = state.get(this.facingProperty);
        Object relativeBlockState = BlockGetterProxy.INSTANCE.getBlockState(level, LocationUtils.toBlockPos(pos.relative(direction.opposite())));
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(relativeBlockState);
        if (optionalCustomState.isPresent()) {
            ImmutableBlockState customState = optionalCustomState.get();
            SofaBlockBehavior sofaBlockBehavior = customState.behavior().getFirst(SofaBlockBehavior.class);
            if (sofaBlockBehavior != null) {
                Direction direction1 = customState.get(sofaBlockBehavior.facingProperty);
                if (direction1.axis() != state.get(this.facingProperty).axis() && canTakeShape(state, level, pos, direction1)) {
                    if (direction1 == direction.counterClockWise()) {
                        return SofaShape.INNER_LEFT;
                    }
                    return SofaShape.INNER_RIGHT;
                }
            }
        }
        return SofaShape.STRAIGHT;
    }

    private boolean canTakeShape(ImmutableBlockState state, Object level, BlockPos pos, Direction face) {
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, LocationUtils.toBlockPos(pos.relative(face)));
        Optional<ImmutableBlockState> optionalAnotherState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalAnotherState.isEmpty()) {
            return true;
        }
        ImmutableBlockState anotherState = optionalAnotherState.get();
        SofaBlockBehavior anotherBehavior = anotherState.behavior().getFirst(SofaBlockBehavior.class);
        if (anotherBehavior == null) {
            return true;
        }
        return anotherState.get(anotherBehavior.facingProperty) != state.get(this.facingProperty);
    }

    private static class Factory implements BlockBehaviorFactory<SofaBlockBehavior> {

        @Override
        public SofaBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SofaBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "shape", SofaShape.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "waterlogged", Boolean.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "left", Boolean.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "right", Boolean.class),
                    section.getBoolean("face_player", false)
            );
        }
    }
}
