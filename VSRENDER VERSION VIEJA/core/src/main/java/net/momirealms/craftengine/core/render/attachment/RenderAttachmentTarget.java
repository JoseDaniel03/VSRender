package net.momirealms.craftengine.core.render.attachment;

/**
 * Destination for attachment poses.
 * The target decides which coordinate spaces it can consume; the executor never invents conversions.
 */
public interface RenderAttachmentTarget {

    boolean supports(RenderTransformSpace space);

    /** Returns true when the transform was accepted/applied. */
    boolean apply(RenderPartTransform transform);
}
