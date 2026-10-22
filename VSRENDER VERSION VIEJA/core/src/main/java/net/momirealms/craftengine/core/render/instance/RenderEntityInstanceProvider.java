package net.momirealms.craftengine.core.render.instance;

import java.util.concurrent.CompletionStage;

/** Provider capable of materializing render instances anchored to a live entity. */
public interface RenderEntityInstanceProvider extends RenderInstanceProvider {
    /**
     * Returns a stage yielding null when this provider cannot materialize the request.
     * Provider callbacks must not require registry locks to remain held.
     */
    CompletionStage<RenderInstance> createEntityInstance(RenderEntityInstanceRequest request);
}
