package com.virgolandstudio.items.core.studio;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.behavior.ActionConfig;
import com.virgolandstudio.items.api.behavior.ConditionConfig;
import com.virgolandstudio.items.api.behavior.ValueConfig;
import com.virgolandstudio.items.api.reload.ReloadSequenceDefinition;
import com.virgolandstudio.items.api.reload.ReloadStageDefinition;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts authored runtime values into the provider-neutral JSON/YAML data model used by Studio.
 *
 * <p>Definitions created in Java are allowed to contain convenient API records such as
 * {@link ReloadSequenceDefinition}, {@link ReloadStageDefinition} and {@link ValueConfig}. Studio
 * must never serialize those objects with {@code toString()}, because doing so makes a definition
 * impossible to duplicate/edit and then compile again. This codec recursively lowers values to
 * null/boolean/number/string/list/map while preserving the shapes expected by the existing
 * compilers.</p>
 */
public final class StudioValueCodec {
    private StudioValueCodec() {}

    public static Object encode(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Character character) return String.valueOf(character);
        if (value instanceof NamespacedId id) return id.toString();
        if (value instanceof Enum<?> enumeration) return enumeration.name();
        if (value instanceof Optional<?> optional) return optional.map(StudioValueCodec::encode).orElse(null);

        // ReloadSequenceCompiler intentionally consumes the sequence as a list of stage objects.
        if (value instanceof ReloadSequenceDefinition sequence) {
            return encode(sequence.stages());
        }
        if (value instanceof ReloadStageDefinition stage) {
            var out = new LinkedHashMap<String, Object>();
            out.put("id", stage.id());
            out.put("duration", stage.durationSeconds());
            out.put("interruptible", stage.interruptible());
            out.put("conditions", encode(stage.conditions()));
            out.put("actions", encode(stage.actions()));
            out.put("repeatWhile", encode(stage.repeatWhile()));
            out.put("maxIterations", stage.maxIterations());
            return Collections.unmodifiableMap(out);
        }
        if (value instanceof ConditionConfig condition) {
            return typedNode(condition.type(), condition.values());
        }
        if (value instanceof ActionConfig action) {
            return typedNode(action.type(), action.values());
        }
        if (value instanceof ValueConfig provider) {
            return typedNode(provider.type(), provider.values());
        }

        if (value instanceof Map<?, ?> map) {
            var out = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                if (entry.getKey() == null) continue;
                out.put(String.valueOf(entry.getKey()), encode(entry.getValue()));
            }
            return Collections.unmodifiableMap(out);
        }
        if (value instanceof Iterable<?> iterable) {
            var out = new ArrayList<Object>();
            for (Object element : iterable) out.add(encode(element));
            return List.copyOf(out);
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            var out = new ArrayList<Object>(length);
            for (int index = 0; index < length; index++) out.add(encode(Array.get(value, index)));
            return List.copyOf(out);
        }

        // Other API data records are lowered generically. This keeps future pure-data records
        // Studio-compatible without teaching the HTTP host their classes or packages.
        if (value instanceof Record record) return encodeRecord(record);

        throw new IllegalArgumentException(
                "Unsupported Studio definition value type: " + value.getClass().getName()
                        + ". Publish plain data or an API record instead."
        );
    }

    private static Map<String, Object> typedNode(String type, Map<String, Object> values) {
        var out = new LinkedHashMap<String, Object>();
        out.put("type", type);
        out.put("values", encode(values));
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Object> encodeRecord(Record record) {
        var out = new LinkedHashMap<String, Object>();
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            try {
                out.put(component.getName(), encode(component.getAccessor().invoke(record)));
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalArgumentException(
                        "Could not serialize Studio record " + record.getClass().getName()
                                + "." + component.getName(),
                        exception
                );
            }
        }
        return Collections.unmodifiableMap(out);
    }
}
