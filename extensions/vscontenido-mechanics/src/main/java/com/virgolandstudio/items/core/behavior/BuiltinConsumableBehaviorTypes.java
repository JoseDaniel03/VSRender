package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.consumable.ConsumableEffectService;
import com.virgolandstudio.items.core.runtime.ExecutionContext;
import com.virgolandstudio.items.core.runtime.ItemConsumableService;
import com.virgolandstudio.items.core.runtime.ItemConsumableSnapshot;

import java.util.Map;
import java.util.UUID;

/** Generic consumable behavior primitives owned by Items. */
public final class BuiltinConsumableBehaviorTypes {
    private BuiltinConsumableBehaviorTypes() {}

    public static void registerAll(BehaviorCompiler compiler) {
        compiler.registerCondition("consumable_available", (config, c) -> context -> {
            ItemConsumableSnapshot snapshot = ItemConsumableService.inspect(context.requireInstance()).orElse(null);
            if (snapshot == null) {
                context.put("consumable.available", false);
                return false;
            }
            publish(context, snapshot);
            boolean available = !snapshot.depleted();
            context.put("consumable.available", available);
            return available;
        });

        compiler.registerAction("consume_consumable", (config, c) -> {
            boolean cancelOnFailure = bool(config.values(), "cancelOnFailure", true);
            return context -> {
                UUID actorId = actorId(context.get("actor.id"));
                ConsumableEffectService effects = context.service(ConsumableEffectService.class).orElse(null);
                ItemConsumableService.Result result = ItemConsumableService.consume(
                        context.requireInstance(), actorId, effects
                );

                context.put("consumable.status", result.status().name());
                context.put("consumable.applied", result.applied());
                context.put("consumable.effect_detail", result.effectDetail());
                if (result.snapshot() != null) publish(context, result.snapshot());

                if (!result.applied() && cancelOnFailure) return ActionResult.CANCEL_TRIGGER;
                return ActionResult.CONTINUE;
            };
        });
    }

    private static void publish(ExecutionContext context, ItemConsumableSnapshot snapshot) {
        var profile = snapshot.profile();
        context.put("consumable.remaining", snapshot.usesRemaining());
        context.put("consumable.depleted", snapshot.depleted());
        context.put("consumable.route", profile.route());
        context.put("consumable.effect_profile", profile.effectProfile());
        context.put("consumable.uses_per_consumption", profile.usesPerConsumption());
        context.put("consumable.remove_when_empty", profile.removeWhenEmpty());
        context.put("consumable.duration_ms", profile.durationMillis());
        context.put("consumable.dose_amount", profile.doseAmount());
        context.put("consumable.dose_unit", profile.doseUnit());
        context.put("consumable.substance_id", profile.substanceId());
    }

    private static UUID actorId(Object raw) {
        if (raw instanceof UUID id) return id;
        if (raw == null) return null;
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
