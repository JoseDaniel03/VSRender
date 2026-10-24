package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.consumable.ConsumableEffectRequest;
import com.virgolandstudio.items.api.consumable.ConsumableEffectResult;
import com.virgolandstudio.items.api.consumable.ConsumableEffectService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ordered, ownership-safe dispatcher for actor-side consumable effects.
 *
 * <p>Providers may decline ownership with {@code UNHANDLED}. The first provider that applies or
 * rejects the request terminates routing. Provider failures are deliberately not swallowed so the
 * Item transaction can report {@code EFFECT_FAILED} without spending a dose.</p>
 */
public final class ConsumableEffectRouter implements ConsumableEffectService {
    private final List<ConsumableEffectService> providers;

    public ConsumableEffectRouter(List<? extends ConsumableEffectService> providers) {
        Objects.requireNonNull(providers, "providers");
        var copy = new ArrayList<ConsumableEffectService>(providers.size());
        for (ConsumableEffectService provider : providers) {
            copy.add(Objects.requireNonNull(provider, "provider"));
        }
        this.providers = List.copyOf(copy);
    }

    @Override
    public ConsumableEffectResult apply(ConsumableEffectRequest request) {
        ConsumableEffectRequest checked = Objects.requireNonNull(request, "request");
        for (ConsumableEffectService provider : providers) {
            ConsumableEffectResult result = provider.apply(checked);
            if (result == null) {
                throw new IllegalStateException("consumable effect provider returned null: " + provider.getClass().getName());
            }
            if (!result.handled()) continue;
            return result;
        }
        return ConsumableEffectResult.unhandled("no consumable effect provider handled the request");
    }

    public int providerCount() {
        return providers.size();
    }
}
