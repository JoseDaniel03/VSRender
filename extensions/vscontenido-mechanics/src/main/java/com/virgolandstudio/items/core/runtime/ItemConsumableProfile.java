package com.virgolandstudio.items.core.runtime;

import java.util.Objects;

/** Provider-neutral consumable metadata resolved from composed Item components. */
public record ItemConsumableProfile(
        String route,
        String effectProfile,
        long usesPerConsumption,
        boolean removeWhenEmpty,
        long durationMillis,
        double doseAmount,
        String doseUnit,
        String substanceId
) {
    public ItemConsumableProfile {
        route = normalize(route, "USE").toUpperCase(java.util.Locale.ROOT);
        effectProfile = effectProfile == null ? "" : effectProfile.trim();
        if (usesPerConsumption <= 0) throw new IllegalArgumentException("usesPerConsumption must be > 0");
        if (durationMillis < 0) throw new IllegalArgumentException("durationMillis must be >= 0");
        if (!Double.isFinite(doseAmount) || doseAmount < 0.0) throw new IllegalArgumentException("doseAmount must be finite and >= 0");
        doseUnit = normalize(doseUnit, "dose");
        substanceId = substanceId == null ? "" : substanceId.trim();
    }

    public boolean hasEffectProfile() {
        return !effectProfile.isBlank();
    }

    public boolean hasSubstance() {
        return !substanceId.isBlank() && doseAmount > 0.0;
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
