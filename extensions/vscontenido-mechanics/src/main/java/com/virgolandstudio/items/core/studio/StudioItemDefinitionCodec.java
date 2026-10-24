package com.virgolandstudio.items.core.studio;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.behavior.ActionConfig;
import com.virgolandstudio.items.api.behavior.BehaviorDefinition;
import com.virgolandstudio.items.api.behavior.ConditionConfig;
import com.virgolandstudio.items.api.behavior.Trigger;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.api.state.StateFieldDefinition;
import com.virgolandstudio.items.api.state.StateType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JDK-only codec used at the Studio boundary. */
public final class StudioItemDefinitionCodec {
    private StudioItemDefinitionCodec() {}

    public static ItemDefinition decode(Map<String, Object> raw) {
        if (raw == null) throw new IllegalArgumentException("definition body is required");
        int schemaVersion = integer(raw.get("schemaVersion"), 1);
        String id = requiredString(raw, "id");
        String name = string(raw.get("name"), string(raw.get("displayName"), id));
        String material = string(raw.get("material"), string(raw.get("baseMaterial"), "PAPER"));
        String family = string(raw.get("family"), string(raw.get("type"), "item"));

        Set<String> tags = new LinkedHashSet<>();
        for (Object value : iterable(raw.get("tags"))) if (value != null) tags.add(String.valueOf(value));

        Map<String, Double> baseStats = new LinkedHashMap<>();
        for (var entry : map(raw.get("baseStats")).entrySet()) {
            if (entry.getValue() == null) continue;
            baseStats.put(entry.getKey(), number(entry.getValue()));
        }

        List<StateFieldDefinition> state = new ArrayList<>();
        for (Object value : iterable(raw.get("state"))) {
            Map<String, Object> entry = map(value);
            String key = requiredString(entry, "key");
            StateType type = StateType.valueOf(requiredString(entry, "type").trim().toUpperCase());
            state.add(new StateFieldDefinition(
                    key,
                    type,
                    normalizeStateDefault(type, entry.get("default")),
                    bool(entry.get("persistent"), true)
            ));
        }

        Object componentsRaw = raw.containsKey("componentConfigs") ? raw.get("componentConfigs") : raw.get("components");
        List<ComponentConfig> components = new ArrayList<>();
        for (Object value : iterable(componentsRaw)) {
            if (value instanceof CharSequence sequence) {
                components.add(new ComponentConfig(NamespacedId.parse(sequence.toString()), Map.of()));
                continue;
            }
            Map<String, Object> entry = map(value);
            String type = requiredString(entry, "type");
            components.add(new ComponentConfig(NamespacedId.parse(type), map(entry.get("values"))));
        }

        List<BehaviorDefinition> behaviors = new ArrayList<>();
        for (Object value : iterable(raw.get("behaviors"))) {
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

        return new ItemDefinition(
                schemaVersion,
                NamespacedId.parse(id),
                name,
                material,
                family,
                tags,
                baseStats,
                state,
                components,
                behaviors
        );
    }

    public static Map<String, Object> encode(ItemDefinition definition) {
        var out = new LinkedHashMap<String, Object>();
        out.put("schemaVersion", definition.schemaVersion());
        out.put("id", definition.id().toString());
        out.put("name", definition.displayName());
        out.put("material", definition.baseMaterial());
        out.put("family", definition.family() == null ? "" : definition.family());
        out.put("tags", definition.tags().stream().sorted().toList());
        out.put("baseStats", StudioValueCodec.encode(definition.baseStats()));
        out.put("state", definition.state().stream().map(field -> {
            var value = new LinkedHashMap<String, Object>();
            value.put("key", field.key());
            value.put("type", field.type().name());
            if (field.defaultValue() != null) value.put("default", StudioValueCodec.encode(field.defaultValue()));
            value.put("persistent", field.persistent());
            return Collections.unmodifiableMap(value);
        }).toList());
        out.put("componentConfigs", definition.components().stream().map(component -> {
            var value = new LinkedHashMap<String, Object>();
            value.put("type", component.type().toString());
            value.put("values", StudioValueCodec.encode(component.values()));
            return Collections.unmodifiableMap(value);
        }).toList());
        out.put("behaviors", definition.behaviors().stream().map(behavior -> {
            var value = new LinkedHashMap<String, Object>();
            value.put("trigger", behavior.trigger().name());
            value.put("conditions", behavior.conditions().stream().map(condition -> {
                var node = new LinkedHashMap<String, Object>();
                node.put("type", condition.type());
                node.put("values", StudioValueCodec.encode(condition.values()));
                return Collections.unmodifiableMap(node);
            }).toList());
            value.put("actions", behavior.actions().stream().map(action -> {
                var node = new LinkedHashMap<String, Object>();
                node.put("type", action.type());
                node.put("values", StudioValueCodec.encode(action.values()));
                return Collections.unmodifiableMap(node);
            }).toList());
            return Collections.unmodifiableMap(value);
        }).toList());
        return Collections.unmodifiableMap(out);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> source)) throw new IllegalArgumentException("Expected object/map but got " + value.getClass().getSimpleName());
        var out = new LinkedHashMap<String, Object>();
        for (var entry : source.entrySet()) if (entry.getKey() != null) out.put(String.valueOf(entry.getKey()), entry.getValue());
        return out;
    }

    private static Iterable<?> iterable(Object value) {
        if (value == null) return List.of();
        if (value instanceof Iterable<?> iterable) return iterable;
        throw new IllegalArgumentException("Expected list but got " + value.getClass().getSimpleName());
    }

    private static String requiredString(Map<String, Object> raw, String key) {
        String value = string(raw.get(key), "").trim();
        if (value.isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value;
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

    private static Object normalizeStateDefault(StateType type, Object value) {
        if (value == null) return null;
        if (type == StateType.INTEGER && value instanceof Number number) {
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                return value;
            }
            double numeric = number.doubleValue();
            if (Double.isFinite(numeric)
                    && numeric >= Long.MIN_VALUE
                    && numeric <= Long.MAX_VALUE
                    && Math.rint(numeric) == numeric) {
                return number.longValue();
            }
        }
        if (type == StateType.DOUBLE && value instanceof Number number) {
            return number.doubleValue();
        }
        return value;
    }
}
