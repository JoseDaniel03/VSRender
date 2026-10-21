package net.momirealms.craftengine.bukkit.render.runtime;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.data.InteractionData;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.compiled.RenderHitBoxBuffer;
import net.momirealms.craftengine.core.render.runtime.CompiledRenderRuntime;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Packet-only Interaction-entity envelope for native rig hitboxes.
 * Exact X/Y/Z hit testing stays in {@link BukkitNativeRigHitBoxRegistry}; the Interaction entity uses
 * max(width, depth) as a conservative horizontal envelope because vanilla Interaction has one width.
 */
final class BukkitNativeRigHitBoxInstance implements AutoCloseable {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final CompiledRenderRuntime runtime;
    private final Entity owner;
    private final BiConsumer<Integer, org.bukkit.entity.Player> mountRequest;
    private final int[] parts;
    private final int[] entityIds;
    private final UUID[] entityUuids;
    private final double[] lastX, lastY, lastZ;
    private final float[] lastWidth, lastHeight;
    private final AABB[] physicalBounds;
    private BukkitNativeRigPhysicalBody physicalBody;
    private final CopyOnWriteArraySet<Player> viewers = new CopyOnWriteArraySet<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private WorldPosition anchor;
    private BukkitDisplayOwnerTransform ownerTransform;

    BukkitNativeRigHitBoxInstance(CompiledRenderRuntime runtime, Entity owner,
                                  WorldPosition anchor, BukkitDisplayOwnerTransform ownerTransform,
                                  BiConsumer<Integer, org.bukkit.entity.Player> mountRequest) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.mountRequest = Objects.requireNonNull(mountRequest, "mountRequest");
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.ownerTransform = Objects.requireNonNull(ownerTransform, "ownerTransform");
        RenderHitBoxBuffer buffer = runtime.hitBoxes();
        this.parts = buffer.hitBoxIndices();
        this.entityIds = new int[parts.length];
        this.entityUuids = new UUID[parts.length];
        this.lastX = new double[parts.length];
        this.lastY = new double[parts.length];
        this.lastZ = new double[parts.length];
        this.lastWidth = new float[parts.length];
        this.lastHeight = new float[parts.length];
        this.physicalBounds = new AABB[parts.length];
        java.util.Arrays.fill(lastX, Double.NaN);
        java.util.Arrays.fill(lastY, Double.NaN);
        java.util.Arrays.fill(lastZ, Double.NaN);
        for (int i = 0; i < parts.length; i++) {
            entityIds[i] = EntityUtils.ENTITY_COUNTER.incrementAndGet();
            entityUuids[i] = UUID.randomUUID();
        }
        updateGeometry(anchor, ownerTransform, false);
        for (int i = 0; i < parts.length; i++) {
            BukkitNativeRigHitBoxRegistry.register(
                    entityIds[i], owner.uuid(), owner.entityId(),
                    (org.bukkit.entity.Entity) owner.platformEntity(), anchor.world.uuid(), parts[i], exactBounds(i),
                    runtime.state().asset().rig().mountPolicy(parts[i]), mountRequest);
        }
    }

    boolean empty() { return parts.length == 0; }

    void syncViewers(Collection<? extends Player> desired) {
        if (closed.get()) return;
        HashSet<Player> target = new HashSet<>(Objects.requireNonNull(desired, "desired"));
        for (Player viewer : viewers) if (!target.contains(viewer)) hide(viewer);
        for (Player viewer : target) show(viewer);
    }

    void update(WorldPosition anchor, BukkitDisplayOwnerTransform ownerTransform) {
        updateGeometry(anchor, ownerTransform, true);
    }

    private void updateGeometry(WorldPosition anchor, BukkitDisplayOwnerTransform ownerTransform, boolean syncPhysical) {
        if (closed.get()) return;
        Objects.requireNonNull(anchor, "anchor");
        boolean worldChanged = !Objects.equals(this.anchor.world, anchor.world);
        if (worldChanged) {
            closePhysicalBody();
            Object remove = removePacket();
            if (remove != null) for (Player viewer : viewers) viewer.sendPacket(remove, false);
            viewers.clear();
        }
        this.anchor = anchor;
        this.ownerTransform = Objects.requireNonNull(ownerTransform, "ownerTransform");
        RenderHitBoxBuffer hitBoxes = runtime.hitBoxes();
        List<Object> movement = viewers.isEmpty() ? null : new ArrayList<>(parts.length);
        List<Object> metadata = viewers.isEmpty() ? null : new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            int part = parts[i];
            WorldCenter center = worldCenter(hitBoxes, part, anchor, ownerTransform);
            float scale = ownerTransform.scale();
            float width = Math.max(hitBoxes.width(part), hitBoxes.depth(part)) * scale;
            float height = hitBoxes.height(part) * scale;
            double packetY = center.y - height * 0.5d;
            boolean moved = Double.doubleToLongBits(lastX[i]) != Double.doubleToLongBits(center.x)
                    || Double.doubleToLongBits(lastY[i]) != Double.doubleToLongBits(packetY)
                    || Double.doubleToLongBits(lastZ[i]) != Double.doubleToLongBits(center.z);
            boolean resized = Float.floatToIntBits(lastWidth[i]) != Float.floatToIntBits(width)
                    || Float.floatToIntBits(lastHeight[i]) != Float.floatToIntBits(height);
            lastX[i] = center.x; lastY[i] = packetY; lastZ[i] = center.z;
            lastWidth[i] = width; lastHeight[i] = height;
            AABB bounds = exactBounds(i);
            physicalBounds[i] = bounds;
            BukkitNativeRigHitBoxRegistry.update(entityIds[i], anchor.world.uuid(), bounds);
            if (moved && movement != null) {
                movement.add(EntityUtils.createUpdatePosPacket(entityIds[i], center.x, packetY, center.z,
                        anchor.yRot, anchor.xRot, false));
            }
            if (resized && metadata != null) {
                metadata.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[i], metadata(width, height)));
            }
        }
        if (syncPhysical) {
            if (physicalBody == null) {
                physicalBody = BukkitNativeRigPhysicalBodyBridge.create(owner.minecraftEntity(), owner.uuid(), physicalBounds);
                physicalBody.sync(physicalBounds);
            } else {
                physicalBody.sync(physicalBounds);
            }
        }
        if (movement != null && !movement.isEmpty()) for (Player viewer : viewers) viewer.sendPackets(movement, false);
        if (metadata != null && !metadata.isEmpty()) for (Player viewer : viewers) viewer.sendPackets(metadata, false);
    }

    boolean show(Player viewer) {
        if (closed.get() || !viewers.add(viewer)) return false;
        List<Object> packets = new ArrayList<>(parts.length * 2);
        for (int i = 0; i < parts.length; i++) {
            packets.add(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                    entityIds[i], entityUuids[i], lastX[i], lastY[i], lastZ[i], anchor.xRot, anchor.yRot,
                    EntityTypesProxy.INTERACTION, 0, Vec3Proxy.ZERO, 0));
            packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[i], metadata(lastWidth[i], lastHeight[i])));
        }
        if (!packets.isEmpty()) viewer.sendPackets(packets, false);
        return true;
    }

    boolean hide(Player viewer) {
        if (!viewers.remove(viewer)) return false;
        Object remove = removePacket();
        if (remove != null) viewer.sendPacket(remove, false);
        return true;
    }

    private static List<Object> metadata(float width, float height) {
        ArrayList<Object> values = new ArrayList<>(4);
        InteractionData.Width.addEntityDataIfNotDefaultValue(width, values);
        InteractionData.Height.addEntityDataIfNotDefaultValue(height, values);
        InteractionData.Response.addEntityDataIfNotDefaultValue(true, values);
        BaseEntityData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, values);
        return values;
    }

    private Object removePacket() {
        if (entityIds.length == 0) return null;
        IntArrayList ids = new IntArrayList(entityIds.length);
        for (int id : entityIds) ids.add(id);
        return ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(ids);
    }

    private AABB exactBounds(int slot) {
        RenderHitBoxBuffer hitBoxes = runtime.hitBoxes();
        int part = parts[slot];
        float scale = ownerTransform.scale();
        double cx = lastX[slot];
        double cy = lastY[slot] + lastHeight[slot] * 0.5d;
        double cz = lastZ[slot];
        return new AABB(
                cx + hitBoxes.minX(part) * scale,
                cy + hitBoxes.minY(part) * scale,
                cz + hitBoxes.minZ(part) * scale,
                cx + hitBoxes.maxX(part) * scale,
                cy + hitBoxes.maxY(part) * scale,
                cz + hitBoxes.maxZ(part) * scale
        );
    }

    private static WorldCenter worldCenter(RenderHitBoxBuffer hitBoxes, int part, WorldPosition anchor,
                                           BukkitDisplayOwnerTransform ownerTransform) {
        float x = hitBoxes.centerX(part) * ownerTransform.scale() + ownerTransform.offsetX();
        float y = hitBoxes.centerY(part) * ownerTransform.scale() + ownerTransform.offsetY();
        float z = hitBoxes.centerZ(part) * ownerTransform.scale() + ownerTransform.offsetZ();

        float pitch = -anchor.xRot * DEG_TO_RAD;
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float py = y * cp - z * sp;
        float pz = y * sp + z * cp;
        y = py; z = pz;

        float yaw = -anchor.yRot * DEG_TO_RAD;
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
        float px = x * cy + z * sy;
        float pzz = -x * sy + z * cy;
        return new WorldCenter(anchor.x + px, anchor.y + y, anchor.z + pzz);
    }

    private void closePhysicalBody() {
        BukkitNativeRigPhysicalBody body = physicalBody;
        physicalBody = null;
        if (body != null) body.close();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        closePhysicalBody();
        Object remove = removePacket();
        if (remove != null) for (Player viewer : viewers) viewer.sendPacket(remove, false);
        viewers.clear();
        for (int id : entityIds) BukkitNativeRigHitBoxRegistry.unregister(id);
    }

    private record WorldCenter(double x, double y, double z) {}
}