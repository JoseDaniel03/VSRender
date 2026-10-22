package net.momirealms.craftengine.core.render.instance;

/** Provider capable of materializing a concrete render instance. */
public interface RenderInstanceProvider {
    String providerId();

    /** Returns null when this provider cannot materialize the requested model. */
    RenderInstance createInstance(RenderInstanceRequest request);
}
