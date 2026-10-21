package net.momirealms.craftengine.bukkit.render.runtime;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.compiled.IndexedRenderRig;
import net.momirealms.craftengine.core.render.compiled.RenderPoseBuffer;
import net.momirealms.craftengine.core.render.rig.RenderNameTagMode;
import net.momirealms.craftengine.core.render.rig.RenderNameTagVisibility;
import net.momirealms.craftengine.core.render.runtime.CompiledRenderRuntime;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Packet-only native TextDisplay nametags for TAG / MOB_TAG / PLAYER_TAG rig parts.
 * Visibility follows BetterModel's custom-name policy: PLAYER_TAG is always visible, MOB_TAG is
 * focus-gated, and TAG is always visible only when the owner is a player.
 */
final class BukkitNativeRigNameTagInstance implements AutoCloseable {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final Vector3f NAME_TRANSFORM = new Vector3f(-1f / 40f, -0.2f - 1f / 40f, 0f);

    private final CompiledRenderRuntime runtime;
    private final Entity owner;
    private final IndexedRenderRig rig;
    private final int[] parts;
    private final int[] entityIds;
    private final UUID[] entityUuids;
    private final double[] lastX, lastY, lastZ;
    private final HashSet<Player>[] visibleTo;
    private final AtomicBoolean closed = new AtomicBoolean();
    private WorldPosition anchor;
    private BukkitDisplayOwnerTransform ownerTransform;
    private Component currentName;

    @SuppressWarnings("unchecked")
    BukkitNativeRigNameTagInstance(CompiledRenderRuntime runtime, Entity owner,
                                   WorldPosition anchor, BukkitDisplayOwnerTransform ownerTransform) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.rig = runtime.state().asset().rig();
        this.parts = rig.nameTagIndices();
        this.entityIds = new int[parts.length];
        this.entityUuids = new UUID[parts.length];
        this.lastX = new double[parts.length];
        this.lastY = new double[parts.length];
        this.lastZ = new double[parts.length];
        this.visibleTo = new HashSet[parts.length];
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.ownerTransform = Objects.requireNonNull(ownerTransform, "ownerTransform");
        for (int i = 0; i < parts.length; i++) {
            entityIds[i] = EntityUtils.ENTITY_COUNTER.incrementAndGet();
            entityUuids[i] = UUID.randomUUID();
            visibleTo[i] = new HashSet<>();
        }
        update(anchor, ownerTransform);
    }

    boolean empty() { return parts.length == 0; }

    void update(WorldPosition anchor, BukkitDisplayOwnerTransform ownerTransform) {
        if (closed.get()) return;
        Objects.requireNonNull(anchor, "anchor");
        boolean worldChanged = !Objects.equals(this.anchor.world, anchor.world);
        if (worldChanged) clearAllViewers();
        this.anchor = anchor;
        this.ownerTransform = Objects.requireNonNull(ownerTransform, "ownerTransform");
        RenderPoseBuffer pose = runtime.poses();
        for (int i = 0; i < parts.length; i++) {
            WorldCenter center = worldCenter(pose, parts[i], anchor, ownerTransform);
            lastX[i] = center.x;
            lastY[i] = center.y;
            lastZ[i] = center.z;
        }
        Component next = platformCustomName();
        if (!Objects.equals(currentName, next)) {
            currentName = next;
            if (next == null) clearAllViewers();
            else sendNameMetadataToVisible();
        }
    }

    void syncViewers(Collection<? extends Player> desired) {
        if (closed.get()) return;
        HashSet<Player> target = new HashSet<>(Objects.requireNonNull(desired, "desired"));
        for (int i = 0; i < parts.length; i++) {
            for (Player viewer : new ArrayList<>(visibleTo[i])) {
                if (!target.contains(viewer)) {
                    visibleTo[i].remove(viewer);
                    viewer.sendPacket(removePacket(entityIds[i]), false);
                }
            }
        }
        for (Player viewer : target) syncViewer(viewer);
    }

    boolean show(Player viewer) {
        if (closed.get()) return false;
        syncViewer(viewer);
        return true;
    }

    boolean hide(Player viewer) {
        if (closed.get()) return false;
        boolean changed = false;
        for (int i = 0; i < parts.length; i++) {
            if (visibleTo[i].remove(viewer)) {
                viewer.sendPacket(removePacket(entityIds[i]), false);
                changed = true;
            }
        }
        return changed;
    }

    private void syncViewer(Player viewer) {
        Component name = currentName;
        for (int i = 0; i < parts.length; i++) {
            boolean should = name != null && visible(viewer, rig.nameTagMode(parts[i]));
            boolean shown = visibleTo[i].contains(viewer);
            if (should && !shown) {
                visibleTo[i].add(viewer);
                viewer.sendPackets(List.of(spawnPacket(i), metadataPacket(i, name)), false);
            } else if (should) {
                viewer.sendPacket(EntityUtils.createUpdatePosPacket(entityIds[i], lastX[i], lastY[i], lastZ[i], 0f, 0f, false), false);
            } else if (shown) {
                visibleTo[i].remove(viewer);
                viewer.sendPacket(removePacket(entityIds[i]), false);
            }
        }
    }

    private boolean visible(Player viewer, RenderNameTagMode mode) {
        if (mode == RenderNameTagMode.PLAYER_TAG) return true;
        if (mode == RenderNameTagMode.TAG && owner instanceof Player) return true;
        return customNameVisible(viewer, anchor);
    }

    /** BetterModel EntityUtil.isCustomNameVisible / isInPoint parity. */
    static boolean customNameVisible(Player viewer, WorldPosition target) {
        return RenderNameTagVisibility.customNameVisible(
                Objects.equals(viewer.world(), target.world),
                viewer.x(), viewer.y(), viewer.z(), viewer.yRot(), viewer.xRot(),
                target.x, target.y, target.z);
    }

    private Component platformCustomName() {
        Object platform = owner.platformEntity();
        if (!(platform instanceof org.bukkit.entity.Entity entity)) return null;
        return entity.customName();
    }

    private Object spawnPacket(int slot) {
        return ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entityIds[slot], entityUuids[slot], lastX[slot], lastY[slot], lastZ[slot], 0f, 0f,
                EntityTypesProxy.TEXT_DISPLAY, 0, Vec3Proxy.ZERO, 0);
    }

    private Object metadataPacket(int slot, Component name) {
        return ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityIds[slot], metadata(name));
    }

    private static List<Object> metadata(Component name) {
        ArrayList<Object> values = new ArrayList<>(5);
        DisplayData.TextDisplayData.Text.addEntityData(ComponentUtils.adventureToMinecraft(name), values, true);
        DisplayData.Translation.addEntityData(NAME_TRANSFORM, values, true);
        DisplayData.BillboardConstraints.addEntityData((byte) 3, values, true);
        if (DisplayData.PosRotInterpolationDuration != null) {
            DisplayData.PosRotInterpolationDuration.addEntityData(3, values, true);
        } else if (DisplayData.InterpolationDuration != null) {
            DisplayData.InterpolationDuration.addEntityData(3, values, true);
        }
        return values;
    }

    private void sendNameMetadataToVisible() {
        Component name = currentName;
        if (name == null) return;
        for (int i = 0; i < parts.length; i++) {
            if (visibleTo[i].isEmpty()) continue;
            Object packet = metadataPacket(i, name);
            for (Player viewer : visibleTo[i]) viewer.sendPacket(packet, false);
        }
    }

    private void clearAllViewers() {
        for (int i = 0; i < parts.length; i++) {
            if (visibleTo[i].isEmpty()) continue;
            Object remove = removePacket(entityIds[i]);
            for (Player viewer : visibleTo[i]) viewer.sendPacket(remove, false);
            visibleTo[i].clear();
        }
    }

    private static Object removePacket(int entityId) {
        IntArrayList ids = new IntArrayList(1);
        ids.add(entityId);
        return ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(ids);
    }

    private WorldCenter worldCenter(RenderPoseBuffer pose, int part, WorldPosition anchor,
                                    BukkitDisplayOwnerTransform ownerTransform) {
        // BetterModel BonePosition(global=0, local=hitBoxPoint): local point rotates with bone and is
        // multiplied by the resolved bone scale before the owner's entity scale/passenger transform.
        float ox = rig.anchorX(part), oy = rig.anchorY(part), oz = rig.anchorZ(part);
        float qx = pose.rotationX(part), qy = pose.rotationY(part), qz = pose.rotationZ(part), qw = pose.rotationW(part);
        float tx = 2f * (qy * oz - qz * oy);
        float ty = 2f * (qz * ox - qx * oz);
        float tz = 2f * (qx * oy - qy * ox);
        float rx = ox + qw * tx + (qy * tz - qz * ty);
        float ry = oy + qw * ty + (qz * tx - qx * tz);
        float rz = oz + qw * tz + (qx * ty - qy * tx);
        float x = (pose.translationX(part) + rx * pose.scaleX(part)) * ownerTransform.scale() + ownerTransform.offsetX();
        float y = (pose.translationY(part) + ry * pose.scaleY(part)) * ownerTransform.scale() + ownerTransform.offsetY();
        float z = (pose.translationZ(part) + rz * pose.scaleZ(part)) * ownerTransform.scale() + ownerTransform.offsetZ();

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

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        clearAllViewers();
    }

    private record WorldCenter(double x, double y, double z) {}
}
