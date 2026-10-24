package com.virgolandstudio.items.api.ammo;

/** How a container represents its content at runtime. */
public enum AmmoStorageMode {
    /** A discrete count, e.g. box/detachable/internal magazines and belts. */
    COUNT,
    /** Individually addressable positions, e.g. revolver cylinders and break-action chambers. */
    SLOTS,
    /** A continuous resource, e.g. an energy battery. */
    RESOURCE
}
