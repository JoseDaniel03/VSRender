package com.virgolandstudio.items.api.consumable;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.Objects;
import java.util.UUID;

/** Provider-neutral request emitted by Items before committing a consumable use. */
public record ConsumableEffectRequest(
        UUID actorId,
        UUID itemInstanceId,
        NamespacedId itemDefinitionId,
        String route,
        String effectProfile,
        double doseAmount,
        String doseUnit,
        String substanceId,
        long durationMillis
) {
    public ConsumableEffectRequest {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(itemInstanceId, "itemInstanceId");
        Objects.requireNonNull(itemDefinitionId, "itemDefinitionId");
        route = normalize(route, "USE");
        effectProfile = effectProfile == null ? "" : effectProfile.trim();
        if (!Double.isFinite(doseAmount) || doseAmount < 0.0) {
            throw new IllegalArgumentException("doseAmount must be finite and >= 0");
        }
        doseUnit = normalize(doseUnit, "dose");
        substanceId = substanceId == null ? "" : substanceId.trim();
        if (durationMillis < 0) throw new IllegalArgumentException("durationMillis must be >= 0");
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
