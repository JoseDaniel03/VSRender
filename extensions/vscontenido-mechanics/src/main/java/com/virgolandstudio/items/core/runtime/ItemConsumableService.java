package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.consumable.ConsumableEffectRequest;
import com.virgolandstudio.items.api.consumable.ConsumableEffectResult;
import com.virgolandstudio.items.api.consumable.ConsumableEffectService;
import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Atomic Item-owned consumable transaction.
 *
 * <p>The service owns item semantics: profile discovery, persisted uses and dose metadata.
 * Actor-side effects belong to a neutral {@link ConsumableEffectService}. When a profile requires
 * an actor effect, that provider is invoked after Item validation but before the use counter is
 * committed. Rejection/failure therefore never spends the Item dose.</p>
 */
public final class ItemConsumableService {
    private static final NamespacedId CONSUMABLE = NamespacedId.parse("virgoland:consumable");
    private static final NamespacedId DOSE = NamespacedId.parse("virgoland:dose");
    private static final NamespacedId SUBSTANCE = NamespacedId.parse("virgoland:substance");

    public enum Status {
        APPLIED,
        NOT_SUPPORTED,
        DISABLED,
        DEPLETED,
        INVALID_STATE,
        EFFECT_UNAVAILABLE,
        EFFECT_REJECTED,
        EFFECT_FAILED
    }

    private ItemConsumableService() {}

    public static Optional<ItemConsumableSnapshot> inspect(ItemInstance instance) {
        if (instance == null) return Optional.empty();
        synchronized (instance) {
            Resolved resolved = resolve(instance);
            if (resolved == null || !resolved.enabled()) return Optional.empty();
            Remaining remaining = remaining(instance);
            if (!remaining.valid()) return Optional.empty();
            long count = remaining.value();
            boolean depleted = count >= 0 && count < resolved.profile().usesPerConsumption();
            return Optional.of(new ItemConsumableSnapshot(instance.id(), resolved.profile(), count, depleted));
        }
    }

    /** Item-only commit. Useful for profiles whose actor effect is intentionally empty. */
    public static Result consume(ItemInstance instance) {
        return consumeLocked(instance, null, null, false);
    }

    /**
     * Actor-aware commit. If the profile declares an effect profile or substance, a provider is
     * mandatory and must accept/apply it before the Item use is decremented.
     */
    public static Result consume(ItemInstance instance, UUID actorId, ConsumableEffectService effects) {
        return consumeLocked(instance, actorId, effects, true);
    }

    private static Result consumeLocked(
            ItemInstance instance,
            UUID actorId,
            ConsumableEffectService effects,
            boolean requireActorEffects
    ) {
        if (instance == null) return Result.rejected(Status.NOT_SUPPORTED, null, "");
        synchronized (instance) {
            Resolved resolved = resolve(instance);
            if (resolved == null) return Result.rejected(Status.NOT_SUPPORTED, null, "");
            if (!resolved.enabled()) {
                return Result.rejected(Status.DISABLED, snapshot(instance, resolved.profile(), remaining(instance)), "");
            }

            Remaining before = remaining(instance);
            if (!before.valid()) return Result.rejected(Status.INVALID_STATE, null, "");
            long requested = resolved.profile().usesPerConsumption();
            if (before.value() >= 0 && before.value() < requested) {
                return Result.rejected(Status.DEPLETED, snapshot(instance, resolved.profile(), before), "");
            }

            ItemConsumableProfile profile = resolved.profile();
            boolean actorEffect = profile.hasEffectProfile() || profile.hasSubstance();
            String effectDetail = "";
            if (requireActorEffects && actorEffect) {
                if (actorId == null || effects == null) {
                    return Result.rejected(Status.EFFECT_UNAVAILABLE, snapshot(instance, profile, before), "effect provider unavailable");
                }
                ConsumableEffectResult effectResult;
                try {
                    effectResult = effects.apply(request(actorId, instance, profile));
                } catch (RuntimeException failure) {
                    return Result.rejected(Status.EFFECT_FAILED, snapshot(instance, profile, before), failure.getMessage());
                }
                if (effectResult == null) {
                    return Result.rejected(
                            Status.EFFECT_REJECTED,
                            snapshot(instance, profile, before),
                            "effect provider returned null"
                    );
                }
                if (!effectResult.handled()) {
                    return Result.rejected(
                            Status.EFFECT_UNAVAILABLE,
                            snapshot(instance, profile, before),
                            effectResult.detail()
                    );
                }
                if (!effectResult.accepted()) {
                    return Result.rejected(
                            Status.EFFECT_REJECTED,
                            snapshot(instance, profile, before),
                            effectResult.detail()
                    );
                }
                effectDetail = effectResult.detail();
            }

            // No failure path remains after the external effect has accepted: this deterministic
            // state write is protected by the instance monitor, so another Item operation cannot
            // race between effect acceptance and use commitment.
            if (before.slot() != null) {
                instance.state().set(before.slot(), before.value() - requested);
            }

            Remaining after = remaining(instance);
            ItemConsumableSnapshot snapshot = snapshot(instance, profile, after);
            return new Result(Status.APPLIED, true, snapshot, effectDetail);
        }
    }

    private static ConsumableEffectRequest request(UUID actorId, ItemInstance instance, ItemConsumableProfile profile) {
        return new ConsumableEffectRequest(
                actorId,
                instance.id(),
                instance.definition().source().id(),
                profile.route(),
                profile.effectProfile(),
                profile.doseAmount(),
                profile.doseUnit(),
                profile.substanceId(),
                profile.durationMillis()
        );
    }

    private static ItemConsumableSnapshot snapshot(ItemInstance instance, ItemConsumableProfile profile, Remaining remaining) {
        long value = remaining.valid() ? remaining.value() : -1L;
        boolean depleted = value >= 0 && value < profile.usesPerConsumption();
        return new ItemConsumableSnapshot(instance.id(), profile, value, depleted);
    }

    private static Resolved resolve(ItemInstance instance) {
        ComponentConfig consumable = component(instance, CONSUMABLE);
        if (consumable == null) return null;
        boolean enabled = bool(configured(consumable, "enabled"), true);

        ComponentConfig dose = component(instance, DOSE);
        ComponentConfig substance = component(instance, SUBSTANCE);

        double doseAmount = number(
                configured(dose, "amount"),
                number(configured(substance, "amount"), 1.0)
        );
        String doseUnit = text(
                configured(dose, "unit"),
                text(configured(substance, "unit"), "dose")
        );
        String substanceId = text(
                configured(substance, "id"),
                text(configured(substance, "substance"), "")
        );

        long usesPerConsumption = positiveLong(configured(consumable, "usesPerConsumption"), 1L);
        long durationMillis = nonNegativeLong(configured(consumable, "durationMillis"), 0L);
        ItemConsumableProfile profile;
        try {
            profile = new ItemConsumableProfile(
                    text(configured(consumable, "route"), "USE"),
                    text(configured(consumable, "effectProfile"), ""),
                    usesPerConsumption,
                    bool(configured(consumable, "removeWhenEmpty"), true),
                    durationMillis,
                    doseAmount,
                    doseUnit,
                    substanceId
            );
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return new Resolved(enabled, profile);
    }

    /**
     * Reads the current schema-driven top-level field first and falls back to the former generic
     * {@code config} map. This lets Studio author explicit fields without invalidating persisted
     * definitions created before the consumable schema became first-class.
     */
    private static Object configured(ComponentConfig component, String key) {
        if (component == null || key == null) return null;
        if (component.values().containsKey(key)) return component.values().get(key);
        return map(component.values().get("config")).get(key);
    }

    private static Remaining remaining(ItemInstance instance) {
        StateSlot slot = instance.state().layout().find("uses_remaining");
        if (slot == null) return new Remaining(null, -1L, true);
        if (slot.type() != StateType.INTEGER) return new Remaining(slot, 0L, false);
        return new Remaining(slot, Math.max(0L, instance.state().getLong(slot, 0L)), true);
    }

    private static ComponentConfig component(ItemInstance instance, NamespacedId type) {
        for (ComponentConfig component : instance.definition().source().components()) {
            if (component.type().equals(type)) return component;
        }
        return null;
    }

    private static Map<?, ?> map(Object raw) {
        return raw instanceof Map<?, ?> value ? value : Map.of();
    }

    private static boolean bool(Object raw, boolean fallback) {
        if (raw == null) return fallback;
        if (raw instanceof Boolean value) return value;
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    private static String text(Object raw, String fallback) {
        if (raw == null) return fallback;
        String text = String.valueOf(raw).trim();
        return text.isEmpty() ? fallback : text;
    }

    private static double number(Object raw, double fallback) {
        if (raw instanceof Number value) return value.doubleValue();
        if (raw == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long positiveLong(Object raw, long fallback) {
        long value = longValue(raw, fallback);
        return value > 0 ? value : fallback;
    }

    private static long nonNegativeLong(Object raw, long fallback) {
        long value = longValue(raw, fallback);
        return value >= 0 ? value : fallback;
    }

    private static long longValue(Object raw, long fallback) {
        if (raw instanceof Number value) return value.longValue();
        if (raw == null) return fallback;
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public record Result(Status status, boolean applied, ItemConsumableSnapshot snapshot, String effectDetail) {
        public Result {
            effectDetail = effectDetail == null ? "" : effectDetail;
        }

        private static Result rejected(Status status, ItemConsumableSnapshot snapshot, String effectDetail) {
            return new Result(status, false, snapshot, effectDetail);
        }
    }

    private record Resolved(boolean enabled, ItemConsumableProfile profile) {}
    private record Remaining(StateSlot slot, long value, boolean valid) {}
}
