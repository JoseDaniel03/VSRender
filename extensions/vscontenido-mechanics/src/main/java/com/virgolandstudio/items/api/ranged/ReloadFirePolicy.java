package com.virgolandstudio.items.api.ranged;

/** Defines what a weapon does when FIRE is requested during an active reload. */
public enum ReloadFirePolicy {
    /** Ignore the fire request and let the reload continue. */
    BLOCK,
    /** Cancel the active reload and immediately allow the fire request to proceed. */
    INTERRUPT
}
