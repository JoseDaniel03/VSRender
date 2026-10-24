package com.virgolandstudio.items.core.world;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

/** Generic target bridge used by non-projectile world impacts. */
public interface EntityImpactBridge {
    boolean damage(ExecutionContext context, double amount);
    boolean knockback(ExecutionContext context, double horizontal, double vertical);
}
