package net.momirealms.craftengine.core.render.animation;

import java.util.Objects;

/**
 * Applies a native {@link RenderAnimationPlayback} to a pose target.
 *
 * <p>Playback owns time; this class owns only target-application lifecycle. It creates no
 * scheduler/thread and never owns/closes the target.</p>
 */
public final class RenderAnimationPlayer implements AutoCloseable {
    private final RenderAnimationPlayback playback;
    private final RenderAnimationPoseTarget target;
    private boolean finished;
    private boolean holding;
    private boolean closed;

    public RenderAnimationPlayer(RenderAnimationClip clip, RenderAnimationMode mode, RenderAnimationPoseTarget target) {
        this.playback = new RenderAnimationPlayback(Objects.requireNonNull(clip, "clip"), Objects.requireNonNull(mode, "mode"));
        this.target = Objects.requireNonNull(target, "target");
    }

    public RenderAnimationPlayer(RenderAnimationClip clip, RenderAnimationPoseTarget target) {
        this(clip, clip.defaultMode(), target);
    }

    public RenderAnimationClip clip() {
        return playback.clip();
    }

    public RenderAnimationMode mode() {
        return playback.mode();
    }

    public synchronized float timeSeconds() {
        return playback.timeSeconds();
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized boolean isHolding() {
        return holding;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Advances playback and applies the resulting pose once.
     * A rejected final pose is retried on the next tick rather than silently finishing.
     */
    public synchronized TickResult tick(float deltaSeconds) {
        if (closed) return TickResult.CLOSED;
        if (finished) return TickResult.FINISHED;
        if (holding) return TickResult.HOLDING;

        boolean accepted = target.apply(playback.advanceState(deltaSeconds));
        if (!accepted) return TickResult.REJECTED;

        if (playback.completed()) {
            finished = true;
            return TickResult.FINISHED;
        }
        if (playback.holding()) {
            holding = true;
            return TickResult.HOLDING;
        }
        return TickResult.APPLIED;
    }

    /** Rewinds playback while preserving the configured clip/mode/target. */
    public synchronized boolean reset() {
        if (closed) return false;
        playback.reset();
        finished = false;
        holding = false;
        return true;
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    public enum TickResult {
        APPLIED,
        FINISHED,
        HOLDING,
        REJECTED,
        CLOSED
    }
}
