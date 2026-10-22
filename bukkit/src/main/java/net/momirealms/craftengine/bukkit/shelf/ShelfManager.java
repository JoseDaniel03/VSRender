package net.momirealms.craftengine.bukkit.shelf;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.block.behavior.MultiblockBlockBehavior;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.block.shelf.ShelfLayout;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Durable, entity-free shelf contents. Every world access occurs on its owning region. */
public final class ShelfManager implements Listener {
    public static final String STATE="vsrender:shelf_state", ACTION="vsrender:shelf_action";
    private static final NamespacedKey WIDTH=new NamespacedKey("vsrender","shelf_width");
    private final BukkitCraftEngine engine;
    private final File file;
    private final Map<String,Float> widths=new HashMap<>();
    private final Map<UUID,Long> resyncTimes=new ConcurrentHashMap<>();
    private final Map<String,Shelf> shelves=new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong revisions=new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
    private long nextRevision(){return revisions.incrementAndGet();}
    private record Entry(ItemStack item,int mode,float width) {}
    private static final class Shelf {
        UUID world; long anchor,revision; int facing; boolean invisible;
        final Map<Integer,Entry> entries=new TreeMap<>();
    }
    private record Target(Shelf shelf,int slot) {}
    public ShelfManager(BukkitCraftEngine engine) {this.engine=engine;this.file=new File(engine.javaPlugin().getDataFolder(),"shelves.yml");loadWidths();load();}
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this,engine.javaPlugin());
        Bukkit.getMessenger().registerOutgoingPluginChannel(engine.javaPlugin(),STATE);
        // Bukkit interaction is authoritative; action payloads never mutate world state.
        Bukkit.getMessenger().registerIncomingPluginChannel(engine.javaPlugin(),ACTION,(channel,player,data)->{if(data.length!=2||data[0]!=1||data[1]!=0)return;long now=System.currentTimeMillis();Long previous=resyncTimes.put(player.getUniqueId(),now);if(previous==null||now-previous>1000)sync(player);});
        Bukkit.getOnlinePlayers().forEach(this::sync);
    }
    public void disable(){save();HandlerList.unregisterAll(this);Bukkit.getMessenger().unregisterIncomingPluginChannel(engine.javaPlugin(),ACTION);Bukkit.getMessenger().unregisterOutgoingPluginChannel(engine.javaPlugin(),STATE);}
    private static String key(UUID world,long anchor){return world+":"+anchor;}
    private Target target(Block block) {
        ImmutableBlockState state=CraftEngineBlocks.getCustomBlockState(block);
        if(state==null)return null;
        MultiblockBlockBehavior multi=state.behavior().getFirst(MultiblockBlockBehavior.class);if(multi==null)return null;
        String id=state.owner().value().id().toString();
        boolean invisible=id.equals("vsrender:demo_invisible_stand");
        if(!invisible && !id.equals("vsrender:demo_shelf"))return null;
        Direction face=state.get(multi.facingProperty);
        BlockPos offset=multi.piecePosition(new BlockPos(0,0,0),face,state.get(multi.partProperty));
        BlockPos anchor=new BlockPos(block.getX()-offset.x(),block.getY()-offset.y(),block.getZ()-offset.z());
        int localX=switch(face){case NORTH->offset.x();case EAST->offset.z();case SOUTH->-offset.x();case WEST->-offset.z();default->0;};
        Shelf shelf=shelves.computeIfAbsent(key(block.getWorld().getUID(),anchor.asLong()),ignored->{Shelf s=new Shelf();s.world=block.getWorld().getUID();s.anchor=anchor.asLong();s.revision=nextRevision();s.invisible=invisible;s.facing=facing(face);return s;});
        shelf.facing=facing(face);shelf.invisible=invisible;
        return new Target(shelf,invisible?0:offset.y()*3+localX+1);
    }
    private static int facing(Direction d){return switch(d){case EAST->1;case SOUTH->2;case WEST->3;default->0;};}
    public synchronized void interact(PlayerInteractEvent event) {
        if(event.getPlayer().getGameMode()==GameMode.SPECTATOR)return;
        if(event.getAction()!=Action.RIGHT_CLICK_BLOCK || event.getHand()!=EquipmentSlot.HAND || event.getClickedBlock()==null)return;
        Target target=target(event.getClickedBlock());if(target==null)return;
        if(!engine.antiGriefProvider().test(event.getPlayer(),net.momirealms.antigrieflib.Flag.OPEN_CONTAINER,event.getClickedBlock().getLocation())){event.setCancelled(true);return;}
        event.setCancelled(true);Player player=event.getPlayer();Shelf shelf=target.shelf();int slot=target.slot();
        synchronized(shelf) {
            ItemStack held=player.getInventory().getItemInMainHand();ItemStack before=held.clone();Entry previous=shelf.entries.get(slot);long revisionBefore=shelf.revision;
            if(held.getType().isAir()) {
                if(previous==null)return;
                if(player.isSneaking())shelf.entries.put(slot,new Entry(previous.item(),(previous.mode()+1)%3,previous.width()));
                else {shelf.entries.remove(slot);player.getInventory().setItemInMainHand(previous.item().clone());}
            } else {
                if(previous!=null)return;
                Float configured=held.hasItemMeta()?held.getItemMeta().getPersistentDataContainer().get(WIDTH,PersistentDataType.FLOAT):null;
                String itemId=String.valueOf(net.momirealms.craftengine.bukkit.api.CraftEngineItems.getCustomItemId(held));
                float width=configured==null?widths.getOrDefault(itemId,widths.getOrDefault(held.getType().getKey().toString(),held.getType().isBlock()?.8f:.45f)):configured;
                if(!Float.isFinite(width)||width<.05f||width>3f||!ShelfLayout.fits(slot,width,shelf.invisible))return;
                for(var pair:shelf.entries.entrySet())if(ShelfLayout.overlaps(pair.getKey(),pair.getValue().width(),slot,width,shelf.invisible))return;
                ItemStack one=held.clone();one.setAmount(1);
                // Validate the complete client representation before consuming anything.
                try {json(one);}catch(RuntimeException ex){return;}
                shelf.entries.put(slot,new Entry(one,player.isSneaking()?0:1,width));held.setAmount(held.getAmount()-1);player.getInventory().setItemInMainHand(held);
            }
            shelf.revision=nextRevision();try{save();}catch(RuntimeException failure){shelf.revision=revisionBefore;if(previous==null)shelf.entries.remove(slot);else shelf.entries.put(slot,previous);player.getInventory().setItemInMainHand(before);engine.javaPlugin().getLogger().severe("Shelf transaction rolled back: "+failure.getMessage());return;}broadcast(shelf,0);
        }
    }
    public synchronized void remove(World world,BlockPos anchor) {
        Shelf shelf=shelves.remove(key(world.getUID(),anchor.asLong()));if(shelf==null)return;
        synchronized(shelf){long revisionBefore=shelf.revision;shelf.revision=nextRevision();try{save();}catch(RuntimeException failure){shelf.revision=revisionBefore;shelves.put(key(world.getUID(),anchor.asLong()),shelf);throw failure;}broadcast(shelf,1);List<ItemStack> drops=shelf.entries.values().stream().map(e->e.item().clone()).toList();shelf.entries.clear();
            Bukkit.getRegionScheduler().execute(engine.javaPlugin(),world,anchor.x()>>4,anchor.z()>>4,()->drops.forEach(item->world.dropItemNaturally(new Location(world,anchor.x()+.5,anchor.y()+.5,anchor.z()+.5),item)));}
    }
    @EventHandler public void quit(PlayerQuitEvent e){resyncTimes.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void join(PlayerJoinEvent e){sync(e.getPlayer());}
    @EventHandler public void change(PlayerChangedWorldEvent e){sync(e.getPlayer());}
    @EventHandler public void load(ChunkLoadEvent e){for(Shelf s:shelves.values())if(s.world.equals(e.getWorld().getUID()) && chunk(s,e.getChunk()))validate(e.getWorld(),s);}
    @EventHandler public synchronized void unload(ChunkUnloadEvent e){for(Shelf s:shelves.values())if(s.world.equals(e.getWorld().getUID()) && chunk(s,e.getChunk())){s.revision=nextRevision();broadcast(s,1);}save();}
    private boolean chunk(Shelf shelf,Chunk chunk){BlockPos p=BlockPos.of(shelf.anchor);return p.x()>>4==chunk.getX() && p.z()>>4==chunk.getZ();}
    private synchronized void validate(World world,Shelf shelf){BlockPos p=BlockPos.of(shelf.anchor);Target t=target(world.getBlockAt(p.x(),p.y(),p.z()));if(t==null)remove(world,p);else {shelf.facing=t.shelf().facing;shelf.invisible=t.shelf().invisible;shelf.revision=nextRevision();save();broadcast(shelf,0);}}
    private void sync(Player player){player.getScheduler().run(engine.javaPlugin(),task->{send(player,empty(player.getWorld()),2);for(Shelf s:shelves.values())if(s.world.equals(player.getWorld().getUID())){World world=player.getWorld();BlockPos p=BlockPos.of(s.anchor);Bukkit.getRegionScheduler().execute(engine.javaPlugin(),world,p.x()>>4,p.z()>>4,()->{if(world.isChunkLoaded(p.x()>>4,p.z()>>4))validate(world,s);});}},null);}
    private Shelf empty(World world){Shelf s=new Shelf();s.world=world.getUID();return s;}
    private void broadcast(Shelf shelf,int op){for(Player p:Bukkit.getOnlinePlayers())p.getScheduler().run(engine.javaPlugin(),task->{if(p.getWorld().getUID().equals(shelf.world))send(p,shelf,op);},null);}
    private static String json(ItemStack item){String result=ItemStackProxy.INSTANCE.getCodec().encodeStart(RegistryOps.JSON,CraftItemStackProxy.INSTANCE.asNMSCopy(item)).getOrThrow(IllegalArgumentException::new).toString();if(result.getBytes(StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("Shelf item exceeds protocol limit");return result;}
    private void send(Player player,Shelf shelf,int op){try {ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeByte(1);out.writeByte(op);utf(out,net.momirealms.craftengine.bukkit.api.BukkitAdaptor.adapt(player.getWorld()).dimension().toString());if(op==2){player.sendPluginMessage(engine.javaPlugin(),STATE,bytes.toByteArray());return;}out.writeLong(shelf.anchor);out.writeLong(shelf.revision);if(op==1){player.sendPluginMessage(engine.javaPlugin(),STATE,bytes.toByteArray());return;}out.writeByte(shelf.facing);out.writeBoolean(shelf.invisible);synchronized(shelf){varint(out,op==0?shelf.entries.size():0);if(op==0)for(var pair:shelf.entries.entrySet()){varint(out,pair.getKey());out.writeByte(pair.getValue().mode());out.writeFloat(pair.getValue().width());utf(out,json(pair.getValue().item()));}}if(bytes.size()<=1048576)player.sendPluginMessage(engine.javaPlugin(),STATE,bytes.toByteArray());}catch(IOException|RuntimeException ex){engine.javaPlugin().getLogger().warning("Shelf synchronization failed: "+ex.getMessage());}}
    private static void utf(DataOutputStream out,String value)throws IOException{byte[] b=value.getBytes(StandardCharsets.UTF_8);varint(out,b.length);out.write(b);}
    private static void varint(DataOutputStream out,int value)throws IOException{while((value&~127)!=0){out.writeByte((value&127)|128);value>>>=7;}out.writeByte(value);}
    private synchronized void save(){YamlConfiguration yaml=new YamlConfiguration();for(var pair:shelves.entrySet()){Shelf s=pair.getValue();String p="shelves."+pair.getKey();yaml.set(p+".world",s.world.toString());yaml.set(p+".anchor",s.anchor);yaml.set(p+".revision",s.revision);yaml.set(p+".facing",s.facing);yaml.set(p+".invisible",s.invisible);for(var e:s.entries.entrySet()){String q=p+".entries."+e.getKey();yaml.set(q+".item",e.getValue().item());yaml.set(q+".mode",e.getValue().mode());yaml.set(q+".width",e.getValue().width());}}try{Files.createDirectories(file.toPath().getParent());Path temp=file.toPath().resolveSibling("shelves.yml.tmp");Files.writeString(temp,yaml.saveToString(),StandardCharsets.UTF_8);try{Files.move(temp,file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){throw new IllegalStateException("Cannot persist shelf contents",e);}}
    private void loadWidths(){
        File config=new File(engine.javaPlugin().getDataFolder(),"shelf-items.yml");
        if(!config.exists()){
            YamlConfiguration defaults=new YamlConfiguration();defaults.set("widths.minecraft:stone",2.5);defaults.set("widths.vsrender:demo_shelf_wide_box",2.5);
            try{defaults.save(config);}catch(IOException ex){throw new IllegalStateException("Cannot initialize shelf sizing",ex);}
        }
        var section=YamlConfiguration.loadConfiguration(config).getConfigurationSection("widths");
        if(section!=null)for(String id:section.getKeys(false)){float width=(float)section.getDouble(id);if(Float.isFinite(width)&&width>=.05&&width<=3)widths.put(id,width);}
    }
    private void load(){YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);var root=yaml.getConfigurationSection("shelves");if(root==null)return;for(String k:root.getKeys(false)){var c=root.getConfigurationSection(k);try{Shelf s=new Shelf();s.world=UUID.fromString(c.getString("world"));s.anchor=c.getLong("anchor");s.revision=c.getLong("revision");revisions.accumulateAndGet(s.revision,Math::max);s.facing=c.getInt("facing");s.invisible=c.getBoolean("invisible");var entries=c.getConfigurationSection("entries");if(entries!=null)for(String slot:entries.getKeys(false)){ItemStack item=entries.getItemStack(slot+".item");int n=Integer.parseInt(slot);float width=(float)entries.getDouble(slot+".width");int mode=entries.getInt(slot+".mode");if(item!=null&&n>=0&&n<(s.invisible?1:15)&&mode>=0&&mode<3&&ShelfLayout.fits(n,width,s.invisible))s.entries.put(n,new Entry(item,mode,width));}shelves.put(key(s.world,s.anchor),s);}catch(RuntimeException ex){engine.javaPlugin().getLogger().warning("Invalid shelf record: "+k);}}}
}
