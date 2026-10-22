package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.block.behavior.MultiblockBlockBehavior;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.*;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import java.nio.file.Path;
import java.util.*;

/** Placement snapshots include every cell, so normal Bukkit cancellation restores the whole group. */
public final class MultiblockBlockItemBehavior extends BlockItemBehavior {
    public static final ItemBehaviorFactory<MultiblockBlockItemBehavior> FACTORY=new Factory();
    private MultiblockBlockItemBehavior(Key id){super(id);}
    @Override protected boolean canPlace(BlockPlaceContext context,ImmutableBlockState state) {
        var behavior=state.behavior().getFirst(MultiblockBlockBehavior.class);
        if(behavior==null)return false;
        BlockPos anchor=context.getClickedPos();World world=(World)context.getLevel().platformWorld();
        if(!behavior.hasFreeSpace(world,anchor,state))return false;
        for(int i=0;i<behavior.pieceCount();i++) {
            BlockPos pos=behavior.piecePosition(anchor,state.get(behavior.facingProperty),i);
            BlockPlaceContext partContext=new BlockPlaceContext(context.getLevel(),context.getPlayer(),context.getHand(),context.getItem(),context.getHitResult().withPosition(pos)) {
                @Override public BlockPos getClickedPos(){return pos;}
            };
            if(!super.canPlace(partContext,behavior.pieceState(state,i)))return false;
        }
        return true;
    }
    @Override protected boolean placeBlock(Location location,ImmutableBlockState state,List<BlockState> revertStates) {
        var behavior=state.behavior().getFirst(MultiblockBlockBehavior.class);
        if(behavior==null)return false;
        World world=location.getWorld();BlockPos anchor=new BlockPos(location.getBlockX(),location.getBlockY(),location.getBlockZ());
        if(!behavior.hasFreeSpace(world,anchor,state))return false;
        List<Location> cells=new ArrayList<>();
        for(int i=0;i<behavior.pieceCount();i++) {
            BlockPos pos=behavior.piecePosition(anchor,state.get(behavior.facingProperty),i);
            Location cell=new Location(world,pos.x(),pos.y(),pos.z());cells.add(cell);
            if(i!=0)revertStates.add(cell.getBlock().getState());
        }
        try {
            for(int i=0;i<cells.size();i++) {
                if(!CraftEngineBlocks.place(cells.get(i),behavior.pieceState(state,i),UpdateFlags.UPDATE_CLIENTS | UpdateFlags.UPDATE_KNOWN_SHAPE | UpdateFlags.UPDATE_SKIP_ON_PLACE,false)) {
                    revertStates.forEach(snapshot->snapshot.update(true,false));return false;
                }
            }
            return true;
        }catch(RuntimeException error){revertStates.forEach(snapshot->snapshot.update(true,false));throw error;}
    }
    private static final class Factory implements ItemBehaviorFactory<MultiblockBlockItemBehavior> {
        @Override public MultiblockBlockItemBehavior create(Pack pack,Path path,Key id,ConfigSection section) {
            ConfigValue block=section.getNonNullValue("block",ConfigConstants.ARGUMENT_SECTION);
            if(block.is(Map.class)) {
                BukkitBlockManager.instance().blockParser().addPendingConfigSection(new PendingConfigSection(pack,path,id,block.getAsSection()));
                return new MultiblockBlockItemBehavior(id);
            }
            return new MultiblockBlockItemBehavior(block.getAsIdentifier());
        }
    }
}
