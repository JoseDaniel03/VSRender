package com.virgolandstudio.items.core.projectile;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

public interface ProjectileHitBridge {
    boolean damage(ExecutionContext context, double amount);
}
