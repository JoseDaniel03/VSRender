package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.registry.BuiltinSimulationSchemas;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves logical item state into provider-neutral visual payloads.
 *
 * <p>Mappings may use exact values, numeric ranges such as {@code 0..24},
 * comparisons such as {@code >=75}, and {@code *} as a fallback. Payloads are
 * intentionally provider-neutral: a string becomes {@code {variant: value}} and
 * a map is passed through as structured visual data for Models or a future
 * adapter to interpret.</p>
 */
public final class ItemVisualStateResolver {
    public Optional<ItemVisualState> resolve(ItemInstance instance) {
        ComponentConfig visual = instance.definition().source().components().stream()
                .filter(component -> component.type().equals(BuiltinSimulationSchemas.VISUAL_STATE))
                .findFirst()
                .orElse(null);
        if (visual == null || !boolValue(visual.values().get("enabled"), false)) return Optional.empty();

        String source = stringValue(visual.values().get("source"), "STATE").toUpperCase(java.util.Locale.ROOT);
        String configuredKey = stringValue(visual.values().get("sourceKey"), "state");
        String sourceKey = resolveSourceKey(source, configuredKey);
        StateSlot slot = instance.state().layout().find(sourceKey);
        if (slot == null) return Optional.empty();

        Object sourceValue = instance.state().get(slot);
        Object mappingsValue = visual.values().get("mappings");
        if (!(mappingsValue instanceof Map<?, ?> mappings) || mappings.isEmpty()) return Optional.empty();

        Match best = null;
        for (Map.Entry<?, ?> entry : mappings.entrySet()) {
            String expression = String.valueOf(entry.getKey()).trim();
            Match candidate = match(expression, sourceValue, entry.getValue());
            if (candidate == null) continue;
            if (best == null || candidate.score() > best.score()
                    || (candidate.score() == best.score() && candidate.key().compareTo(best.key()) < 0)) {
                best = candidate;
            }
        }
        if (best == null) return Optional.empty();

        return Optional.of(new ItemVisualState(
                sourceKey,
                sourceValue,
                best.key(),
                payload(best.value()),
                boolValue(visual.values().get("interpolate"), false)
        ));
    }

    private static Match match(String expression, Object sourceValue, Object payload) {
        if (expression.equals("*")) return new Match(expression, payload, 0);

        if (!(sourceValue instanceof Number number)) {
            return expression.equalsIgnoreCase(String.valueOf(sourceValue))
                    ? new Match(expression, payload, 10_000)
                    : null;
        }

        double value = number.doubleValue();
        if (!Double.isFinite(value)) return null;

        Double exact = parseDouble(expression);
        if (exact != null && Double.compare(value, exact) == 0) {
            return new Match(expression, payload, 10_000);
        }

        int dots = expression.indexOf("..");
        if (dots > 0 && dots < expression.length() - 2) {
            Double min = parseDouble(expression.substring(0, dots).trim());
            Double max = parseDouble(expression.substring(dots + 2).trim());
            if (min == null || max == null) return null;
            double low = Math.min(min, max);
            double high = Math.max(min, max);
            if (value < low || value > high) return null;
            double width = Math.max(0.0, high - low);
            int specificity = 1000 + (int) Math.max(0, Math.min(899, 899 - Math.floor(width)));
            return new Match(expression, payload, specificity);
        }

        String[] operators = {">=", "<=", ">", "<"};
        for (String operator : operators) {
            if (!expression.startsWith(operator)) continue;
            Double boundary = parseDouble(expression.substring(operator.length()).trim());
            if (boundary == null) return null;
            boolean matched = switch (operator) {
                case ">=" -> value >= boundary;
                case "<=" -> value <= boundary;
                case ">" -> value > boundary;
                case "<" -> value < boundary;
                default -> false;
            };
            return matched ? new Match(expression, payload, 500) : null;
        }

        return null;
    }

    private static Map<String, Object> payload(Object value) {
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return out;
        }
        if (value instanceof String string) return Map.of("variant", string);
        return Map.of("value", value);
    }

    private static String resolveSourceKey(String source, String configuredKey) {
        if (configuredKey != null && !configuredKey.isBlank()
                && (!configuredKey.equals("state") || source.equals("STATE") || source.equals("CUSTOM"))) {
            return configuredKey;
        }
        return switch (source) {
            case "COUNTER" -> "count";
            case "USES" -> "uses_remaining";
            case "DURABILITY" -> "durability";
            case "FLUID" -> "fluid_amount";
            case "TEMPERATURE" -> "temperature_c";
            case "ENERGY" -> "energy";
            default -> configuredKey == null || configuredKey.isBlank() ? "state" : configuredKey;
        };
    }

    private static boolean boolValue(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static Double parseDouble(String text) {
        try {
            double value = Double.parseDouble(text);
            return Double.isFinite(value) ? value : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record Match(String key, Object value, int score) {}
}
