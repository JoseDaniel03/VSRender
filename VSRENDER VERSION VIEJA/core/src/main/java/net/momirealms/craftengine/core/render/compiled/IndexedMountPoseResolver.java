package net.momirealms.craftengine.core.render.compiled;

import java.util.Objects;

/** Resolves mount anchors from the already-resolved rig pose without platform dependencies. */
public final class IndexedMountPoseResolver {
    private IndexedMountPoseResolver() {
    }

    /**
     * Resolves BetterModel-compatible mount anchors in rig instance space.
     * The imported anchor offset is local to the part, so it rotates with the resolved part rotation
     * and is scaled by the resolved part scale before being added to the resolved translation.
     */
    public static void resolve(IndexedRenderRig rig, RenderPoseBuffer poses, RenderMountBuffer out) {
        Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(poses, "poses");
        Objects.requireNonNull(out, "out");
        if (poses.rig() != rig || out.rig() != rig) {
            throw new IllegalArgumentException("rig/buffer identity mismatch");
        }

        out.beginFrame();
        for (int part : rig.mountIndices()) {
            float ox = rig.anchorX(part);
            float oy = rig.anchorY(part);
            float oz = rig.anchorZ(part);
            float qx = poses.rotationX(part);
            float qy = poses.rotationY(part);
            float qz = poses.rotationZ(part);
            float qw = poses.rotationW(part);

            float tx2 = 2f * (qy * oz - qz * oy);
            float ty2 = 2f * (qz * ox - qx * oz);
            float tz2 = 2f * (qx * oy - qy * ox);
            float rox = ox + qw * tx2 + (qy * tz2 - qz * ty2);
            float roy = oy + qw * ty2 + (qz * tx2 - qx * tz2);
            float roz = oz + qw * tz2 + (qx * ty2 - qy * tx2);

            out.set(
                    part,
                    poses.translationX(part) + rox * poses.scaleX(part),
                    poses.translationY(part) + roy * poses.scaleY(part),
                    poses.translationZ(part) + roz * poses.scaleZ(part)
            );
        }
    }
}
