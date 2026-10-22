package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.compiled.RenderPoseBuffer;
import net.momirealms.craftengine.core.render.runtime.RenderFrameBatch;
import net.momirealms.craftengine.core.render.runtime.RenderFrameSink;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bukkit packet edge for native VsRender poses.
 * Display entities are owned/spawned elsewhere; this sink only publishes transform metadata.
 */
public final class BukkitDisplayPoseSink implements RenderFrameSink {
    private final Iterable<Player> viewers;
    private final int[] entityIdsByPart;
    private final BukkitDisplayPartPayload[] payloadsByPart;
    private volatile BukkitDisplayOwnerTransform ownerTransform = BukkitDisplayOwnerTransform.IDENTITY;

    public BukkitDisplayPoseSink(Player viewer, int[] entityIdsByPart) {
        this(List.of(Objects.requireNonNull(viewer, "viewer")), entityIdsByPart, null);
    }

    public BukkitDisplayPoseSink(Iterable<Player> viewers,
                                 int[] entityIdsByPart,
                                 BukkitDisplayPartPayload[] payloadsByPart) {
        this.viewers = Objects.requireNonNull(viewers, "viewers");
        this.entityIdsByPart = Objects.requireNonNull(entityIdsByPart, "entityIdsByPart");
        if (payloadsByPart == null) {
            this.payloadsByPart = new BukkitDisplayPartPayload[this.entityIdsByPart.length];
            java.util.Arrays.fill(this.payloadsByPart, BukkitDisplayPartPayload.transformOnly());
        } else {
            if (payloadsByPart.length != this.entityIdsByPart.length) {
                throw new IllegalArgumentException("payload/entity id part count mismatch");
            }
            this.payloadsByPart = payloadsByPart;
            for (int i = 0; i < this.payloadsByPart.length; i++) {
                if (this.payloadsByPart[i] == null) this.payloadsByPart[i] = BukkitDisplayPartPayload.transformOnly();
            }
        }
    }

    public void ownerTransform(BukkitDisplayOwnerTransform transform) {
        this.ownerTransform = Objects.requireNonNull(transform, "transform");
    }

    public BukkitDisplayOwnerTransform ownerTransform() {
        return ownerTransform;
    }

    @Override
    public void publish(RenderFrameBatch batch) {
        Objects.requireNonNull(batch, "batch");
        if (batch.empty()) return;
        RenderPoseBuffer poses = batch.poses();
        if (poses.partCount() != entityIdsByPart.length) {
            throw new IllegalArgumentException("entity id map/pose part count mismatch");
        }

        List<Object> packets = new ArrayList<>(batch.size());
        for (int i = 0; i < batch.size(); i++) {
            int part = batch.partIndex(i);
            int entityId = entityIdsByPart[part];
            if (entityId < 0) continue;
            List<Object> metadata = createTransformMetadata(poses, part, payloadsByPart[part], ownerTransform, batch.interpolationTicks());
            packets.add(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityId, metadata));
        }
        if (packets.isEmpty()) return;
        for (Player viewer : viewers) viewer.sendPackets(packets, false);
    }

    public static List<Object> createTransformMetadata(RenderPoseBuffer poses,
                                                       int part,
                                                       BukkitDisplayPartPayload payload,
                                                       int interpolationTicks) {
        return createTransformMetadata(poses, part, payload, BukkitDisplayOwnerTransform.IDENTITY, interpolationTicks);
    }

    public static List<Object> createTransformMetadata(RenderPoseBuffer poses,
                                                       int part,
                                                       BukkitDisplayPartPayload payload,
                                                       BukkitDisplayOwnerTransform ownerTransform,
                                                       int interpolationTicks) {
        Objects.requireNonNull(poses, "poses");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(ownerTransform, "ownerTransform");
        float qx = poses.rotationX(part), qy = poses.rotationY(part);
        float qz = poses.rotationZ(part), qw = poses.rotationW(part);

        // Rotate BetterModel's visual offset by the resolved bone rotation without allocating a quaternion/vector helper.
        float ox = payload.offsetX(), oy = payload.offsetY(), oz = payload.offsetZ();
        float tx2 = 2f * (qy * oz - qz * oy);
        float ty2 = 2f * (qz * ox - qx * oz);
        float tz2 = 2f * (qx * oy - qy * ox);
        float rox = ox + qw * tx2 + (qy * tz2 - qz * ty2);
        float roy = oy + qw * ty2 + (qz * tx2 - qx * tz2);
        float roz = oz + qw * tz2 + (qx * ty2 - qy * tx2);

        List<Object> metadata = new ArrayList<>(5);
        if (DisplayData.TransformationInterpolationDuration != null) {
            DisplayData.TransformationInterpolationDuration.addEntityData(interpolationTicks, metadata, true);
        }
        float ownerScale = ownerTransform.scale();
        DisplayData.Translation.addEntityData(new Vector3f(
                (poses.translationX(part) + rox) * ownerScale + payload.positionX() + ownerTransform.offsetX(),
                (poses.translationY(part) + roy) * ownerScale + payload.positionY() + ownerTransform.offsetY(),
                (poses.translationZ(part) + roz) * ownerScale + payload.positionZ() + ownerTransform.offsetZ()), metadata, true);
        DisplayData.Scale.addEntityData(new Vector3f(
                poses.scaleX(part) * payload.scaleX() * ownerScale,
                poses.scaleY(part) * payload.scaleY() * ownerScale,
                poses.scaleZ(part) * payload.scaleZ() * ownerScale), metadata, true);
        DisplayData.LeftRotation.addEntityData(new Quaternionf(qx, qy, qz, qw), metadata, true);
        return metadata;
    }
}
