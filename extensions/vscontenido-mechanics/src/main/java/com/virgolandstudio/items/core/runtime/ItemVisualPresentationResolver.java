package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.core.compile.CompiledItemDefinition;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.registry.BuiltinVisualSchemas;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Merges the authored base visual, logical visual_state and temporary behavior overrides.
 * The result contains only neutral ids/keys; provider-specific rendering belongs to adapters.
 */
public final class ItemVisualPresentationResolver {
    public static final String RUNTIME_ANIMATION_REVISION = "_runtimeAnimationRevision";

    private final ItemVisualStateResolver stateResolver = new ItemVisualStateResolver();

    public Optional<ItemVisualPresentation> resolve(
            CompiledItemDefinition definition,
            ItemInstance instance,
            String context,
            ItemVisualActionBridge.VisualOverride override
    ) {
        String normalizedContext = normalizeContext(context);
        ComponentConfig visual = visualComponent(definition);
        Map<String, Object> values = visual == null ? Map.of() : visual.values();
        Map<String, Object> merged = new LinkedHashMap<>();

        // Layer 1: the authored visual for this render context.
        Object contexts = values.get("contexts");
        if (contexts instanceof Map<?, ?> contextMap) {
            mergePayload(merged, contextMap.get(normalizedContext));
        }

        // Layer 2: logical visual_state. A named variant/animation replaces the base visual,
        // then any explicit fields carried by the state payload win over the named preset.
        if (instance != null) {
            stateResolver.resolve(instance).ifPresent(state -> {
                Map<String, Object> statePayload = scopedPayload(state.payload(), normalizedContext);
                String variant = string(statePayload.get("variant"));
                String animation = string(statePayload.get("animation"));
                mergeNamedPayload(merged, values.get("variants"), variant);
                mergeNamedPayload(merged, values.get("animations"), animation);
                mergePayload(merged, statePayload);
            });
        }

        // Layer 3: behavior overrides are deliberately last. These are transient runtime requests
        // such as FIRE -> firing or RELOAD -> reloading and must beat both base and state visuals.
        if (override != null) {
            if (!override.variant().isBlank()) {
                mergeNamedPayload(merged, values.get("variants"), override.variant());
                merged.put("variant", override.variant());
            }
            if (!override.animation().isBlank()) {
                mergeNamedPayload(merged, values.get("animations"), override.animation());
                merged.put("animation", override.animation());
                merged.put(RUNTIME_ANIMATION_REVISION, override.animationRevision());
            }
        }

        if (merged.isEmpty()) return Optional.empty();

        String type = string(merged.get("type"));
        String itemModel = string(merged.get("itemModel"));
        String reference = string(merged.get("reference"));
        String provider = string(merged.get("provider"));
        String modelId = string(merged.get("modelId"));
        String animation = string(merged.get("animation"));

        if (type.isBlank()) {
            if (!modelId.isBlank()) type = "MODEL_3D";
            else if (!itemModel.isBlank() || !reference.isBlank()) type = "SPRITE_2D";
            else type = "VANILLA";
        }
        type = type.trim().toUpperCase(Locale.ROOT);

        Map<String, Object> transform = map(merged.get("transform"));
        return Optional.of(new ItemVisualPresentation(
                type,
                reference,
                itemModel,
                provider,
                modelId,
                animation,
                transform,
                merged
        ));
    }

    private static ComponentConfig visualComponent(CompiledItemDefinition definition) {
        if (definition == null) return null;
        return definition.source().components().stream()
                .filter(component -> component.type().equals(BuiltinVisualSchemas.VISUAL))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Object> scopedPayload(Map<String, Object> payload, String context) {
        if (payload == null || payload.isEmpty()) return Map.of();
        Object scoped = payload.get("contexts");
        if (scoped instanceof Map<?, ?> contexts && contexts.get(context) != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            mergePayload(result, contexts.get(context));
            return result;
        }
        return canonicalMap(payload);
    }

    private static void mergeNamedPayload(Map<String, Object> target, Object rawCatalog, String name) {
        if (name == null || name.isBlank() || !(rawCatalog instanceof Map<?, ?> catalog)) return;
        Object selected = catalog.get(name);
        if (selected != null) mergePayload(target, selected);
    }

    /**
     * Merges a visual payload using canonical field names. Aliases are normalized while merging so
     * a later {sprite: ...} really replaces an earlier {reference: ...}, rather than coexisting and
     * accidentally losing because the resolver happens to read one alias first.
     */
    private static void mergePayload(Map<String, Object> target, Object raw) {
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) continue;
                String key = canonicalKey(String.valueOf(entry.getKey()));
                if (key.equals("contexts")) {
                    target.put(key, entry.getValue());
                    continue;
                }
                target.put(key, entry.getValue());
            }
        } else if (raw instanceof String text && !text.isBlank()) {
            target.put("variant", text.trim());
        }
    }

    private static Map<String, Object> canonicalMap(Map<String, Object> input) {
        Map<String, Object> output = new LinkedHashMap<>();
        mergePayload(output, input);
        return output;
    }

    private static String canonicalKey(String raw) {
        return switch (raw) {
            case "sprite" -> "reference";
            case "item_model" -> "itemModel";
            case "modelProvider" -> "provider";
            case "model" -> "modelId";
            default -> raw;
        };
    }

    private static Map<String, Object> map(Object raw) {
        if (!(raw instanceof Map<?, ?> input)) return Map.of();
        Map<String, Object> output = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (entry.getKey() != null) output.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return output;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalizeContext(String value) {
        if (value == null || value.isBlank()) return "INVENTORY";
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
