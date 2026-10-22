package net.momirealms.craftengine.core.render.rig;

import java.util.Objects;

/** Provider-independent mount capability attached to a rig part. */
public record RenderMountPolicy(Mode mode, boolean canDismountBySelf, boolean canJump, boolean canBeDamagedByRider) {
    public static final RenderMountPolicy DISABLED = new RenderMountPolicy(Mode.DISABLED, true, true, false);
    public static final RenderMountPolicy PASSENGER_ONLY = new RenderMountPolicy(Mode.PASSENGER_ONLY, true, true, false);
    public static final RenderMountPolicy WALK = new RenderMountPolicy(Mode.WALK, true, true, false);
    public static final RenderMountPolicy FLY = new RenderMountPolicy(Mode.FLY, true, true, false);

    public RenderMountPolicy {
        Objects.requireNonNull(mode, "mode");
    }

    public boolean canMount() { return mode != Mode.DISABLED && mode != Mode.UNSUPPORTED; }
    public boolean canControl() { return mode == Mode.WALK || mode == Mode.FLY; }
    public boolean canFly() { return mode == Mode.FLY; }
    public boolean supported() { return mode != Mode.UNSUPPORTED; }

    public enum Mode {
        DISABLED,
        PASSENGER_ONLY,
        WALK,
        FLY,
        /** External/custom movement function cannot be represented by the native v1 controller. */
        UNSUPPORTED
    }
}
