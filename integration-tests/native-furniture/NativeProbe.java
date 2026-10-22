package studio.virgoland.vsrprobe;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.util.Key;

public final class NativeProbe extends JavaPlugin {
    @Override public void onEnable() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            var definition = CraftEngineBlocks.byId(Key.of("vsrender", "demo_table"));
            if (definition == null || Bukkit.getWorlds().isEmpty()) return;
            task.cancel();
            var world = Bukkit.getWorlds().getFirst();
            world.getChunkAtAsync(0, 0).thenAccept(chunk -> Bukkit.getRegionScheduler().run(this, world, 0, 0, t -> {
                try {
                    var location = new Location(world, 4, 100, 4);
                    var block = location.getBlock();
                    if (CraftEngineBlocks.getCustomBlockState(block) == definition.defaultState()) {
                        getLogger().info("VSR_NATIVE_RESTART_PASS");
                    }
                    if (!definition.defaultState().visualBlockState().toString().contains("leaves")) throw new IllegalStateException("VSR_FLOOR_OPAQUE_CARRIER: " + definition.defaultState().visualBlockState());
                    block.setType(Material.AIR, false);
                    int before = chunk.getEntities().length;
                    if (!CraftEngineBlocks.place(location, definition.defaultState(), false)) throw new IllegalStateException("place failed");
                    if (CraftEngineBlocks.getCustomBlockState(block) != definition.defaultState()) throw new IllegalStateException("identity mismatch");
                    if (block.getCollisionShape().getBoundingBoxes().isEmpty()) throw new IllegalStateException("no collision");
                    if (chunk.getEntities().length != before) throw new IllegalStateException("entity added for native block");
                    if (!CraftEngineBlocks.remove(block) || !block.getType().isAir()) throw new IllegalStateException("cleanup failed");
                    if (!CraftEngineBlocks.place(location, definition.defaultState(), false)) throw new IllegalStateException("persistent placement failed");
                    
                    var sofa = CraftEngineBlocks.byId(Key.of("vsrender", "demo_sofa"));
                    if (sofa == null) throw new IllegalStateException("sofa missing");
                    var facing = (net.momirealms.craftengine.core.block.property.Property<net.momirealms.craftengine.core.util.Direction>) sofa.getProperty("facing");
                    var left = (net.momirealms.craftengine.core.block.property.Property<Boolean>) sofa.getProperty("left");
                    var right = (net.momirealms.craftengine.core.block.property.Property<Boolean>) sofa.getProperty("right");
                    var shape = (net.momirealms.craftengine.core.block.property.Property<net.momirealms.craftengine.core.block.property.type.SofaShape>) sofa.getProperty("shape");
                    
                    // Exercise real placement-state resolution for all horizontal look directions.
                    var ceWorld = net.momirealms.craftengine.bukkit.api.BukkitAdaptor.adapt(world);
                    var stack = net.momirealms.craftengine.bukkit.api.BukkitAdaptor.adapt(new org.bukkit.inventory.ItemStack(Material.PAPER));
                    var hit = new net.momirealms.craftengine.core.world.BlockHitResult(
                        new net.momirealms.craftengine.core.world.Vec3d(12.5,100,12.5),
                        net.momirealms.craftengine.core.util.Direction.UP,
                        new net.momirealms.craftengine.core.world.BlockPos(12,100,12),false);
                    new Location(world,12,100,12).getBlock().setType(Material.AIR,false);
                    for (var look : new net.momirealms.craftengine.core.util.Direction[]{
                        net.momirealms.craftengine.core.util.Direction.NORTH,
                        net.momirealms.craftengine.core.util.Direction.EAST,
                        net.momirealms.craftengine.core.util.Direction.SOUTH,
                        net.momirealms.craftengine.core.util.Direction.WEST}) {
                        var context = new net.momirealms.craftengine.core.world.context.BlockPlaceContext(
                            ceWorld,null,net.momirealms.craftengine.core.entity.player.InteractionHand.MAIN_HAND,stack,hit) {
                                @Override public net.momirealms.craftengine.core.util.Direction getHorizontalDirection() {return look;}
                            };
                        var legacy = CraftEngineBlocks.byId(Key.of("default","sofa"));
                        if(legacy==null) throw new IllegalStateException("legacy sofa missing from fixture");
                        var legacyFacing = (net.momirealms.craftengine.core.block.property.Property<net.momirealms.craftengine.core.util.Direction>) legacy.getProperty("facing");
                        if(legacy.getStateForPlacement(context).get(legacyFacing)!=look) throw new IllegalStateException("legacy placement changed");
                        var placed = sofa.getStateForPlacement(context);
                        if (placed.get(facing)!=look.opposite()) throw new IllegalStateException("VSR_SOFA_FACING_REVERSED: look="+look+", sofa="+placed.get(facing));
                        var target = new Location(world,12,100,12);
                        CraftEngineBlocks.place(target,placed,false);
                        if(CraftEngineBlocks.getCustomBlockState(target.getBlock()).get(facing)!=look.opposite()) throw new IllegalStateException("placed facing mismatch");
                        CraftEngineBlocks.remove(target.getBlock());
                    }
                    getLogger().info("VSR_SOFA_FACING_PASS: north east south west");

                    var north = sofa.defaultState().with(facing, net.momirealms.craftengine.core.util.Direction.NORTH);
                    var a = new Location(world, 10,100,4); var b = new Location(world,11,100,4); var corner = new Location(world,10,100,5);
                    a.getBlock().setType(Material.AIR); b.getBlock().setType(Material.AIR); corner.getBlock().setType(Material.AIR);
                    CraftEngineBlocks.place(a,north,false); CraftEngineBlocks.place(b,north,false);
                    if (!CraftEngineBlocks.getCustomBlockState(a.getBlock()).get(right) || !CraftEngineBlocks.getCustomBlockState(b.getBlock()).get(left)) throw new IllegalStateException("VSR_SOFA_LATERAL_MISSING");
                    CraftEngineBlocks.remove(b.getBlock());
                    if (CraftEngineBlocks.getCustomBlockState(a.getBlock()).get(right)) throw new IllegalStateException("sofa end did not reset");
                    CraftEngineBlocks.place(corner,north.with(facing,net.momirealms.craftengine.core.util.Direction.WEST),false);
                    if (CraftEngineBlocks.getCustomBlockState(a.getBlock()).get(shape) != net.momirealms.craftengine.core.block.property.type.SofaShape.INNER_LEFT) throw new IllegalStateException("corner missing");
                    CraftEngineBlocks.remove(corner.getBlock());
                    if (CraftEngineBlocks.getCustomBlockState(a.getBlock()).get(shape) != net.momirealms.craftengine.core.block.property.type.SofaShape.STRAIGHT) throw new IllegalStateException("corner did not reset");
                    CraftEngineBlocks.remove(a.getBlock());
                    for (String id : new String[]{"demo_chair","demo_lamp"}) {
                        var demo = CraftEngineBlocks.byId(Key.of("vsrender",id));
                        if (demo == null) throw new IllegalStateException(id+" missing");
                        var l = new Location(world,7,100,4);
                        CraftEngineBlocks.place(l,demo.defaultState(),false);
                        if (l.getBlock().getCollisionShape().getBoundingBoxes().isEmpty()) throw new IllegalStateException(id+" collision missing");
                        if (id.equals("demo_lamp")) {
                            var lit = (net.momirealms.craftengine.core.block.property.Property<Boolean>) demo.getProperty("lit");
                            var on = demo.defaultState().with(lit,true);
                            if (on.settings().luminance()!=15 || demo.defaultState().settings().luminance()!=0) throw new IllegalStateException("lamp light settings");
                            CraftEngineBlocks.place(l,on,false);
                            continue; // Chunk lighting propagates asynchronously; verify after ticks.
                        }
                        CraftEngineBlocks.remove(l.getBlock());
                    }
                    if(chunk.getEntities().length!=before) throw new IllegalStateException("furniture spawned entity");
                    Bukkit.getRegionScheduler().runDelayed(this, world, 0, 0, lightTask -> {
                        try {
                            var l = new Location(world,7,100,4);
                            if(l.getBlock().getLightFromBlocks()!=15) throw new IllegalStateException("lamp native light missing after propagation: "+l.getBlock().getLightFromBlocks());
                            var lamp = CraftEngineBlocks.byId(Key.of("vsrender","demo_lamp"));
                            CraftEngineBlocks.place(l,lamp.defaultState(),false);
                            Bukkit.getRegionScheduler().runDelayed(this,world,0,0,offTask -> {
                                try {
                                    if(l.getBlock().getLightFromBlocks()!=0) throw new IllegalStateException("lamp native light did not turn off");
                                    CraftEngineBlocks.remove(l.getBlock());
                                    getLogger().info("VSR_FURNITURE_PASS");
                                    getLogger().info("VSR_NATIVE_PASS");
                                } catch(Throwable ex) {getLogger().log(java.util.logging.Level.SEVERE,"VSR_NATIVE_FAIL",ex);}
                            },20);
                        } catch(Throwable ex) {getLogger().log(java.util.logging.Level.SEVERE,"VSR_NATIVE_FAIL",ex);}
                    },20);
                } catch (Throwable ex) { getLogger().log(java.util.logging.Level.SEVERE, "VSR_NATIVE_FAIL", ex); }
            }));
        }, 20, 20);
    }
}
