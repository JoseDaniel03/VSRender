package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

/** Platform bridge exposing the compiled ammo/feed runtime to declarative actions. */
public interface AmmoActionBridge {
    boolean fire(ExecutionContext context, boolean cycleAfterShot);
    boolean feedOne(ExecutionContext context);
    double available(ExecutionContext context);
    int capacity(ExecutionContext context);
    int chambered(ExecutionContext context);
}
