package net.momirealms.craftengine.core.render.instance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Deterministic registry for concrete render-instance factories. */
public final class RenderInstanceRegistry {
    private final Map<String, RenderInstanceProvider> providers = new LinkedHashMap<>();

    public synchronized void register(RenderInstanceProvider provider) {
        Objects.requireNonNull(provider, "provider");
        providers.put(normalize(provider.providerId()), provider);
    }

    public synchronized RenderInstanceProvider provider(String providerId) {
        return providers.get(normalize(providerId));
    }

    public synchronized List<RenderInstanceProvider> providers() {
        return List.copyOf(providers.values());
    }

    public Optional<RenderInstance> create(RenderInstanceRequest request) {
        Objects.requireNonNull(request, "request");
        RenderInstanceProvider provider;
        synchronized (this) {
            provider = providers.get(normalize(request.model().providerId()));
        }
        return provider == null ? Optional.empty() : Optional.ofNullable(provider.createInstance(request));
    }

    /** Resolves a live-entity render provider without invoking provider code under the registry lock. */
    public CompletionStage<Optional<RenderInstance>> createEntity(RenderEntityInstanceRequest request) {
        Objects.requireNonNull(request, "request");
        RenderInstanceProvider provider;
        synchronized (this) {
            provider = providers.get(normalize(request.model().providerId()));
        }
        if (!(provider instanceof RenderEntityInstanceProvider entityProvider)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        CompletionStage<RenderInstance> stage = entityProvider.createEntityInstance(request);
        if (stage == null) return CompletableFuture.completedFuture(Optional.empty());
        return stage.thenApply(Optional::ofNullable);
    }

    private static String normalize(String id) {
        Objects.requireNonNull(id, "providerId");
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("providerId must not be blank");
        return normalized;
    }
}
