package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.core.instance.ItemInstance;

import java.util.Optional;

/**
 * Provider-neutral bridge used by item behaviors to request temporary visual changes.
 * Implementations own only transient per-instance overrides; they must not expose Media,
 * BetterModel or any other concrete visual provider to Items Core.
 */
public interface ItemVisualActionBridge {
    void setVariant(ItemInstance instance, String variant, long durationTicks);

    void setAnimation(ItemInstance instance, String animation, long durationTicks);

    void clear(ItemInstance instance);

    Optional<VisualOverride> current(ItemInstance instance);

    record VisualOverride(String variant, String animation, long animationRevision) {
        public VisualOverride(String variant, String animation) {
            this(variant, animation, 0L);
        }

        public VisualOverride {
            variant = normalize(variant);
            animation = normalize(animation);
            animationRevision = Math.max(0L, animationRevision);
        }

        public boolean empty() {
            return variant.isEmpty() && animation.isEmpty();
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
