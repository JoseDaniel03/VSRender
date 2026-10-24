package com.virgolandstudio.items.api.accessory;

import java.util.Locale;
import java.util.Objects;

/**
 * Data-only modifier authored by an accessory.
 *
 * Targets are intentionally string paths (for example "projectile.count" or
 * "fire.sound.volume") so new runtime consumers can be added without changing
 * the accessory file format.
 */
public record AccessoryModifierDefinition(
        String target,
        AccessoryModifierOperation operation,
        Object value
) {
    public AccessoryModifierDefinition {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(value, "value");
        target = normalizeTarget(target);
        if ((operation == AccessoryModifierOperation.ADD || operation == AccessoryModifierOperation.MULTIPLY)
                && !(value instanceof Number)) {
            throw new IllegalArgumentException(operation + " modifier requires a numeric value: " + target);
        }
    }

    private static String normalizeTarget(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("modifier target cannot be blank");
        return normalized;
    }
}
