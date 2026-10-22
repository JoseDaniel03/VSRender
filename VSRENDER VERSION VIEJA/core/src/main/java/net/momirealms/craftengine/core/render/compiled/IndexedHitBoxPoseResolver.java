package net.momirealms.craftengine.core.render.compiled;

import java.util.Objects;

/** Resolves hitbox centers from the already-resolved rig pose without platform dependencies. */
public final class IndexedHitBoxPoseResolver {
    private IndexedHitBoxPoseResolver() {
    }

    /**
     * Resolves BetterModel-compatible hitbox centers in rig instance space.
     *
     * <p>The local center offset is rotated by the resolved bone rotation, then scaled by the
     * resolved bone scale before being added to the resolved bone translation. Hitbox extents are
     * deliberately not multiplied by animation scale; owner/platform scale is applied later.</p>
     */
    public static void resolve(IndexedRenderRig rig, RenderPoseBuffer poses, RenderHitBoxBuffer out) {
        Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(poses, "poses");
        Objects.requireNonNull(out, "out");
        if (poses.rig() != rig || out.rig() != rig) {
            throw new IllegalArgumentException("rig/buffer identity mismatch");
        }

        out.beginFrame();
        for (int part : rig.hitBoxIndices()) {
            float ox = rig.hitBoxCenterX(part);
            float oy = rig.hitBoxCenterY(part);
            float oz = rig.hitBoxCenterZ(part);
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

            out.setCenter(
                    part,
                    poses.translationX(part) + rox * poses.scaleX(part),
                    poses.translationY(part) + roy * poses.scaleY(part),
                    poses.translationZ(part) + roz * poses.scaleZ(part)
            );
        }
    }
}
