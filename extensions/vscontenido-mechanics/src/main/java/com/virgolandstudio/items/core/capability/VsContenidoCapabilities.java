package com.virgolandstudio.items.core.capability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Renderer-independent, composable configuration under an object's "vscontenido" YAML key.
 * The object may be an item, block, furniture or entity. Capabilities are deliberately open:
 * the feature adapter, not the object kind, owns runtime semantics.
 */
public final class VsContenidoCapabilities {
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]{0,63}");
    private static final int MAX_FEATURES = 32;
    private static final int MAX_ENTRIES = 128;
    private static final int MAX_DEPTH = 8;
    private static final Set<String> BUILT_INS = Set.of(
            "fov", "controles", "bala", "recarga", "acciones", "efectos", "cooldown",
            "interaccion", "estado", "energia", "proyectil", "animaciones", "sonidos");

    private final Map<String, Map<String, Object>> features;

    private VsContenidoCapabilities(Map<String, Map<String, Object>> features) {
        this.features = Collections.unmodifiableMap(new LinkedHashMap<>(features));
    }

    /** Parse a raw YAML map, retaining custom extension identifiers for later providers. */
    public static VsContenidoCapabilities parse(Object raw) {
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            throw new IllegalArgumentException("vscontenido debe ser un mapa YAML con al menos una capacidad");
        }
        if (map.size() > MAX_FEATURES) {
            throw new IllegalArgumentException("vscontenido admite hasta " + MAX_FEATURES + " capacidades por objeto");
        }
        var features = new LinkedHashMap<String, Map<String, Object>>();
        for (var entry : map.entrySet()) {
            String name = normalized(entry.getKey(), "capacidad");
            if (!(entry.getValue() instanceof Map<?, ?> parameters)) {
                throw new IllegalArgumentException("vscontenido." + name + " debe ser un mapa de parametros");
            }
            var values = freezeMap(parameters, "vscontenido." + name, 1);
            validateBuiltIn(name, values);
            if (features.putIfAbsent(name, values) != null) {
                throw new IllegalArgumentException("Capacidad repetida: " + name);
            }
        }
        return new VsContenidoCapabilities(features);
    }

    public Map<String, Map<String, Object>> all() { return features; }

    public boolean has(String name) { return features.containsKey(normalized(name, "capacidad")); }

    public Map<String, Object> settings(String name) {
        return features.getOrDefault(normalized(name, "capacidad"), Map.of());
    }

    public boolean isBuiltIn(String name) { return BUILT_INS.contains(normalized(name, "capacidad")); }

    private static void validateBuiltIn(String name, Map<String, Object> values) {
        if ("fov".equals(name)) {
            validateNumber(values, "aumento", 1.0, 10.0);
            validateNumber(values, "magnification", 1.0, 10.0);
            validateNumber(values, "multiplicador", 1.0, 10.0);
        } else if ("bala".equals(name) || "proyectil".equals(name)) {
            validateNumber(values, "cantidad", 1.0, 64.0);
            validateNumber(values, "velocidad", 0.0001, 1000.0);
            validateNumber(values, "danio", 0.0, 100000.0);
        } else if ("recarga".equals(name)) {
            validateNumber(values, "ticks", 0.0, 72000.0);
            validateNumber(values, "capacidad", 1.0, 100000.0);
        } else if ("controles".equals(name)) {
            for (var action : values.entrySet()) {
                if (!(action.getValue() instanceof String) && !(action.getValue() instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException("vscontenido.controles." + action.getKey()
                            + " debe ser una tecla o una configuracion de entrada");
                }
            }
        }
    }

    private static void validateNumber(Map<String, Object> values, String key, double min, double max) {
        if (!values.containsKey(key)) return;
        Object value = values.get(key);
        if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue())
                || n.doubleValue() < min || n.doubleValue() > max) {
            throw new IllegalArgumentException("vscontenido: " + key + " debe ser un numero entre " + min + " y " + max);
        }
    }

    private static Map<String, Object> freezeMap(Map<?, ?> input, String path, int depth) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("Configuracion demasiado profunda: " + path);
        if (input.size() > MAX_ENTRIES) throw new IllegalArgumentException("Demasiados parametros: " + path);
        var result = new LinkedHashMap<String, Object>();
        for (var entry : input.entrySet()) {
            String key = normalized(entry.getKey(), path);
            Object value = freeze(entry.getValue(), path + "." + key, depth + 1);
            if (result.putIfAbsent(key, value) != null) throw new IllegalArgumentException("Clave repetida: " + path + "." + key);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Object freeze(Object value, String path, int depth) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("Configuracion demasiado profunda: " + path);
        if (value instanceof Map<?, ?> map) return freezeMap(map, path, depth);
        if (value instanceof java.util.List<?> list) {
            if (list.size() > MAX_ENTRIES) throw new IllegalArgumentException("Lista demasiado larga: " + path);
            var items = new java.util.ArrayList<Object>(list.size());
            for (Object e : list) items.add(freeze(e, path, depth + 1));
            return java.util.List.copyOf(items);
        }
        if (value instanceof String text) {
            if (text.length() > 2048) throw new IllegalArgumentException("Texto demasiado largo: " + path);
            return text;
        }
        if (value instanceof Boolean) return value;
        if (value instanceof Number number && Double.isFinite(number.doubleValue())) return number;
        throw new IllegalArgumentException("Valor YAML no soportado en " + path);
    }

    private static String normalized(Object raw, String label) {
        if (!(raw instanceof String text)) throw new IllegalArgumentException("Nombre invalido de " + label);
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (!NAME.matcher(normalized).matches()) throw new IllegalArgumentException("Nombre invalido de " + label + ": " + text);
        return normalized;
    }
}
