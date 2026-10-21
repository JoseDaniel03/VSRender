package net.momirealms.craftengine.core.render.runtime;

/** Platform edge. Implementations must consume the reusable batch synchronously. */
@FunctionalInterface
public interface RenderFrameSink {
    void publish(RenderFrameBatch batch);
}
