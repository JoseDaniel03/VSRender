package com.virgolandstudio.items.core.cinematic;

import java.util.Objects;

/**
 * Tick-driven persistent camera timeline.
 *
 * <p>The camera is never detached between steps. MOVE segments always start from the actual pose
 * produced by the previous frame. Consecutive pass-through MOVE keyframes use a cubic Hermite
 * curve with matching tangents, so the camera does not brake/re-accelerate at every authored
 * point. Tiny redundant MOVE corrections are skipped entirely.</p>
 */
public final class CinematicRuntime {
    private static final double NOOP_POSITION_EPSILON = 0.015;
    private static final float NOOP_ANGLE_EPSILON = 0.60f;

    private CinematicSequence sequence;
    private CinematicPose pose;
    private CinematicPose stepStartPose;
    private CinematicPose previousMoveStartPose;
    private int stepIndex;
    private int stepTick;
    private boolean running;
    private boolean interactive;
    private boolean previousMovePassedThrough;

    public void start(CinematicSequence sequence, CinematicPose initialPose) {
        this.sequence = Objects.requireNonNull(sequence, "sequence");
        this.pose = Objects.requireNonNull(initialPose, "initialPose");
        this.stepStartPose = initialPose;
        this.previousMoveStartPose = initialPose;
        this.stepIndex = 0;
        this.stepTick = 0;
        this.running = true;
        this.interactive = false;
        this.previousMovePassedThrough = false;
        prepareCurrentStep();
    }

    public TickResult tick() {
        if (!running) return new TickResult(pose, false, true);
        CinematicStep step = sequence.steps().get(stepIndex);
        boolean changed = false;

        switch (step.type()) {
            case MOVE -> {
                stepTick++;
                double raw = clamp((double) stepTick / (double) Math.max(1, step.durationTicks()));
                CinematicPose nextTarget = nextMoveTargetForPass(stepIndex, step.target());
                boolean passThrough = step.continuous() && nextTarget != null;

                if (previousMovePassedThrough || passThrough) {
                    pose = hermiteMove(step.target(), nextTarget, passThrough, raw);
                } else {
                    pose = stepStartPose.lerp(step.target(), step.easing().apply(raw));
                }

                if (stepTick >= step.durationTicks()) {
                    pose = step.target();
                    changed = advanceStep(passThrough);
                }
            }
            case HOLD -> {
                stepTick++;
                if (stepTick >= step.durationTicks()) changed = advanceStep(false);
            }
            case INTERACTIVE -> interactive = true;
        }
        return new TickResult(pose, changed, !running);
    }

    public void resumeInteractive() {
        if (!running || !interactive) return;
        interactive = false;
        advanceStep(false);
    }

    public void cancel() {
        running = false;
        interactive = false;
    }

    private CinematicPose hermiteMove(CinematicPose target, CinematicPose nextTarget,
                                      boolean passThrough, double t) {
        CinematicPose a = stepStartPose;
        CinematicPose b = target;
        CinematicPose p0 = previousMovePassedThrough ? previousMoveStartPose : a;

        double m0x = previousMovePassedThrough ? 0.5 * (b.x() - p0.x()) : 0.0;
        double m0y = previousMovePassedThrough ? 0.5 * (b.y() - p0.y()) : 0.0;
        double m0z = previousMovePassedThrough ? 0.5 * (b.z() - p0.z()) : 0.0;
        double m1x = passThrough ? 0.5 * (nextTarget.x() - a.x()) : 0.0;
        double m1y = passThrough ? 0.5 * (nextTarget.y() - a.y()) : 0.0;
        double m1z = passThrough ? 0.5 * (nextTarget.z() - a.z()) : 0.0;

        double bxYaw = a.yaw() + CinematicPose.shortestAngleDelta(a.yaw(), b.yaw());
        double p0Yaw = a.yaw() + CinematicPose.shortestAngleDelta(a.yaw(), p0.yaw());
        double nextYaw = passThrough
                ? bxYaw + CinematicPose.shortestAngleDelta((float) bxYaw, nextTarget.yaw())
                : bxYaw;
        double m0Yaw = previousMovePassedThrough ? 0.5 * (bxYaw - p0Yaw) : 0.0;
        double m1Yaw = passThrough ? 0.5 * (nextYaw - a.yaw()) : 0.0;

        double m0Pitch = previousMovePassedThrough ? 0.5 * (b.pitch() - p0.pitch()) : 0.0;
        double m1Pitch = passThrough ? 0.5 * (nextTarget.pitch() - a.pitch()) : 0.0;

        return new CinematicPose(
                hermite(a.x(), b.x(), m0x, m1x, t),
                hermite(a.y(), b.y(), m0y, m1y, t),
                hermite(a.z(), b.z(), m0z, m1z, t),
                CinematicPose.normalizeYaw(hermite(a.yaw(), bxYaw, m0Yaw, m1Yaw, t)),
                (float) hermite(a.pitch(), b.pitch(), m0Pitch, m1Pitch, t)
        );
    }

    private boolean advanceStep(boolean movePassedThrough) {
        CinematicStep completed = sequence.steps().get(stepIndex);
        if (completed.type() == CinematicStepType.MOVE) {
            previousMoveStartPose = stepStartPose;
            previousMovePassedThrough = movePassedThrough;
        } else {
            previousMovePassedThrough = false;
        }

        stepIndex++;
        stepTick = 0;
        stepStartPose = pose; // actual previous-frame pose: continuity invariant
        if (stepIndex >= sequence.steps().size()) {
            running = false;
            interactive = false;
            return true;
        }
        prepareCurrentStep();
        return true;
    }

    /**
     * Enters the next meaningful step. MOVE targets already within visual tolerance are discarded
     * without snapping the camera to their authored coordinates.
     */
    private void prepareCurrentStep() {
        while (running && stepIndex < sequence.steps().size()) {
            CinematicStep step = sequence.steps().get(stepIndex);
            stepStartPose = pose;
            stepTick = 0;
            interactive = step.type() == CinematicStepType.INTERACTIVE;
            if (step.type() != CinematicStepType.MOVE
                    || !pose.nearlyEquals(step.target(), NOOP_POSITION_EPSILON, NOOP_ANGLE_EPSILON)) {
                return;
            }
            // A skipped MOVE must not create a fake tangent at the next keyframe.
            previousMovePassedThrough = false;
            stepIndex++;
        }
        if (stepIndex >= sequence.steps().size()) {
            running = false;
            interactive = false;
        }
    }

    /**
     * Returns the next distinct MOVE target only while there is an uninterrupted MOVE chain.
     * Duplicate authored points are ignored, eliminating tiny detours between equivalent anchors.
     */
    private CinematicPose nextMoveTargetForPass(int index, CinematicPose currentTarget) {
        for (int i = index + 1; i < sequence.steps().size(); i++) {
            CinematicStep next = sequence.steps().get(i);
            if (next.type() != CinematicStepType.MOVE) return null;
            if (currentTarget.nearlyEquals(next.target(), NOOP_POSITION_EPSILON, NOOP_ANGLE_EPSILON)) continue;
            return next.target();
        }
        return null;
    }

    private static double hermite(double p0, double p1, double m0, double m1, double t) {
        double tt = clamp(t);
        double t2 = tt * tt;
        double t3 = t2 * tt;
        double h00 = 2.0 * t3 - 3.0 * t2 + 1.0;
        double h10 = t3 - 2.0 * t2 + tt;
        double h01 = -2.0 * t3 + 3.0 * t2;
        double h11 = t3 - t2;
        return h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
    }

    private static double clamp(double t) { return Math.max(0.0, Math.min(1.0, t)); }

    public boolean running() { return running; }
    public boolean interactive() { return interactive; }
    public int stepIndex() { return stepIndex; }
    public CinematicPose pose() { return pose; }
    public CinematicSequence sequence() { return sequence; }

    public record TickResult(CinematicPose pose, boolean stepChanged, boolean complete) {}
}
