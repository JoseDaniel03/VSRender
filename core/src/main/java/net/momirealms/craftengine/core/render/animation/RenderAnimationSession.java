package net.momirealms.craftengine.core.render.animation;

/**
 * VsRender-owned lifecycle for one running visual animation.
 * Provider-specific trackers/resources stay hidden behind this contract.
 */
public interface RenderAnimationSession extends AutoCloseable {

    /**
     * Starts the animation and invokes {@code onFinish} when a finite animation completes.
     * Looping sessions may keep running until {@link #close()}.
     *
     * @return true when the provider accepted the animation
     */
    boolean start(Runnable onFinish);

    /** Releases only resources owned by this session. Must be safe to call during shutdown. */
    @Override
    void close();
}
