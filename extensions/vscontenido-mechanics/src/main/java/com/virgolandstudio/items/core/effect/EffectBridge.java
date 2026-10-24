package com.virgolandstudio.items.core.effect;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

/** Platform-side rendering/audio bridge. Calls may be rejected by global budgets. */
public interface EffectBridge {
    boolean particle(
            ExecutionContext context,
            EffectOrigin origin,
            String particleId,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra
    );

    boolean sound(
            ExecutionContext context,
            EffectOrigin origin,
            String soundId,
            String category,
            double volume,
            double pitch
    );
}
