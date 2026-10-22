package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.multiblock.MultiblockRemovalReservations;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.*;

/** Native cell layout. Part and facing encode the anchor without an auxiliary registry. */
public final class MultiblockBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<MultiblockBlockBehavior> FACTORY = new Factory();
    private static final MultiblockRemovalReservations PENDING = new MultiblockRemovalReservations();
    private static final ThreadLocal<Set<String>> REMOVING = ThreadLocal.withInitial(HashSet::new);
    public final Property<Integer> partProperty;
    public final Property<Direction> facingProperty;
    private final List<BlockPos> offsets;

    private MultiblockBlockBehavior(BlockDefinition block, Property<Integer> part,
                                   Property<Direction> facing, List<BlockPos> offsets) {
        super(block); this.partProperty=part; this.facingProperty=facing; this.offsets=List.copyOf(offsets);
        if(offsets.isEmpty() || offsets.size()>64 || !offsets.getFirst().equals(new BlockPos(0,0,0))
                || new HashSet<>(offsets).size()!=offsets.size()) throw new IllegalArgumentException("Multiblock needs 1..64 unique pieces with anchor 0,0,0 first");
        for(BlockPos offset:offsets) if(Math.abs(offset.x())>16 || Math.abs(offset.z())>16 || offset.y()<0 || offset.y()>31)
            throw new IllegalArgumentException("Multiblock offsets exceed supported local bounds");
        if(part.possibleValues().size()!=offsets.size()) throw new IllegalArgumentException("part property must enumerate every piece");
        for(int i=0;i<offsets.size();i++) if(!part.possibleValues().contains(i)) throw new IllegalArgumentException("part property must start at zero");
        if(facing.possibleValues().size()!=4 || facing.possibleValues().stream().anyMatch(d->!d.axis().isHorizontal()))
            throw new IllegalArgumentException("Multiblock facing must be horizontal");
    }
    public int pieceCount(){return this.offsets.size();}
    public BlockPos piecePosition(BlockPos anchor, Direction face, int part) {
        BlockPos offset=this.offsets.get(part);int x=offset.x(),z=offset.z();
        return switch(face){case NORTH->anchor.offset(x,offset.y(),z);case EAST->anchor.offset(-z,offset.y(),x);
            case SOUTH->anchor.offset(-x,offset.y(),-z);case WEST->anchor.offset(z,offset.y(),-x);default->throw new IllegalArgumentException("Horizontal facing required");};
    }
    public ImmutableBlockState pieceState(ImmutableBlockState base,int part){return base.with(this.partProperty,part);}
    public boolean hasFreeSpace(World world,BlockPos anchor,ImmutableBlockState state) {
        if(state.get(this.partProperty)!=0 || PENDING.isReserved(world.getUID()+":"+anchor.asLong()))return false;
        // Check ownership and bounds for the entire footprint BEFORE reading any cells.
        for(int i=0;i<pieceCount();i++) {
            BlockPos p=piecePosition(anchor,state.get(this.facingProperty),i);
            if(p.y()<world.getMinHeight() || p.y()>=world.getMaxHeight()
                    || !world.isChunkLoaded(p.x()>>4,p.z()>>4)
                    || !Bukkit.isOwnedByCurrentRegion(world,p.x()>>4,p.z()>>4)
                    || !world.getWorldBorder().isInside(new Location(world,p.x()+.5,p.y(),p.z()+.5)))return false;
        }
        for(int i=0;i<pieceCount();i++) {
            BlockPos p=piecePosition(anchor,state.get(this.facingProperty),i);
            if(!world.getBlockAt(p.x(),p.y(),p.z()).getType().isAir())return false;
        }
        return true;
    }
    @Override public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context,ImmutableBlockState state) {
        ImmutableBlockState base=state.with(this.partProperty,0).with(this.facingProperty,context.getHorizontalDirection().opposite());
        World world=(World)context.getLevel().platformWorld();BlockPos anchor=context.getClickedPos();
        if(!hasFreeSpace(world,anchor,base))return null;
        if(context.getPlayer()!=null) {
            org.bukkit.entity.Player player=(org.bukkit.entity.Player)context.getPlayer().platformPlayer();
            for(int i=0;i<pieceCount();i++) {
                BlockPos p=piecePosition(anchor,base.get(this.facingProperty),i);
                if(!BukkitCraftEngine.instance().antiGriefProvider().test(player,Flag.PLACE,new Location(world,p.x(),p.y(),p.z())))return null;
            }
        }
        return base;
    }
    @Override public void affectNeighborsAfterRemoval(Object thisBlock,Object[] args) {
        ImmutableBlockState old=BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if(old==null)return;
        World world=LevelProxy.INSTANCE.getWorld(args[1]);BlockPos removed=LocationUtils.fromBlockPos(args[2]);
        Direction face=old.get(this.facingProperty);int index=old.get(this.partProperty);
        BlockPos rotated=piecePosition(new BlockPos(0,0,0),face,index);
        BlockPos anchor=removed.offset(-rotated.x(),-rotated.y(),-rotated.z());
        String key=world.getUID()+":"+anchor.asLong();
        if(REMOVING.get().contains(key))return;
        ImmutableBlockState current=CraftEngineBlocks.getCustomBlockState(world.getBlockAt(removed.x(),removed.y(),removed.z()));
        if(matches(current,old,index))return; // property refresh rather than a removed piece
        var ticket=PENDING.begin(key,pieceCount());
        if(ticket==null)return;
        var shelves=BukkitCraftEngine.instance().shelfManager();
        try {if(shelves!=null)shelves.remove(world,anchor);}
        catch(RuntimeException failure){for(int part=0;part<pieceCount();part++)ticket.completePart(part);throw failure;}
        var plugin=BukkitCraftEngine.instance().javaPlugin();
        for(int i=0;i<pieceCount();i++) {
            final int part=i;BlockPos target=piecePosition(anchor,face,i);
            Runnable remove=()->{
                Set<String> active=REMOVING.get();if(!active.add(key)){ticket.completePart(part);return;}
                try {
                    ImmutableBlockState found=CraftEngineBlocks.getCustomBlockState(world.getBlockAt(target.x(),target.y(),target.z()));
                    if(matches(found,old,part)) {
                        LevelWriterProxy.INSTANCE.setBlock(CraftWorldProxy.INSTANCE.getWorld(world),LocationUtils.toBlockPos(target),BlocksProxy.AIR$defaultState,
                                UpdateFlags.UPDATE_ALL | UpdateFlags.UPDATE_SUPPRESS_DROPS);
                    }
                }finally{active.remove(key);if(active.isEmpty())REMOVING.remove();ticket.completePart(part);}
            };
            if(world.isChunkLoaded(target.x()>>4,target.z()>>4) && Bukkit.isOwnedByCurrentRegion(world,target.x()>>4,target.z()>>4))remove.run();
            else {
                try {
                    world.getChunkAtAsync(target.x()>>4,target.z()>>4).thenAccept(chunk->
                            Bukkit.getRegionScheduler().execute(plugin,world,target.x()>>4,target.z()>>4,remove))
                            .exceptionally(error->{ticket.completePart(part);plugin.getLogger().warning("Multiblock cleanup failed: "+error);return null;});
                } catch(RuntimeException error) {
                    ticket.completePart(part);
                    plugin.getLogger().warning("Multiblock cleanup could not be scheduled: "+error);
                }
            }
        }
    }
    private boolean matches(ImmutableBlockState state,ImmutableBlockState original,int index) {
        return state!=null && state.owner().value()==original.owner().value()
                && state.get(this.partProperty)==index && state.get(this.facingProperty)==original.get(this.facingProperty);
    }
    private static final class Factory implements BlockBehaviorFactory<MultiblockBlockBehavior> {
        @Override public MultiblockBlockBehavior create(BlockDefinition block,ConfigSection section) {
            List<BlockPos> pieces=section.getNonNullValue("pieces",ConfigConstants.ARGUMENT_LIST).getAsNonEmptyList(value->{
                List<Integer> v=value.getAsFixedSizeList(3,ConfigValue::getAsInt);return new BlockPos(v.get(0),v.get(1),v.get(2));
            });
            return new MultiblockBlockBehavior(block,BlockBehaviorFactory.getProperty(section.path(),block,"part",Integer.class),
                    BlockBehaviorFactory.getProperty(section.path(),block,"facing",Direction.class),pieces);
        }
    }
}
