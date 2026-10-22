package net.momirealms.craftengine.bukkit.render.runtime;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.util.EntityDataUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.runtime.CompiledRenderRuntime;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Native packet-backed Item Display instance owned by VsRender.
 * Scheduling is external; callers tick it from the correct platform owner thread.
 */
public final class BukkitNativeDisplayInstance implements AutoCloseable {
    private final BukkitNativeDisplayAsset asset;
    private final CompiledRenderRuntime runtime;
    private final int[] entityIdsByPart;
    private final UUID[] entityUuidsByPart;
    private final BukkitDisplayPartPayload[] payloadsByPart;
    private final BukkitDisplayPayloadBinding[] payloadBindingsByPart;
    private final ItemStack[] boundItemSnapshots;
    private final Object[] configuredTintedItemsByPart;
    private final Object[] damageTintedItemsByPart;
    private int visualPartCount;
    private final CopyOnWriteArraySet<Player> viewers = new CopyOnWriteArraySet<>();
    private final BukkitDisplayPoseSink poseSink;
    private volatile BukkitDisplayOwnerTransform ownerTransform = BukkitDisplayOwnerTransform.IDENTITY;
    private volatile BukkitDisplayEntityVisualState entityVisualState = BukkitDisplayEntityVisualState.DEFAULT;
    private int damageTintTicks = -1;
    private int damageTintRgb = 0xFF8080;
    private volatile World anchorWorld;
    private volatile double anchorX, anchorY, anchorZ;
    private volatile float anchorXRot, anchorYRot;
    private final AtomicBoolean closed = new AtomicBoolean();

    public BukkitNativeDisplayInstance(BukkitNativeDisplayAsset asset, double anchorX, double anchorY, double anchorZ) {
        this(asset, null, anchorX, anchorY, anchorZ, 0f, 0f);
    }

    public BukkitNativeDisplayInstance(BukkitNativeDisplayAsset asset, WorldPosition position) {
        this(asset, java.util.Objects.requireNonNull(position, "position").world,
                position.x, position.y, position.z, position.xRot, position.yRot);
    }

    public BukkitNativeDisplayInstance(BukkitNativeDisplayAsset asset, World anchorWorld,
                                       double anchorX, double anchorY, double anchorZ,
                                       float anchorXRot, float anchorYRot) {
        this.asset = java.util.Objects.requireNonNull(asset, "asset");
        this.runtime = new CompiledRenderRuntime(asset.asset());
        this.anchorWorld = anchorWorld;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.anchorXRot = anchorXRot;
        this.anchorYRot = anchorYRot;
        this.entityIdsByPart = new int[asset.partCount()];
        this.entityUuidsByPart = new UUID[asset.partCount()];
        this.payloadsByPart = new BukkitDisplayPartPayload[asset.partCount()];
        this.payloadBindingsByPart = new BukkitDisplayPayloadBinding[asset.partCount()];
        this.boundItemSnapshots = new ItemStack[asset.partCount()];
        this.configuredTintedItemsByPart = new Object[asset.partCount()];
        this.damageTintedItemsByPart = new Object[asset.partCount()];
        java.util.Arrays.fill(entityIdsByPart, -1);
        for (int i = 0; i < asset.partCount(); i++) {
            BukkitDisplayPartPayload payload = asset.payload(i);
            payloadsByPart[i] = payload;
            payloadBindingsByPart[i] = asset.binding(i);
            if (!payload.hasItem()) continue;
            allocateEntity(i);
            visualPartCount++;
        }
        this.poseSink = new BukkitDisplayPoseSink(viewers, entityIdsByPart, payloadsByPart);
        this.poseSink.ownerTransform(ownerTransform);
    }

    public boolean closed() { return closed.get(); }
    public int visualPartCount() { return visualPartCount; }
    public BukkitNativeDisplayAsset asset() { return asset; }
    public int entityId(int partIndex) { return entityIdsByPart[partIndex]; }
    public CompiledRenderRuntime runtime() { return runtime; }
    public boolean play(String animationId) { return runtime.play(animationId); }
    public boolean play(String animationId, RenderAnimationMode mode) { return runtime.play(animationId, mode); }
    public boolean stop() { return runtime.stop(); }

    public boolean show(Player viewer) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        if (closed.get() || !viewers.add(viewer)) return false;
        List<Object> packets = new ArrayList<>(visualPartCount * 2);
        var poses = runtime.poses();
        for (int part = 0; part < asset.partCount(); part++) {
            BukkitDisplayPartPayload payload = payloadsByPart[part];
            int entityId = entityIdsByPart[part];
            if (entityId < 0 || !payload.hasItem()) continue;
            packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                    entityId, entityUuidsByPart[part], anchorX, anchorY, anchorZ, anchorXRot, anchorYRot,
                    EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
            ));
            List<Object> metadata = new ArrayList<>(8);
            DisplayData.ItemDisplayData.ItemStack.addEntityData(displayItem(part, payload), metadata, true);
            DisplayData.ItemDisplayData.ItemTransform.addEntityData(payload.itemTransform(), metadata, true);
            addEntityVisualState(metadata);
            metadata.addAll(BukkitDisplayPoseSink.createTransformMetadata(poses, part, payload, ownerTransform, 0));
            packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityId, metadata));
        }
        if (!packets.isEmpty()) viewer.sendPackets(packets, false);
        return true;
    }

    public boolean hide(Player viewer) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        if (!viewers.remove(viewer)) return false;
        Object remove = currentRemovePacket();
        if (remove != null) viewer.sendPacket(remove, false);
        return true;
    }


    /** Number of viewers currently owning this packet-only instance. */
    public int viewerCount() {
        return viewers.size();
    }

    /**
     * Reconciles the viewer set without recreating the instance.
     * New viewers receive a full spawn snapshot; stale viewers receive the owned remove packet.
     */
    public void syncViewers(java.util.Collection<? extends Player> desired) {
        java.util.Objects.requireNonNull(desired, "desired");
        java.util.HashSet<Player> target = new java.util.HashSet<>(desired);
        for (Player viewer : viewers) {
            if (!target.contains(viewer)) hide(viewer);
        }
        for (Player viewer : target) show(viewer);
    }

    /**
     * Moves the shared Item Display anchor without respawning parts. A world change invalidates the
     * client-side entities and clears viewers so the owner host can respawn them in the new world.
     */
    public boolean updateAnchor(WorldPosition position) {
        java.util.Objects.requireNonNull(position, "position");
        if (closed.get()) return false;
        World nextWorld = position.world;
        if (!java.util.Objects.equals(anchorWorld, nextWorld)) {
            Object remove = currentRemovePacket();
            if (remove != null) {
                for (Player viewer : viewers) viewer.sendPacket(remove, false);
            }
            viewers.clear();
            anchorWorld = nextWorld;
            anchorX = position.x;
            anchorY = position.y;
            anchorZ = position.z;
            anchorXRot = position.xRot;
            anchorYRot = position.yRot;
            return true;
        }

        if (Double.doubleToLongBits(anchorX) == Double.doubleToLongBits(position.x)
                && Double.doubleToLongBits(anchorY) == Double.doubleToLongBits(position.y)
                && Double.doubleToLongBits(anchorZ) == Double.doubleToLongBits(position.z)
                && Float.floatToIntBits(anchorXRot) == Float.floatToIntBits(position.xRot)
                && Float.floatToIntBits(anchorYRot) == Float.floatToIntBits(position.yRot)) {
            return false;
        }

        anchorX = position.x;
        anchorY = position.y;
        anchorZ = position.z;
        anchorXRot = position.xRot;
        anchorYRot = position.yRot;
        if (!viewers.isEmpty() && visualPartCount > 0) {
            List<Object> packets = new ArrayList<>(visualPartCount);
            for (int entityId : entityIdsByPart) {
                if (entityId < 0) continue;
                packets.add(EntityUtils.createUpdatePosPacket(
                        entityId, anchorX, anchorY, anchorZ, anchorYRot, anchorXRot, false
                ));
            }
            if (!packets.isEmpty()) {
                for (Player viewer : viewers) viewer.sendPackets(packets, false);
            }
        }
        return true;
    }


    /** Mirrors owner invisibility/glow onto every packet-only display without respawning. */
    public boolean updateEntityVisualState(BukkitDisplayEntityVisualState state) {
        java.util.Objects.requireNonNull(state, "state");
        if (closed.get() || state.equals(entityVisualState)) return false;
        entityVisualState = state;
        if (!viewers.isEmpty() && visualPartCount > 0) {
            List<Object> packets = new ArrayList<>(visualPartCount);
            for (int entityId : entityIdsByPart) {
                if (entityId < 0) continue;
                List<Object> metadata = new ArrayList<>(1);
                addEntityVisualState(metadata);
                packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityId, metadata));
            }
            for (Player viewer : viewers) viewer.sendPackets(packets, false);
        }
        return true;
    }

    public BukkitDisplayEntityVisualState entityVisualState() {
        return entityVisualState;
    }

    /** Starts BetterModel-compatible damage tint. Retriggers while active are intentionally ignored. */
    public synchronized boolean triggerDamageTint(int rgb) {
        if (closed.get() || !BukkitDisplayTintSupport.supported() || damageTintTicks >= 0) return false;
        damageTintRgb = rgb & 0xFFFFFF;
        damageTintTicks = 10;
        java.util.Arrays.fill(damageTintedItemsByPart, null);
        publishCurrentItems();
        return true;
    }

    /** Advances the tint lifecycle once per owner tick. */
    public synchronized boolean tickDamageTint() {
        if (closed.get() || damageTintTicks < 0) return false;
        if (damageTintTicks-- != 0) return false;
        damageTintTicks = -1;
        java.util.Arrays.fill(damageTintedItemsByPart, null);
        publishCurrentItems();
        return true;
    }

    public synchronized boolean damageTintActive() {
        return damageTintTicks >= 0;
    }

    /**
     * Updates entity-owner scale/passenger compensation without resetting animation state.
     * BetterModel parity is applied at the packet edge, so the compiled rig/clip stays shared.
     */
    public boolean updateOwnerTransform(BukkitDisplayOwnerTransform transform) {
        java.util.Objects.requireNonNull(transform, "transform");
        if (closed.get() || transform.equals(ownerTransform)) return false;
        ownerTransform = transform;
        poseSink.ownerTransform(transform);
        if (!viewers.isEmpty() && visualPartCount > 0) {
            List<Object> packets = new ArrayList<>(visualPartCount);
            var poses = runtime.poses();
            for (int part = 0; part < payloadsByPart.length; part++) {
                int entityId = entityIdsByPart[part];
                BukkitDisplayPartPayload payload = payloadsByPart[part];
                if (entityId < 0 || !payload.hasItem()) continue;
                packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(
                        entityId, BukkitDisplayPoseSink.createTransformMetadata(poses, part, payload, transform, 1)));
            }
            if (!packets.isEmpty()) for (Player viewer : viewers) viewer.sendPackets(packets, false);
        }
        return true;
    }

    public BukkitDisplayOwnerTransform ownerTransform() {
        return ownerTransform;
    }

    /**
     * Refreshes imported MAIN_HAND/OFF_HAND bindings directly from Bukkit. BetterModel is no longer
     * consulted after import for these bindings. Animation state and stable IDs are preserved.
     */
    public boolean refreshBoundPayloads(net.momirealms.craftengine.core.entity.Entity owner) {
        java.util.Objects.requireNonNull(owner, "owner");
        if (closed.get() || asset.dynamicBindingCount() == 0) return false;
        if (!(owner.platformEntity() instanceof LivingEntity living)) return false;
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) return false;
        boolean changed = false;
        for (int part = 0; part < payloadBindingsByPart.length; part++) {
            BukkitDisplayPayloadBinding binding = payloadBindingsByPart[part];
            if (binding == null || !binding.dynamic()) continue;
            ItemStack current = binding == BukkitDisplayPayloadBinding.MAIN_HAND
                    ? equipment.getItemInMainHand() : equipment.getItemInOffHand();
            ItemStack previous = boundItemSnapshots[part];
            if (previous != null && previous.equals(current)) continue;
            boundItemSnapshots[part] = current.clone();
            BukkitDisplayPartPayload template = payloadsByPart[part];
            BukkitDisplayPartPayload next;
            if (current.getType().isAir() || current.getAmount() <= 0) {
                next = new BukkitDisplayPartPayload(
                        null, template.itemTransform(),
                        template.positionX(), template.positionY(), template.positionZ(),
                        template.offsetX(), template.offsetY(), template.offsetZ(),
                        template.scaleX(), template.scaleY(), template.scaleZ());
            } else {
                Object minecraftItem = BukkitAdaptor.adapt(current.clone()).minecraftItem();
                next = BukkitDisplayPartPayload.item(
                        minecraftItem, template.itemTransform(),
                        template.positionX(), template.positionY(), template.positionZ(),
                        template.offsetX(), template.offsetY(), template.offsetZ(),
                        template.scaleX(), template.scaleY(), template.scaleZ());
            }
            changed |= refreshPayload(part, next);
        }
        return changed;
    }

    private synchronized boolean refreshPayload(int part, BukkitDisplayPartPayload next) {
        if (closed.get()) return false;
        BukkitDisplayPartPayload previous = payloadsByPart[part];
        boolean before = previous.hasItem();
        boolean after = next.hasItem();
        payloadsByPart[part] = next;
        configuredTintedItemsByPart[part] = null;
        damageTintedItemsByPart[part] = null;
        if (!before && !after) return true;

        List<Object> packets = new ArrayList<>(2);
        if (before && !after) {
            int oldId = entityIdsByPart[part];
            if (oldId >= 0) {
                IntArrayList removed = new IntArrayList(1);
                removed.add(oldId);
                packets.add(ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(removed));
            }
            entityIdsByPart[part] = -1;
            entityUuidsByPart[part] = null;
            visualPartCount--;
        } else {
            if (!before) {
                allocateEntity(part);
                visualPartCount++;
                packets.add(spawnPacket(part));
            }
            int entityId = entityIdsByPart[part];
            List<Object> metadata = new ArrayList<>(8);
            configuredTintedItemsByPart[part] = null;
            damageTintedItemsByPart[part] = null;
            DisplayData.ItemDisplayData.ItemStack.addEntityData(displayItem(part, next), metadata, true);
            DisplayData.ItemDisplayData.ItemTransform.addEntityData(next.itemTransform(), metadata, true);
            addEntityVisualState(metadata);
            metadata.addAll(BukkitDisplayPoseSink.createTransformMetadata(runtime.poses(), part, next, ownerTransform, 0));
            packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityId, metadata));
        }
        if (!packets.isEmpty() && !viewers.isEmpty()) {
            for (Player viewer : viewers) viewer.sendPackets(packets, false);
        }
        return true;
    }

    /** Returns the nearest current viewer distance, or +infinity when no viewer can see this anchor world. */
    public float nearestViewerDistance() {
        double bestSq = Double.POSITIVE_INFINITY;
        for (Player viewer : viewers) {
            if (anchorWorld != null && !anchorWorld.equals(viewer.world())) continue;
            double dx = viewer.x() - anchorX;
            double dy = viewer.y() - anchorY;
            double dz = viewer.z() - anchorZ;
            double sq = dx * dx + dy * dy + dz * dz;
            if (sq < bestSq) bestSq = sq;
        }
        return bestSq == Double.POSITIVE_INFINITY ? Float.POSITIVE_INFINITY : (float) Math.sqrt(bestSq);
    }

    /** Advances the native compiled runtime. No work is performed when closed. */
    public boolean tick(float deltaSeconds, float nearestViewerDistance) {
        if (closed.get()) return false;
        return runtime.tick(deltaSeconds, !viewers.isEmpty(), nearestViewerDistance, poseSink);
    }

    /** Advances animation time while culled without forcing packet publication. */
    public boolean tick(float deltaSeconds, float nearestViewerDistance, boolean advanceWhenCulled) {
        if (closed.get()) return false;
        return runtime.tick(deltaSeconds, !viewers.isEmpty(), nearestViewerDistance, advanceWhenCulled, poseSink);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        Object remove = currentRemovePacket();
        if (remove != null) {
            for (Player viewer : viewers) viewer.sendPacket(remove, false);
        }
        viewers.clear();
    }

    /** Replaces visual payloads without resetting animation state or stable IDs for still-visible parts. */
    public synchronized boolean refreshPayloads(BukkitDisplayPartPayload[] replacement) {
        java.util.Objects.requireNonNull(replacement, "replacement");
        if (replacement.length != payloadsByPart.length) {
            throw new IllegalArgumentException("replacement payload/rig part count mismatch");
        }
        if (closed.get()) return false;
        List<Object> packets = new ArrayList<>();
        IntArrayList removed = new IntArrayList();
        boolean changed = false;
        var poses = runtime.poses();
        for (int part = 0; part < replacement.length; part++) {
            BukkitDisplayPartPayload next = replacement[part] == null
                    ? BukkitDisplayPartPayload.transformOnly() : replacement[part];
            BukkitDisplayPartPayload previous = payloadsByPart[part];
            boolean before = previous.hasItem();
            boolean after = next.hasItem();
            payloadsByPart[part] = next;
            configuredTintedItemsByPart[part] = null;
            damageTintedItemsByPart[part] = null;
            if (!before && !after) continue;
            changed = true;
            if (before && !after) {
                int oldId = entityIdsByPart[part];
                if (oldId >= 0) removed.add(oldId);
                entityIdsByPart[part] = -1;
                entityUuidsByPart[part] = null;
                visualPartCount--;
                continue;
            }
            if (!before) {
                allocateEntity(part);
                visualPartCount++;
                packets.add(spawnPacket(part));
            }
            int entityId = entityIdsByPart[part];
            List<Object> metadata = new ArrayList<>(8);
            configuredTintedItemsByPart[part] = null;
            damageTintedItemsByPart[part] = null;
            DisplayData.ItemDisplayData.ItemStack.addEntityData(displayItem(part, next), metadata, true);
            DisplayData.ItemDisplayData.ItemTransform.addEntityData(next.itemTransform(), metadata, true);
            addEntityVisualState(metadata);
            metadata.addAll(BukkitDisplayPoseSink.createTransformMetadata(poses, part, next, ownerTransform, 0));
            packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityId, metadata));
        }
        if (!removed.isEmpty()) packets.add(ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(removed));
        if (!packets.isEmpty() && !viewers.isEmpty()) {
            for (Player viewer : viewers) viewer.sendPackets(packets, false);
        }
        return changed;
    }

    private Object displayItem(int part, BukkitDisplayPartPayload payload) {
        if (!payload.hasItem()) return payload.minecraftItem();
        if (damageTintTicks >= 0) {
            Object cached = damageTintedItemsByPart[part];
            if (cached != null) return cached;
            cached = BukkitDisplayTintSupport.tint(payload.minecraftItem(), damageTintRgb);
            damageTintedItemsByPart[part] = cached;
            return cached;
        }
        int configuredTint = asset.tintRgb(part);
        if (configuredTint == BukkitNativeDisplayAsset.NO_TINT) return payload.minecraftItem();
        Object cached = configuredTintedItemsByPart[part];
        if (cached != null) return cached;
        cached = BukkitDisplayTintSupport.tint(payload.minecraftItem(), configuredTint);
        configuredTintedItemsByPart[part] = cached;
        return cached;
    }

    private void addEntityVisualState(List<Object> metadata) {
        byte flags = 0;
        flags = EntityDataUtils.setSharedFlag(flags, EntityDataUtils.SHARED_FLAG_INVISIBLE, entityVisualState.invisible());
        flags = EntityDataUtils.setSharedFlag(flags, EntityDataUtils.SHARED_FLAG_GLOWING, entityVisualState.glowing());
        BaseEntityData.SharedFlags.addEntityData(flags, metadata, true);
    }

    private void publishCurrentItems() {
        if (viewers.isEmpty() || visualPartCount <= 0) return;
        List<Object> packets = new ArrayList<>(visualPartCount);
        for (int part = 0; part < payloadsByPart.length; part++) {
            int entityId = entityIdsByPart[part];
            BukkitDisplayPartPayload payload = payloadsByPart[part];
            if (entityId < 0 || !payload.hasItem()) continue;
            List<Object> metadata = new ArrayList<>(1);
            DisplayData.ItemDisplayData.ItemStack.addEntityData(displayItem(part, payload), metadata, true);
            packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityId, metadata));
        }
        if (!packets.isEmpty()) for (Player viewer : viewers) viewer.sendPackets(packets, false);
    }

    private void allocateEntity(int part) {
        entityIdsByPart[part] = EntityUtils.ENTITY_COUNTER.incrementAndGet();
        entityUuidsByPart[part] = UUID.randomUUID();
    }

    private Object spawnPacket(int part) {
        return ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityIdsByPart[part], entityUuidsByPart[part], anchorX, anchorY, anchorZ, anchorXRot, anchorYRot,
                EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
    }

    private Object currentRemovePacket() {
        if (visualPartCount <= 0) return null;
        IntArrayList ids = new IntArrayList(visualPartCount);
        for (int id : entityIdsByPart) if (id >= 0) ids.add(id);
        return ids.isEmpty() ? null : ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(ids);
    }
}
