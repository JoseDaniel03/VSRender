package com.virgolandstudio.items.core.world;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

/** Platform bridge for authored entity manipulation such as gravity pulls. */
public interface EntityActionBridge {
    boolean canPull(ExecutionContext context);
    boolean pull(ExecutionContext context);
}
