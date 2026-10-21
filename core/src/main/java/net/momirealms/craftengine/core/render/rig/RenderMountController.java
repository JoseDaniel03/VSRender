package net.momirealms.craftengine.core.render.rig;

import java.util.Objects;

/**
 * Pure-Core movement policy matching BetterModel's built-in WALK/FLY mount controllers.
 * Physics application, friction, gravity and jump impulses remain platform-owned.
 */
public final class RenderMountController {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private RenderMountController() {
    }

    public static RenderMountMotion resolve(RenderMountPolicy policy, RenderMountInput input,
                                            float movementSpeed, float riderYawDegrees, boolean flying) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(input, "input");
        if (!Float.isFinite(movementSpeed) || movementSpeed < 0f) {
            throw new IllegalArgumentException("movementSpeed must be finite and >= 0");
        }
        if (!Float.isFinite(riderYawDegrees)) throw new IllegalArgumentException("riderYawDegrees must be finite");
        if (!policy.canControl()) return RenderMountMotion.ZERO;

        // BetterModel normalizes the full rider input first. WALK then clears Y, while FLY keeps it.
        float x = input.strafe();
        float y = input.vertical();
        float z = input.forward();
        float lenSq = x * x + y * y + z * z;
        if (lenSq > 1e-6f) {
            float inv = (float) (1d / Math.sqrt(lenSq));
            x *= inv;
            y *= inv;
            z *= inv;
        }
        if (!policy.canFly()) y = 0f;
        x *= 0.5f;
        if (z <= 0f) z *= 0.25f;

        float multiplier = movementSpeed * (flying ? 1.5f : 1f);
        x *= multiplier;
        y *= multiplier;
        z *= multiplier;

        float yaw = -riderYawDegrees * DEG_TO_RAD;
        float c = (float) Math.cos(yaw);
        float s = (float) Math.sin(yaw);
        return new RenderMountMotion(x * c + z * s, y, -x * s + z * c);
    }

    public static boolean shouldJump(RenderMountPolicy policy, RenderMountInput input,
                                     boolean horizontalCollision, float verticalVelocity,
                                     int jumpCooldownTicks) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(input, "input");
        if (!policy.canControl() || !policy.canJump()) return false;
        return (horizontalCollision || input.jump())
                && verticalVelocity >= 0f && verticalVelocity <= 0.01f
                && jumpCooldownTicks <= 0;
    }
}
