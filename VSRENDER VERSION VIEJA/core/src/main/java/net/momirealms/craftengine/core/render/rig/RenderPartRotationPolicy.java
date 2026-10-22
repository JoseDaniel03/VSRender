package net.momirealms.craftengine.core.render.rig;

/**
 * Provider-independent dynamic rotation policy for one rig part.
 * The policy describes how owner rotation modifiers enter the rig; platform/provider tags stay at the adapter edge.
 */
public enum RenderPartRotationPolicy {
    NONE(false, false),
    OWNER_HEAD(true, false),
    OWNER_HEAD_WITH_DESCENDANTS(true, true);

    private final boolean ownerHead;
    private final boolean descendants;

    RenderPartRotationPolicy(boolean ownerHead, boolean descendants) {
        this.ownerHead = ownerHead;
        this.descendants = descendants;
    }

    public boolean ownerHead() {
        return ownerHead;
    }

    public boolean includeDescendants() {
        return descendants;
    }
}
