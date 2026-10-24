package com.virgolandstudio.items.api.consumable;

/**
 * Actor-side provider for consumable effects.
 *
 * <p>Items owns the consumable transaction and calls this service only after validating the Item
 * has enough persisted uses. Implementations own actor state such as health, hydration, hunger,
 * intoxication or medicine. A rejected result must leave actor state unchanged.</p>
 */
@FunctionalInterface
public interface ConsumableEffectService {
    ConsumableEffectResult apply(ConsumableEffectRequest request);
}
