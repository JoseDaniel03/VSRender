package net.momirealms.craftengine.core.render.rig;

/**
 * Provider-independent nametag visibility semantics attached to one rig part.
 * NONE means the part has no native nametag surface.
 */
public enum RenderNameTagMode {
    NONE,
    /** BetterModel TAG: always visible for player owners, focus-gated for other entities. */
    TAG,
    /** BetterModel MOB_TAG: focus-gated custom name. */
    MOB_TAG,
    /** BetterModel PLAYER_TAG: always visible custom name. */
    PLAYER_TAG
}
