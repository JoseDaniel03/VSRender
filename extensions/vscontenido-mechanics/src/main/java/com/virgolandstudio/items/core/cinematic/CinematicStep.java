package com.virgolandstudio.items.core.cinematic;

import java.util.Objects;

/**
 * One step in a persistent camera sequence.
 *
 * <p>For MOVE, {@code continuous=true} means the target is a pass-through keyframe when another
 * MOVE follows. The runtime keeps velocity through that point instead of stopping/restarting.</p>
 */
public record CinematicStep(
        CinematicStepType type,
        CinematicPose target,
        int durationTicks,
        CinematicEase easing,
        boolean continuous
) {
    public CinematicStep {
        Objects.requireNonNull(type, "type");
        easing = easing == null ? CinematicEase.SMOOTHSTEP : easing;
        if (durationTicks < 0) throw new IllegalArgumentException("durationTicks must be >= 0");
        if (type == CinematicStepType.MOVE && target == null) throw new IllegalArgumentException("MOVE requires target");
    }

    public static CinematicStep move(CinematicPose target, int durationTicks, CinematicEase easing, boolean continuous) {
        return new CinematicStep(CinematicStepType.MOVE, target, Math.max(1, durationTicks), easing, continuous);
    }

    /** Pass through this keyframe without intentionally braking when a MOVE follows. */
    public static CinematicStep pass(CinematicPose target, int durationTicks) {
        return move(target, durationTicks, CinematicEase.LINEAR, true);
    }

    /** Arrive at this keyframe and settle there. */
    public static CinematicStep stop(CinematicPose target, int durationTicks, CinematicEase easing) {
        return move(target, durationTicks, easing, false);
    }

    public static CinematicStep hold(int durationTicks) {
        return new CinematicStep(CinematicStepType.HOLD, null, Math.max(1, durationTicks), CinematicEase.LINEAR, false);
    }

    public static CinematicStep interactive() {
        return new CinematicStep(CinematicStepType.INTERACTIVE, null, 0, CinematicEase.LINEAR, false);
    }
}
