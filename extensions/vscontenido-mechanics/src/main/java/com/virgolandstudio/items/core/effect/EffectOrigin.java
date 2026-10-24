package com.virgolandstudio.items.core.effect;

import java.util.Locale;

public enum EffectOrigin {
    ACTOR,
    HIT,
    PROJECTILE;

    public static EffectOrigin parse(String raw) {
        if (raw == null || raw.isBlank()) return HIT;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported effect origin: " + raw);
        }
    }
}
