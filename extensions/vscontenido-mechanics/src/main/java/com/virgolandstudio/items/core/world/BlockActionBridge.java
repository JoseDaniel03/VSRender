package com.virgolandstudio.items.core.world;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

/** Platform bridge for block grab/hold/throw world mutations. */
public interface BlockActionBridge {
    boolean isHolding(ExecutionContext context);
    boolean canGrab(ExecutionContext context);
    boolean grab(ExecutionContext context);
    boolean throwHeld(ExecutionContext context);
    boolean releaseHeld(ExecutionContext context);
}
