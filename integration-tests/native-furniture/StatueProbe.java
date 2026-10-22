package studio.virgoland.vsrprobe;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.entity.Item;
import net.momirealms.craftengine.bukkit.api.*;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.entity.player.InteractionHand;

public final class StatueProbe extends JavaPlugin implements Listener {
    private static final Key ID=Key.of("vsrender","demo_statue_giant");
    private static final int[][] PARTS={{0,0,0},{-1,0,-1},{0,0,-1},{1,0,-1},{-1,0,0},{1,0,0},{-1,0,1},{0,0,1},{1,0,1},{0,1,0},{0,2,0},{-1,3,0},{0,3,0},{1,3,0},{0,4,0}};
    private boolean denyHead;
    @EventHandler public void build(BlockCanBuildEvent e) {if(denyHead && e.getBlock().getY()==114)e.setBuildable(false);}
    @Override public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this,this);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            if(CraftEngineBlocks.byId(Key.of("vsrender","demo_table"))==null || Bukkit.getWorlds().isEmpty())return;
            task.cancel();var world=Bukkit.getWorlds().getFirst();
            world.getChunkAtAsync(0,0).thenAccept(chunk -> Bukkit.getRegionScheduler().run(this,world,0,0,t -> {
                try {
                    var definition=CraftEngineBlocks.byId(ID);var item=CraftEngineItems.byId(ID);
                    if(definition==null || item==null)throw new IllegalStateException("VSR_STATUE_MISSING");
                    var behavior=item.behavior().getFirst(BlockItemBehavior.class);
                    if(behavior==null)throw new IllegalStateException("statue item behavior missing");
                    var facing=(Property<Direction>)definition.getProperty("facing");var part=(Property<Integer>)definition.getProperty("part");
                    var anchor=new BlockPos(8,110,8);
                    if(count(world,anchor)==15)getLogger().info("VSR_STATUE_RESTART_PASS");
                    clear(world,anchor);clear(world,new BlockPos(12,110,8));
                    int entities=(int)java.util.Arrays.stream(chunk.getEntities()).filter(e->e.isValid()).count();
                    world.getBlockAt(8,114,8).setType(Material.STONE);
                    if(behavior.place(context(world,anchor,Direction.NORTH)).success())throw new IllegalStateException("obstructed statue accepted");
                    if(count(world,anchor)!=0 || world.getBlockAt(8,114,8).getType()!=Material.STONE)throw new IllegalStateException("obstruction overwritten or partial placement");
                    world.getBlockAt(8,114,8).setType(Material.AIR);
                    denyHead=true;
                    if(behavior.place(context(world,anchor,Direction.NORTH)).success() || count(world,anchor)!=0)throw new IllegalStateException("per-piece build veto ignored");
                    denyHead=false;
                    var high=new BlockPos(8,world.getMaxHeight()-2,8);
                    if(behavior.place(context(world,high,Direction.NORTH)).success())throw new IllegalStateException("height overflow accepted");
                    for(var look:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}) {
                        if(!behavior.place(context(world,anchor,look)).success())throw new IllegalStateException("placement failed: "+look);
                        if(count(world,anchor)!=15)throw new IllegalStateException("piece count mismatch: "+count(world,anchor));
                        for(int i=0;i<PARTS.length;i++) {
                            var pos=pos(anchor,look.opposite(),PARTS[i]);var block=world.getBlockAt(pos.x(),pos.y(),pos.z());var state=CraftEngineBlocks.getCustomBlockState(block);
                            if(state==null || !state.owner().value().id().equals(ID) || state.get(part)!=i || state.get(facing)!=look.opposite())throw new IllegalStateException("wrong part/rotation "+i+" "+look);
                            if(block.getCollisionShape().getBoundingBoxes().isEmpty())throw new IllegalStateException("part collision missing");
                        }
                        CraftEngineBlocks.remove(world.getBlockAt(8,114,8));
                        if(count(world,anchor)!=0)throw new IllegalStateException("head removal left orphan pieces");
                    }
                    var other=new BlockPos(12,110,8);
                    behavior.place(context(world,anchor,Direction.NORTH));behavior.place(context(world,other,Direction.EAST));
                    CraftEngineBlocks.remove(world.getBlockAt(8,110,8));
                    if(count(world,anchor)!=0 || count(world,other)!=15)throw new IllegalStateException("neighbor statue damaged");
                    CraftEngineBlocks.remove(world.getBlockAt(12,114,8),null,false,true,false);
                    if(count(world,other)!=0)throw new IllegalStateException("drop removal left pieces");
                    var drops=java.util.Arrays.stream(chunk.getEntities()).filter(e->e instanceof Item).map(e->(Item)e).filter(e->ID.equals(CraftEngineItems.getCustomItemId(e.getItemStack()))).toList();
                    if(drops.size()!=1 || drops.getFirst().getItemStack().getAmount()!=1)throw new IllegalStateException("expected one drop, got "+drops.size());
                    drops.forEach(Item::remove);
                    behavior.place(context(world,anchor,Direction.NORTH));
                    world.getBlockAt(8,114,8).setType(Material.STONE);
                    if(count(world,anchor)!=0 || world.getBlockAt(8,114,8).getType()!=Material.STONE)throw new IllegalStateException("replacement destroyed or orphaned");
                    world.getBlockAt(8,114,8).setType(Material.AIR);
                    behavior.place(context(world,anchor,Direction.NORTH));
                    if(count(world,anchor)!=15)throw new IllegalStateException("persistent statue missing");
                    if(java.util.Arrays.stream(chunk.getEntities()).filter(e->e.isValid()).count()!=entities)throw new IllegalStateException("entity spawned for statue");
                    getLogger().info("VSR_STATUE_PASS: 15 pieces, rotations, obstruction, build veto, height, removal, neighbor isolation, single drop, persistence candidate");
                }catch(Throwable ex){getLogger().log(java.util.logging.Level.SEVERE,"VSR_STATUE_FAIL",ex);}
            }));
        },20,20);
    }
    private static BlockPlaceContext context(org.bukkit.World world,BlockPos pos,Direction look) {
        var hit=new BlockHitResult(new Vec3d(pos.x()+.5,pos.y(),pos.z()+.5),Direction.UP,pos,false);
        return new BlockPlaceContext(BukkitAdaptor.adapt(world),null,InteractionHand.MAIN_HAND,BukkitAdaptor.adapt(new org.bukkit.inventory.ItemStack(Material.PAPER)),hit){@Override public Direction getHorizontalDirection(){return look;}};
    }
    private static BlockPos pos(BlockPos anchor,Direction face,int[] p) {
        int x=p[0],z=p[2];return switch(face){case NORTH->anchor.offset(x,p[1],z);case EAST->anchor.offset(-z,p[1],x);case SOUTH->anchor.offset(-x,p[1],-z);case WEST->anchor.offset(z,p[1],-x);default->throw new IllegalArgumentException();};
    }
    private static int count(org.bukkit.World world,BlockPos a) {int n=0;for(int x=-1;x<=1;x++)for(int y=0;y<5;y++)for(int z=-1;z<=1;z++){var s=CraftEngineBlocks.getCustomBlockState(world.getBlockAt(a.x()+x,a.y()+y,a.z()+z));if(s!=null && s.owner().value().id().equals(ID))n++;}return n;}
    private static void clear(org.bukkit.World world,BlockPos a) {for(int x=-1;x<=1;x++)for(int y=0;y<5;y++)for(int z=-1;z<=1;z++)world.getBlockAt(a.x()+x,a.y()+y,a.z()+z).setType(Material.AIR,false);}
}
