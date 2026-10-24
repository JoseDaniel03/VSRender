package com.virgolandstudio.items.core.resource;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

/** Runtime energy service. Implementations may use lazy timestamp regeneration. */
public interface EnergyBridge {
    double current(ExecutionContext context);
    boolean has(ExecutionContext context, double amount);
    boolean consume(ExecutionContext context, double amount);
}
