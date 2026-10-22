package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.render.attachment.RenderPartTransform;
import net.momirealms.craftengine.core.render.attachment.RenderTransformSpace;
import net.momirealms.craftengine.core.render.compiled.RenderPoseBuffer;
import net.momirealms.craftengine.core.render.rig.RenderQuat;

/** Shared conversion from the dense native pose buffer to the public attachment contract. */
final class BukkitNativeAttachmentSnapshots {
    private BukkitNativeAttachmentSnapshots() {
    }

    static RenderPartTransform local(RenderPoseBuffer poses, int partIndex) {
        RenderQuat quaternion = new RenderQuat(
                poses.rotationX(partIndex), poses.rotationY(partIndex),
                poses.rotationZ(partIndex), poses.rotationW(partIndex));
        var euler = quaternion.toEulerZYXDegrees();
        return new RenderPartTransform(
                RenderTransformSpace.INSTANCE_LOCAL,
                poses.translationX(partIndex), poses.translationY(partIndex), poses.translationZ(partIndex),
                euler.x(), euler.y(), euler.z()
        );
    }
}
