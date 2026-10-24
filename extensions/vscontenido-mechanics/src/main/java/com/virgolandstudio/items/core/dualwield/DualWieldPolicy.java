package com.virgolandstudio.items.core.dualwield;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Provider-neutral rules for pairing two independently stateful item instances.
 *
 * <p>The policy does not own ammo, reload or visuals. It only decides whether two authored
 * definitions may operate as a pair and exposes the penalties/policy a platform coordinator
 * should apply while both hands are active.</p>
 */
public record DualWieldPolicy(
        Set<String> compatibilityTags,
        PairingMode pairingMode,
        ReloadPolicy reloadPolicy,
        double spreadMultiplier,
        double recoilMultiplier
) {
    public enum PairingMode { SAME_TAG, ANY_DUAL_WIELD }
    public enum ReloadPolicy { SEQUENTIAL, ACTIVE_HAND_ONLY }

    public DualWieldPolicy {
        compatibilityTags = compatibilityTags == null
                ? Set.of()
                : compatibilityTags.stream()
                        .filter(Objects::nonNull)
                        .map(DualWieldPolicy::normalizeTag)
                        .filter(tag -> !tag.isEmpty())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
        pairingMode = pairingMode == null ? PairingMode.SAME_TAG : pairingMode;
        reloadPolicy = reloadPolicy == null ? ReloadPolicy.SEQUENTIAL : reloadPolicy;
        spreadMultiplier = positiveOrOne(spreadMultiplier);
        recoilMultiplier = positiveOrOne(recoilMultiplier);
    }

    public boolean compatibleWith(DualWieldPolicy other) {
        if (other == null) return false;
        if (pairingMode == PairingMode.ANY_DUAL_WIELD && other.pairingMode == PairingMode.ANY_DUAL_WIELD) {
            return true;
        }
        for (String tag : compatibilityTags) {
            if (other.compatibilityTags.contains(tag)) return true;
        }
        return false;
    }

    private static String normalizeTag(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static double positiveOrOne(double value) {
        return Double.isFinite(value) && value > 0.0 ? value : 1.0;
    }
}
