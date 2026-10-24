package com.virgolandstudio.items.api.ammo;

/** High-level reload resource strategy. Exact timing remains authored as a ReloadSequence. */
public enum ReloadMechanismKind {
    SWAP_CONTAINER,
    TOP_OFF,
    PER_SLOT,
    REPLACE_RESOURCE,
    CUSTOM
}
