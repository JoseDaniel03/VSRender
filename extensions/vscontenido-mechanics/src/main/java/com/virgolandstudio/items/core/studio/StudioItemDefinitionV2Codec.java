package com.virgolandstudio.items.core.studio;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.behavior.ActionConfig;
import com.virgolandstudio.items.api.behavior.BehaviorDefinition;
import com.virgolandstudio.items.api.behavior.ConditionConfig;
import com.virgolandstudio.items.api.behavior.Trigger;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.v2.ItemComponentSet;
import com.virgolandstudio.items.api.definition.v2.ItemDefinitionV2;
import com.virgolandstudio.items.api.definition.v2.ItemFallback;
import com.virgolandstudio.items.api.definition.v2.ItemIdentity;
import com.virgolandstudio.items.api.definition.v2.ItemRepresentation;
import com.virgolandstudio.items.api.definition.v2.ItemStateSchema;
import com.virgolandstudio.items.api.definition.v2.ItemVisualContext;
import com.virgolandstudio.items.api.definition.v2.ItemVisualReference;
import com.virgolandstudio.items.api.definition.v2.StackMode;
import com.virgolandstudio.items.api.definition.v2.StackPolicy;
import com.virgolandstudio.items.api.state.StateFieldDefinition;
import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.migration.LegacyItemDefinitionV2Migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Studio boundary codec for the Item v2 root contract.
 *
 * <p>This codec is intentionally parallel to {@link StudioItemDefinitionCodec}. The live definition
 * store/compiler remains on v1 until migration and resolver gates are green.</p>
 */
public final class StudioItemDefinitionV2Codec {
    private StudioItemDefinitionV2Codec() {}

    /** Decode the canonical v2 shape. */
    public static ItemDefinitionV2 decode(Map<String, Object> raw) {
        if (raw == null) throw new IllegalArgumentException("definition body is required");
        int schemaVersion = integer(raw.get("schemaVersion"), 2);
        if (schemaVersion < 2) throw new IllegalArgumentException("Item v2 requires schemaVersion >= 2");

        Map<String, Object> identityRaw = map(raw.get("identity"));
        if (identityRaw.isEmpty()) throw new IllegalArgumentException("identity is required for Item v2");
        ItemIdentity identity = new ItemIdentity(
                NamespacedId.parse(requiredString(identityRaw, "id")),
                string(identityRaw.get("name"), string(identityRaw.get("displayName"), requiredString(identityRaw, "id"))),
                string(identityRaw.get("family"), "item"),
                strings(identityRaw.get("tags"))
        );

        ItemRepresentation representation = decodeRepresentation(raw);
        StackPolicy stackPolicy = decodeStackPolicy(map(raw.get("stackPolicy")));
        ItemComponentSet components = decodeComponents(map(raw.get("components")));
        ItemStateSchema state = decodeState(map(raw.get("state")));
        List<BehaviorDefinition> behaviors = decodeBehaviors(raw.get("behaviors"));

        return new ItemDefinitionV2(
                schemaVersion,
                identity,
                representation,
                stackPolicy,
                components,
                state,
                behaviors
        );
    }

    /**
     * Compatibility entry point. Legacy data is decoded by the existing v1 codec and then wrapped.
     * Visual and stack semantics remain explicit inputs because legacy definitions cannot determine
     * them safely on their own.
     */
    public static ItemDefinitionV2 decodeCompatible(
            Map<String, Object> raw,
            ItemRepresentation legacyRepresentation,
            StackPolicy legacyStackPolicy
    ) {
        if (raw == null) throw new IllegalArgumentException("definition body is required");
        int schemaVersion = integer(raw.get("schemaVersion"), raw.containsKey("identity") ? 2 : 1);
        if (schemaVersion >= 2 && raw.containsKey("identity")) return decode(raw);
        return LegacyItemDefinitionV2Migration.migrate(
                StudioItemDefinitionCodec.decode(raw),
                legacyRepresentation,
                legacyStackPolicy
        );
    }

    public static Map<String, Object> encode(ItemDefinitionV2 definition) {
        var out = new LinkedHashMap<String, Object>();
        out.put("schemaVersion", definition.schemaVersion());

        var identity = new LinkedHashMap<String, Object>();
        identity.put("id", definition.identity().id().toString());
        identity.put("name", definition.identity().displayName());
        identity.put("family", definition.identity().family());
        identity.put("tags", definition.identity().tags().stream().sorted().toList());
        out.put("identity", Collections.unmodifiableMap(identity));

        var representation = new LinkedHashMap<String, Object>();
        representation.put("fallback", Map.of("material", definition.representation().fallback().material()));
        var visual = new LinkedHashMap<String, Object>();
        if (definition.representation().defaultVisual() != null) {
            visual.put("default", definition.representation().defaultVisual().value());
        }
        putVisual(visual, "inventory", definition, ItemVisualContext.INVENTORY);
        putVisual(visual, "firstPerson", definition, ItemVisualContext.FIRST_PERSON);
        putVisual(visual, "thirdPerson", definition, ItemVisualContext.THIRD_PERSON);
        putVisual(visual, "world", definition, ItemVisualContext.WORLD);
        putVisual(visual, "equipped", definition, ItemVisualContext.EQUIPPED);
        representation.put("visual", Collections.unmodifiableMap(visual));
        out.put("representation", Collections.unmodifiableMap(representation));

        var stackPolicy = new LinkedHashMap<String, Object>();
        stackPolicy.put("mode", definition.stackPolicy().mode().name());
        stackPolicy.put("maxStack", definition.stackPolicy().maxStack());
        stackPolicy.put("mergeStateKeys", definition.stackPolicy().mergeStateKeys().stream().sorted().toList());
        out.put("stackPolicy", Collections.unmodifiableMap(stackPolicy));

        var components = new LinkedHashMap<String, Object>();
        components.put("configs", definition.components().configs().stream().map(component -> {
            var value = new LinkedHashMap<String, Object>();
            value.put("type", component.type().toString());
            value.put("values", StudioValueCodec.encode(component.values()));
            return Collections.unmodifiableMap(value);
        }).toList());
        if (!definition.components().compatibilityBaseStats().isEmpty()) {
            components.put("compatibilityBaseStats", StudioValueCodec.encode(definition.components().compatibilityBaseStats()));
        }
        out.put("components", Collections.unmodifiableMap(components));

        var state = new LinkedHashMap<String, Object>();
        state.put("fields", definition.state().fields().stream().map(field -> {
            var value = new LinkedHashMap<String, Object>();
            value.put("key", field.key());
            value.put("type", field.type().name());
            if (field.defaultValue() != null) value.put("default", StudioValueCodec.encode(field.defaultValue()));
            value.put("persistent", field.persistent());
            return Collections.unmodifiableMap(value);
        }).toList());
        out.put("state", Collections.unmodifiableMap(state));

        out.put("behaviors", definition.behaviors().stream().map(behavior -> {
            var value = new LinkedHashMap<String, Object>();
            value.put("trigger", behavior.trigger().name());
            value.put("conditions", behavior.conditions().stream().map(condition -> typedNode(condition.type(), condition.values())).toList());
            value.put("actions", behavior.actions().stream().map(action -> typedNode(action.type(), action.values())).toList());
            return Collections.unmodifiableMap(value);
        }).toList());

        return Collections.unmodifiableMap(out);
    }

    private static ItemRepresentation decodeRepresentation(Map<String, Object> root) {
        Map<String, Object> representation = map(root.get("representation"));
        if (representation.isEmpty()) throw new IllegalArgumentException("representation is required for Item v2");

        Map<String, Object> fallback = map(representation.get("fallback"));
        String material = string(
                fallback.get("material"),
                string(root.get("material"), string(root.get("baseMaterial"), ""))
        ).trim();
        if (material.isEmpty()) throw new IllegalArgumentException("representation.fallback.material is required");

        Map<String, Object> visual = map(representation.get("visual"));
        ItemVisualReference defaultVisual = visualReference(visual.get("default"));
        var overrides = new EnumMap<ItemVisualContext, ItemVisualReference>(ItemVisualContext.class);
        addVisual(overrides, ItemVisualContext.INVENTORY, visual.get("inventory"));
        addVisual(overrides, ItemVisualContext.FIRST_PERSON, firstNonNull(visual.get("firstPerson"), visual.get("first_person")));
        addVisual(overrides, ItemVisualContext.THIRD_PERSON, firstNonNull(visual.get("thirdPerson"), visual.get("third_person")));
        addVisual(overrides, ItemVisualContext.WORLD, visual.get("world"));
        addVisual(overrides, ItemVisualContext.EQUIPPED, visual.get("equipped"));

        return new ItemRepresentation(new ItemFallback(material), defaultVisual, overrides);
    }

    private static StackPolicy decodeStackPolicy(Map<String, Object> raw) {
        if (raw.isEmpty()) throw new IllegalArgumentException("stackPolicy is required for Item v2");
        StackMode mode = StackMode.valueOf(requiredString(raw, "mode").trim().toUpperCase());
        int maxStack = requiredInteger(raw, "maxStack");
        return new StackPolicy(mode, maxStack, strings(raw.get("mergeStateKeys")));
    }

    private static ItemComponentSet decodeComponents(Map<String, Object> raw) {
        if (raw.isEmpty()) return new ItemComponentSet(List.of(), Map.of());
        List<ComponentConfig> configs = new ArrayList<>();
        for (Object value : iterable(raw.get("configs"))) {
            Map<String, Object> entry = map(value);
            configs.add(new ComponentConfig(
                    NamespacedId.parse(requiredString(entry, "type")),
                    map(entry.get("values"))
            ));
        }

        Map<String, Double> compatibilityBaseStats = new LinkedHashMap<>();
        for (var entry : map(raw.get("compatibilityBaseStats")).entrySet()) {
            if (entry.getValue() != null) compatibilityBaseStats.put(entry.getKey(), number(entry.getValue()));
        }
        return new ItemComponentSet(configs, compatibilityBaseStats);
    }

    private static ItemStateSchema decodeState(Map<String, Object> raw) {
        List<StateFieldDefinition> fields = new ArrayList<>();
        for (Object value : iterable(raw.get("fields"))) {
            Map<String, Object> entry = map(value);
            StateType type = StateType.valueOf(requiredString(entry, "type").trim().toUpperCase());
            fields.add(new StateFieldDefinition(
                    requiredString(entry, "key"),
                    type,
                    entry.get("default"),
                    bool(entry.get("persistent"), true)
            ));
        }
        return new ItemStateSchema(fields);
    }

    private static List<BehaviorDefinition> decodeBehaviors(Object raw) {
        List<BehaviorDefinition> behaviors = new ArrayList<>();
        for (Object value : iterable(raw)) {
            Map<String, Object> entry = map(value);
            Trigger trigger = Trigger.valueOf(requiredString(entry, "trigger").trim().toUpperCase());
            List<ConditionConfig> conditions = new ArrayList<>();
            for (Object conditionValue : iterable(entry.get("conditions"))) {
                Map<String, Object> condition = map(conditionValue);
                conditions.add(new ConditionConfig(requiredString(condition, "type"), map(condition.get("values"))));
            }
            List<ActionConfig> actions = new ArrayList<>();
            for (Object actionValue : iterable(entry.get("actions"))) {
                Map<String, Object> action = map(actionValue);
                actions.add(new ActionConfig(requiredString(action, "type"), map(action.get("values"))));
            }
            behaviors.add(new BehaviorDefinition(trigger, conditions, actions));
        }
        return List.copyOf(behaviors);
    }

    private static void putVisual(
            Map<String, Object> target,
            String key,
            ItemDefinitionV2 definition,
            ItemVisualContext context
    ) {
        ItemVisualReference reference = definition.representation().overrides().get(context);
        if (reference != null) target.put(key, reference.value());
    }

    private static void addVisual(
            Map<ItemVisualContext, ItemVisualReference> target,
            ItemVisualContext context,
            Object value
    ) {
        ItemVisualReference reference = visualReference(value);
        if (reference != null) target.put(context, reference);
    }

    private static ItemVisualReference visualReference(Object value) {
        if (value == null) return null;
        String raw = String.valueOf(value).trim();
        return raw.isEmpty() ? null : ItemVisualReference.parse(raw);
    }

    private static Map<String, Object> typedNode(String type, Map<String, Object> values) {
        var out = new LinkedHashMap<String, Object>();
        out.put("type", type);
        out.put("values", StudioValueCodec.encode(values));
        return Collections.unmodifiableMap(out);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalArgumentException("Expected object/map but got " + value.getClass().getSimpleName());
        }
        var out = new LinkedHashMap<String, Object>();
        for (var entry : source.entrySet()) {
            if (entry.getKey() != null) out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }

    private static Iterable<?> iterable(Object value) {
        if (value == null) return List.of();
        if (value instanceof Iterable<?> iterable) return iterable;
        throw new IllegalArgumentException("Expected list but got " + value.getClass().getSimpleName());
    }

    private static Set<String> strings(Object value) {
        var out = new LinkedHashSet<String>();
        for (Object entry : iterable(value)) {
            if (entry == null) continue;
            String text = String.valueOf(entry).trim();
            if (!text.isEmpty()) out.add(text);
        }
        return Set.copyOf(out);
    }

    private static String requiredString(Map<String, Object> raw, String key) {
        String value = string(raw.get(key), "").trim();
        if (value.isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static int requiredInteger(Map<String, Object> raw, String key) {
        if (!raw.containsKey(key) || raw.get(key) == null) throw new IllegalArgumentException(key + " is required");
        return integer(raw.get(key), 0);
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean bool(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static int integer(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private static double number(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }

    private static Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }
}
