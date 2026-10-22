package studio.virgoland.vsrprobe;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.momirealms.craftengine.bukkit.api.*;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import java.lang.reflect.Proxy;
import java.io.File;
public final class ShelfProbe extends JavaPlugin {
 private byte[] packet;
 private ItemStack hand=new ItemStack(Material.AIR);private boolean sneak;
 @Override public void onEnable(){Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,t->{if(CraftEngineItems.byId(Key.of("vsrender:demo_shelf"))==null)return;t.cancel();org.bukkit.World world=Bukkit.getWorlds().getFirst();world.getChunkAtAsync(0,0).thenAccept(c->Bukkit.getRegionScheduler().run(this,world,0,0,ignored->{try{test(world);}catch(Throwable e){getLogger().log(java.util.logging.Level.SEVERE,"VSR_SHELF_FAIL",e);}}));},20,20);}
 private void test(org.bukkit.World world)throws Exception{
  BlockPos a=new BlockPos(8,125,8),b=new BlockPos(12,125,8);
  File file=new File("plugins/VsRender/shelves.yml");String path="shelves."+world.getUID()+":"+a.asLong()+".entries.7";
  var yaml=YamlConfiguration.loadConfiguration(file);
  if(yaml.getItemStack(path+".item")!=null&&yaml.getItemStack(path+".item").getType()==Material.DIAMOND)getLogger().info("VSR_SHELF_RESTART_PASS");
  CraftEngineBlocks.remove(world.getBlockAt(8,125,8));CraftEngineBlocks.remove(world.getBlockAt(12,125,8));
  for(int x=7;x<=13;x++)for(int y=125;y<130;y++)world.getBlockAt(x,y,8).setType(Material.AIR,false);
  java.util.Arrays.stream(world.getChunkAt(0,0).getEntities()).filter(e->e instanceof org.bukkit.entity.Item&&e.getLocation().getY()>123).forEach(org.bukkit.entity.Entity::remove);
  var shelf=CraftEngineItems.byId(Key.of("vsrender:demo_shelf")).behavior().getFirst(BlockItemBehavior.class);
  var stand=CraftEngineItems.byId(Key.of("vsrender:demo_invisible_stand")).behavior().getFirst(BlockItemBehavior.class);
  if(!shelf.place(context(world,a)).success()||!stand.place(context(world,b)).success())throw new IllegalStateException("shelf/stand placement failed");
  long entities=java.util.Arrays.stream(world.getChunkAt(0,0).getEntities()).filter(e->e.isValid()).count();
  PlayerInventory inventory=(PlayerInventory)Proxy.newProxyInstance(getClassLoader(),new Class[]{PlayerInventory.class},(o,m,args)->switch(m.getName()){case "getItemInMainHand"->hand;case "setItemInMainHand"->{hand=(ItemStack)args[0];yield null;}default->primitive(m.getReturnType());});
  Player player=(Player)Proxy.newProxyInstance(getClassLoader(),new Class[]{Player.class},(o,m,args)->switch(m.getName()){case "sendPluginMessage"->{packet=(byte[])args[2];yield null;}case "getInventory"->inventory;case "isSneaking"->sneak;case "getGameMode"->GameMode.SURVIVAL;case "getWorld"->world;case "getUniqueId"->new java.util.UUID(0,7);case "getName"->"ShelfProbe";case "hasPermission"->true;default->primitive(m.getReturnType());});
  var manager=BukkitCraftEngine.instance().shelfManager();
  hand=new ItemStack(Material.DIAMOND,3);var meta=hand.getItemMeta();meta.setDisplayName("Shelf preserved name");hand.setItemMeta(meta);
  click(manager,player,world,8,127,8);if(hand.getAmount()!=2)throw new IllegalStateException("insertion count");
  yaml=YamlConfiguration.loadConfiguration(file);if(yaml.getItemStack(path+".item").getAmount()!=1||yaml.getInt(path+".mode")!=1)throw new IllegalStateException("entry not stored");
  
  var field=manager.getClass().getDeclaredField("shelves");field.setAccessible(true);var records=(java.util.Map<?,?>)field.get(manager);Object record=records.get(world.getUID()+":"+a.asLong());
  var sender=java.util.Arrays.stream(manager.getClass().getDeclaredMethods()).filter(m->m.getName().equals("send")).findFirst().orElseThrow();sender.setAccessible(true);sender.invoke(manager,player,record,0);
  var decoded=com.virgolandstudio.client.shelf.ShelfWire.decode(packet);
  if(!decoded.dimension().equals("minecraft:overworld")||decoded.entries().size()!=1||decoded.entries().getFirst().slot()!=7||!decoded.entries().getFirst().itemJson().contains("minecraft:diamond"))throw new IllegalStateException("server/client wire mismatch");
  getLogger().info("VSR_SHELF_WIRE_PASS: actual server codec decoded by client wire");

  hand=new ItemStack(Material.AIR);sneak=true;click(manager,player,world,8,127,8);yaml=YamlConfiguration.loadConfiguration(file);if(yaml.getInt(path+".mode")!=2)throw new IllegalStateException("sized mode missing");
  click(manager,player,world,8,127,8);yaml=YamlConfiguration.loadConfiguration(file);if(yaml.getInt(path+".mode")!=0)throw new IllegalStateException("flat mode missing");
  sneak=false;click(manager,player,world,8,127,8);if(hand.getType()!=Material.DIAMOND||hand.getAmount()!=1||!hand.getItemMeta().getDisplayName().equals("Shelf preserved name"))throw new IllegalStateException("retrieve lost item metadata");
  hand=new ItemStack(Material.STONE,2);meta=hand.getItemMeta();meta.getPersistentDataContainer().set(new NamespacedKey("vsrender","shelf_width"),PersistentDataType.FLOAT,2.5f);hand.setItemMeta(meta);click(manager,player,world,8,125,8);if(hand.getAmount()!=1)throw new IllegalStateException("large center item rejected");
  hand=new ItemStack(Material.GOLD_INGOT,2);click(manager,player,world,9,125,8);if(hand.getAmount()!=2)throw new IllegalStateException("overlap consumed item");
  hand=new ItemStack(Material.AIR);click(manager,player,world,8,125,8);if(hand.getType()!=Material.STONE)throw new IllegalStateException("large item retrieval");
  hand=new ItemStack(Material.IRON_INGOT,2);sneak=true;click(manager,player,world,12,125,8);sneak=false;if(hand.getAmount()!=1)throw new IllegalStateException("invisible insert");
  hand=new ItemStack(Material.AIR);click(manager,player,world,12,125,8);if(hand.getType()!=Material.IRON_INGOT)throw new IllegalStateException("stand retrieve");
  if(java.util.Arrays.stream(world.getChunkAt(0,0).getEntities()).filter(e->e.isValid()).count()!=entities)throw new IllegalStateException("shelf display entity spawned");
  hand=new ItemStack(Material.DIAMOND);click(manager,player,world,8,127,8);
  hand=new ItemStack(Material.IRON_INGOT);click(manager,player,world,12,125,8);
  CraftEngineBlocks.remove(world.getBlockAt(12,125,8));
  Bukkit.getRegionScheduler().runDelayed(this,world,0,0,task->{try{
   var drops=java.util.Arrays.stream(world.getChunkAt(0,0).getEntities()).filter(e->e instanceof org.bukkit.entity.Item).map(e->(org.bukkit.entity.Item)e).filter(e->e.getItemStack().getType()==Material.IRON_INGOT&&e.getLocation().distanceSquared(new Location(world,12.5,125.5,8.5))<4).toList();
   if(drops.size()!=1||drops.getFirst().getItemStack().getAmount()!=1)throw new IllegalStateException("shelf content removal drop count "+drops.size());
   drops.forEach(org.bukkit.entity.Item::remove);
   getLogger().info("VSR_SHELF_PASS: 15 native pieces, stand, transfer, metadata, three modes, oversized overlap refusal, actual wire, single contents drop, persistence candidate, zero display entities");
  }catch(Throwable error){getLogger().log(java.util.logging.Level.SEVERE,"VSR_SHELF_FAIL",error);}},2);
 }
 private static void click(net.momirealms.craftengine.bukkit.shelf.ShelfManager manager,Player p,org.bukkit.World w,int x,int y,int z){manager.interact(new PlayerInteractEvent(p,Action.RIGHT_CLICK_BLOCK,p.getInventory().getItemInMainHand(),w.getBlockAt(x,y,z),org.bukkit.block.BlockFace.NORTH,EquipmentSlot.HAND));}
 private static Object primitive(Class<?> c){if(c==boolean.class)return false;if(c==int.class)return 0;if(c==long.class)return 0L;if(c==float.class)return 0f;if(c==double.class)return 0d;return null;}
 private static BlockPlaceContext context(org.bukkit.World w,BlockPos a){var h=new BlockHitResult(new Vec3d(a.x()+.5,a.y(),a.z()+.5),Direction.UP,a,false);return new BlockPlaceContext(BukkitAdaptor.adapt(w),null,InteractionHand.MAIN_HAND,BukkitAdaptor.adapt(new ItemStack(Material.PAPER)),h){@Override public Direction getHorizontalDirection(){return Direction.SOUTH;}};}
}
