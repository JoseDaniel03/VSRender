package net.momirealms.craftengine.core.render.attachment;

/** Coordinate space used by a provider-independent render transform snapshot. */
public enum RenderTransformSpace {
    /** Position/rotation are relative to the owning render instance anchor. */
    INSTANCE_LOCAL,
    /** Position/rotation are already expressed in world space. */
    WORLD
}
