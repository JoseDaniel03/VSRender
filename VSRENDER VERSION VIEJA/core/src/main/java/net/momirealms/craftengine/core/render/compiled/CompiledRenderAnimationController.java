package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationController;
import net.momirealms.craftengine.core.render.animation.RenderAnimationKey;
import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.animation.RenderAnimationSession;
import net.momirealms.craftengine.core.render.runtime.CompiledRenderRuntime;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Native animation-session owner backed by one {@link CompiledRenderRuntime}.
 * Scheduling stays external; the owner must invoke {@link #afterTick()} after advancing the runtime.
 */
public final class CompiledRenderAnimationController implements RenderAnimationController {
    private static final String BASE_LAYER = "vsrender:base";
    private static final String TRANSIENT_LAYER = "vsrender:transient";
    private static final String TERMINAL_LAYER = "vsrender:terminal";
    private static final int DEFAULT_PRIORITY = 0;

    private final String rigId;
    private final CompiledRenderRuntime runtime;
    private final Set<String> animations;
    private boolean closed;
    private Session active;
    private TransientEvent transientEvent;
    private TerminalEvent terminalEvent;

    public CompiledRenderAnimationController(String rigId, CompiledRenderRuntime runtime) {
        this.rigId = Objects.requireNonNull(rigId, "rigId");
        if (rigId.isBlank()) throw new IllegalArgumentException("rigId must not be blank");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (IndexedAnimationClip clip : runtime.state().asset().animations()) ids.add(clip.animationId());
        this.animations = Collections.unmodifiableSet(ids);
    }

    @Override
    public String rigId() {
        return rigId;
    }

    @Override
    public Set<String> animations() {
        return animations;
    }

    @Override
    public synchronized Optional<RenderAnimationSession> open(RenderAnimationKey key, RenderAnimationMode mode) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(mode, "mode");
        if (closed || terminalEvent != null || !supports(key)) return Optional.empty();
        return Optional.of(new Session(key.animationId(), mode));
    }

    /**
     * Starts/restarts a transient ONCE event animation. Override clips are routed through the indexed
     * per-part stack, so only their tracked subtrees preempt the base layer; non-override clips still
     * claim the full rig. Reusing the transient layer intentionally discards the previous event.
     */
    public synchronized boolean triggerFullRigEventOnce(String animationId) {
        Objects.requireNonNull(animationId, "animationId");
        if (closed || terminalEvent != null) return false;
        IndexedAnimationClip clip = runtime.state().asset().animationOrNull(animationId);
        if (clip == null) return false;

        Session suspended = transientEvent == null ? active : transientEvent.suspended;
        if (!runtime.putLayer(TRANSIENT_LAYER, animationId, RenderAnimationMode.ONCE, DEFAULT_PRIORITY)) {
            return false;
        }
        transientEvent = new TransientEvent(animationId, suspended);
        return true;
    }

    /**
     * Starts a terminal full-rig ONCE event. A terminal event permanently owns playback until it
     * completes or this controller is closed. It never restores a suspended/base session.
     *
     * <p>Repeated requests for the same terminal animation are idempotent and do not restart the
     * clock. Override/subtree clips use the same indexed selection rules as transient events; parts
     * outside the terminal subtree may keep evaluating lower layers until terminal cleanup closes the
     * instance.</p>
     */
    public boolean triggerTerminalFullRigEventOnce(String animationId, Runnable onFinish) {
        Objects.requireNonNull(animationId, "animationId");
        Objects.requireNonNull(onFinish, "onFinish");
        synchronized (this) {
            if (closed) return false;
            if (terminalEvent != null) return terminalEvent.animationId.equals(animationId);
            IndexedAnimationClip clip = runtime.state().asset().animationOrNull(animationId);
            if (clip == null) return false;
            if (!runtime.putLayer(TERMINAL_LAYER, animationId, RenderAnimationMode.ONCE, DEFAULT_PRIORITY)) {
                return false;
            }

            // A terminal event never resumes the previous owner. The instance lifecycle owns final
            // cleanup, so public sessions are invalidated instead of pretending they completed. Their
            // lower layer may remain physically present so an override death matches BetterModel on
            // unaffected bones until the terminal layer completes and closes the whole instance.
            Session session = active;
            active = null;
            if (session != null) session.closed = true;
            transientEvent = null;
            terminalEvent = new TerminalEvent(animationId, onFinish);
            return true;
        }
    }

    public synchronized boolean terminalEventActive() {
        return terminalEvent != null;
    }

    /**
     * Completion hook invoked by the platform owner after the runtime has advanced.
     * Transient events are resolved before the public session lifecycle.
     */
    public void afterTick() {
        Runnable finish = null;
        synchronized (this) {
            if (closed) return;
            if (terminalEvent != null) {
                if (terminalEvent.finished || !runtime.layerCompleted(TERMINAL_LAYER)) return;
                TerminalEvent event = terminalEvent;
                event.finished = true;
                finish = event.onFinish;
            } else if (transientEvent != null) {
                if (!runtime.layerCompleted(TRANSIENT_LAYER)) return;
                TransientEvent event = transientEvent;
                transientEvent = null;
                runtime.removeLayer(TRANSIENT_LAYER);
                Session suspended = event.suspended;
                if (suspended != null && active == suspended && !suspended.closed) {
                    if (suspended.mode == RenderAnimationMode.ONCE && runtime.layerCompleted(BASE_LAYER)) {
                        active = null;
                        if (!suspended.finished) {
                            suspended.finished = true;
                            finish = suspended.onFinish;
                        }
                        runtime.removeLayer(BASE_LAYER);
                    }
                }
            } else {
                Session session = active;
                if (session == null || session.mode != RenderAnimationMode.ONCE
                        || session.finished || !runtime.layerCompleted(BASE_LAYER)) return;
                session.finished = true;
                finish = session.onFinish;
            }
        }
        if (finish != null) finish.run();
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        Session session = active;
        active = null;
        transientEvent = null;
        terminalEvent = null;
        if (session != null) session.closed = true;
        runtime.stop();
    }

    private final class TerminalEvent {
        private final String animationId;
        private final Runnable onFinish;
        private boolean finished;

        private TerminalEvent(String animationId, Runnable onFinish) {
            this.animationId = animationId;
            this.onFinish = onFinish;
        }
    }

    private final class TransientEvent {
        private final String animationId;
        private Session suspended;

        private TransientEvent(String animationId, Session suspended) {
            this.animationId = animationId;
            this.suspended = suspended;
        }
    }

    private final class Session implements RenderAnimationSession {
        private final String animationId;
        private final RenderAnimationMode mode;
        private boolean started;
        private boolean closed;
        private boolean finished;
        private Runnable onFinish;

        private Session(String animationId, RenderAnimationMode mode) {
            this.animationId = animationId;
            this.mode = mode;
        }

        @Override
        public boolean start(Runnable onFinish) {
            Objects.requireNonNull(onFinish, "onFinish");
            boolean completeImmediately;
            synchronized (CompiledRenderAnimationController.this) {
                if (closed || CompiledRenderAnimationController.this.closed || terminalEvent != null
                        || started || active != null) return false;
                if (transientEvent != null) transientEvent = null;
                if (!runtime.putLayer(BASE_LAYER, animationId, mode, DEFAULT_PRIORITY)) return false;
                this.onFinish = onFinish;
                started = true;
                active = this;
                completeImmediately = mode == RenderAnimationMode.ONCE && runtime.layerCompleted(BASE_LAYER);
                if (completeImmediately) finished = true;
            }
            if (completeImmediately) onFinish.run();
            return true;
        }

        @Override
        public void close() {
            synchronized (CompiledRenderAnimationController.this) {
                if (closed) return;
                closed = true;
                if (active == this) {
                    active = null;
                    runtime.removeLayer(BASE_LAYER);
                    if (transientEvent != null && transientEvent.suspended == this) {
                        transientEvent.suspended = null;
                    }
                }
            }
        }
    }
}
