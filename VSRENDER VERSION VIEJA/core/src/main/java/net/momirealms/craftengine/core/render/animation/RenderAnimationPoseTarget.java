package net.momirealms.craftengine.core.render.animation;

import java.util.Map;

/** Destination for one provider-independent sampled animation pose. */
@FunctionalInterface
public interface RenderAnimationPoseTarget {

    /** Returns true when the sampled pose was accepted/applied. */
    boolean apply(Map<String, RenderAnimationSample> samples);
}
