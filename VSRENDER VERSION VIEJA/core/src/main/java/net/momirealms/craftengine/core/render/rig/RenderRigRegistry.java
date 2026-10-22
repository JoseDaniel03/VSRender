package net.momirealms.craftengine.core.render.rig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic registry for provider-independent rig discovery.
 * Provider callbacks execute outside the registry monitor.
 */
public final class RenderRigRegistry {
    private final Map<String, RenderRigProvider> providers = new LinkedHashMap<>();

    public synchronized void register(RenderRigProvider provider) {
        Objects.requireNonNull(provider, "provider");
        providers.put(normalize(provider.providerId()), provider);
    }

    public synchronized RenderRigProvider provider(String providerId) {
        return providers.get(normalize(providerId));
    }

    public synchronized List<RenderRigProvider> providers() {
        return List.copyOf(providers.values());
    }

    public Optional<RenderRig> resolve(RenderRigReference reference) {
        Objects.requireNonNull(reference, "reference");
        if (reference.providerId() != null) {
            RenderRigProvider provider;
            synchronized (this) {
                provider = providers.get(normalize(reference.providerId()));
            }
            return provider == null
                    ? Optional.empty()
                    : Optional.ofNullable(provider.resolveRig(reference.rigId(), reference.type()));
        }

        List<RenderRigProvider> snapshot;
        synchronized (this) {
            snapshot = List.copyOf(providers.values());
        }
        for (RenderRigProvider provider : snapshot) {
            RenderRig rig = provider.resolveRig(reference.rigId(), reference.type());
            if (rig != null) return Optional.of(rig);
        }
        return Optional.empty();
    }

    private static String normalize(String id) {
        Objects.requireNonNull(id, "providerId");
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("providerId must not be blank");
        return normalized;
    }
}
