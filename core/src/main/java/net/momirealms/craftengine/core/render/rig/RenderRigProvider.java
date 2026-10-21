package net.momirealms.craftengine.core.render.rig;

/** Provider boundary for exposing rig metadata to VsRender Core. */
public interface RenderRigProvider {
    String providerId();

    /** Returns an immutable rig snapshot, or null when the resource is unknown. */
    RenderRig resolveRig(String rigId, RenderRigType type);
}
