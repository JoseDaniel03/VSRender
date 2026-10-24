package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.behavior.ValueConfig;
import com.virgolandstudio.items.core.runtime.ExecutionContext;
import com.virgolandstudio.items.core.runtime.ItemMaintenanceService;
import com.virgolandstudio.items.core.runtime.ItemMaintenanceSnapshot;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generic condition/action vocabulary for item maintenance state.
 *
 * <p>Nothing here knows about rifles, knives, tools or machines. Authored definitions choose which
 * maintenance components exist and which trigger should clean, repair, lubricate, jam or service
 * the current item instance.</p>
 */
public final class BuiltinMaintenanceBehaviorTypes {
    private BuiltinMaintenanceBehaviorTypes() {}

    public static void registerAll(BehaviorCompiler compiler) {
        compiler.registerCondition("maintenance_operational", (config, c) -> {
            boolean expected = bool(config.values(), "expected", true);
            return context -> {
                ItemMaintenanceSnapshot snapshot = snapshot(context);
                publish(context, snapshot);
                return snapshot.operational() == expected;
            };
        });

        compiler.registerCondition("maintenance_jammed", (config, c) -> {
            boolean expected = bool(config.values(), "expected", true);
            return context -> {
                ItemMaintenanceSnapshot snapshot = snapshot(context);
                publish(context, snapshot);
                return snapshot.jammed() == expected;
            };
        });

        compiler.registerCondition("maintenance_metric_between", (config, c) -> {
            String metric = string(config.values(), "metric", "CONDITION").toUpperCase(Locale.ROOT);
            CompiledValue min = c.compileValue(valueConfig(config.values().getOrDefault("min", Double.NEGATIVE_INFINITY)));
            CompiledValue max = c.compileValue(valueConfig(config.values().getOrDefault("max", Double.POSITIVE_INFINITY)));
            return context -> {
                ItemMaintenanceSnapshot snapshot = snapshot(context);
                double value = metric(snapshot, metric);
                double lower = min.resolveDouble(context, Double.NEGATIVE_INFINITY);
                double upper = max.resolveDouble(context, Double.POSITIVE_INFINITY);
                if (!Double.isFinite(value) || Double.isNaN(lower) || Double.isNaN(upper) || lower > upper) return false;
                context.put("maintenance.metric", metric);
                context.put("maintenance.metric_value", value);
                publish(context, snapshot);
                return value >= lower && value <= upper;
            };
        });

        compiler.registerAction("maintenance_clean", (config, c) -> {
            CompiledValue dirt = c.compileValue(valueConfig(config.values().getOrDefault("dirt", 25.0)));
            CompiledValue rust = c.compileValue(valueConfig(config.values().getOrDefault("rust", 0.0)));
            return context -> {
                ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.clean(
                        context.requireInstance(),
                        amount(dirt, context, "dirt"),
                        amount(rust, context, "rust")
                );
                publish(context, snapshot);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("maintenance_clean_profile", (config, c) -> context -> {
            ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.clean(context.requireInstance());
            publish(context, snapshot);
            return ActionResult.CONTINUE;
        });

        compiler.registerAction("maintenance_lubricate", (config, c) -> {
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", 25.0)));
            return context -> {
                ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.lubricate(
                        context.requireInstance(), amount(amount, context, "amount"));
                publish(context, snapshot);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("maintenance_repair", (config, c) -> {
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", 25.0)));
            return context -> {
                ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.repair(
                        context.requireInstance(), amount(amount, context, "amount"));
                publish(context, snapshot);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("maintenance_damage", (config, c) -> {
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", 1.0)));
            return context -> {
                ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.damage(
                        context.requireInstance(), amount(amount, context, "amount"));
                publish(context, snapshot);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("maintenance_corrode", (config, c) -> {
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", 1.0)));
            return context -> {
                ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.corrode(
                        context.requireInstance(), amount(amount, context, "amount"));
                publish(context, snapshot);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("maintenance_set_jammed", (config, c) -> {
            boolean jammed = bool(config.values(), "jammed", true);
            return context -> {
                ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.setJammed(context.requireInstance(), jammed);
                publish(context, snapshot);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("maintenance_record", (config, c) -> context -> {
            ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.recordMaintenance(
                    context.requireInstance(), System.currentTimeMillis());
            publish(context, snapshot);
            return ActionResult.CONTINUE;
        });

        compiler.registerAction("maintenance_service", (config, c) -> {
            CompiledValue dirt = c.compileValue(valueConfig(config.values().getOrDefault("dirt", 0.0)));
            CompiledValue rust = c.compileValue(valueConfig(config.values().getOrDefault("rust", 0.0)));
            CompiledValue lubrication = c.compileValue(valueConfig(config.values().getOrDefault("lubrication", 0.0)));
            CompiledValue repair = c.compileValue(valueConfig(config.values().getOrDefault("repair", 0.0)));
            boolean clearJam = bool(config.values(), "clearJam", false);
            return context -> {
                ItemMaintenanceSnapshot snapshot = ItemMaintenanceService.service(
                        context.requireInstance(),
                        amount(dirt, context, "dirt"),
                        amount(rust, context, "rust"),
                        amount(lubrication, context, "lubrication"),
                        amount(repair, context, "repair"),
                        clearJam,
                        System.currentTimeMillis()
                );
                publish(context, snapshot);
                return ActionResult.CONTINUE;
            };
        });
    }

    private static ItemMaintenanceSnapshot snapshot(ExecutionContext context) {
        return ItemMaintenanceService.snapshot(context.requireInstance());
    }

    private static void publish(ExecutionContext context, ItemMaintenanceSnapshot snapshot) {
        context.put("maintenance.durability", snapshot.durability());
        context.put("maintenance.durability_max", snapshot.durabilityMax());
        context.put("maintenance.wear", snapshot.wear());
        context.put("maintenance.wear_max", snapshot.wearMax());
        context.put("maintenance.dirt", snapshot.dirt());
        context.put("maintenance.dirt_max", snapshot.dirtMax());
        context.put("maintenance.rust", snapshot.rust());
        context.put("maintenance.rust_max", snapshot.rustMax());
        context.put("maintenance.lubrication", snapshot.lubrication());
        context.put("maintenance.lubrication_max", snapshot.lubricationMax());
        context.put("maintenance.condition", snapshot.condition());
        context.put("maintenance.operational", snapshot.operational());
        context.put("maintenance.jammed", snapshot.jammed());
        context.put("maintenance.failure_count", snapshot.failureCount());
        context.put("maintenance.maintenance_count", snapshot.maintenanceCount());
        context.put("maintenance.last_maintenance_ms", snapshot.lastMaintenanceMillis());
        context.put("maintenance.failure_chance", snapshot.mechanicalFailureChance());
    }

    private static double metric(ItemMaintenanceSnapshot snapshot, String metric) {
        return switch (metric) {
            case "DURABILITY" -> snapshot.durability();
            case "DURABILITY_MAX" -> snapshot.durabilityMax();
            case "WEAR" -> snapshot.wear();
            case "WEAR_MAX" -> snapshot.wearMax();
            case "DIRT" -> snapshot.dirt();
            case "DIRT_MAX" -> snapshot.dirtMax();
            case "RUST" -> snapshot.rust();
            case "RUST_MAX" -> snapshot.rustMax();
            case "LUBRICATION" -> snapshot.lubrication();
            case "LUBRICATION_MAX" -> snapshot.lubricationMax();
            case "CONDITION" -> snapshot.condition();
            case "FAILURE_CHANCE" -> snapshot.mechanicalFailureChance();
            case "FAILURE_COUNT" -> snapshot.failureCount();
            case "MAINTENANCE_COUNT" -> snapshot.maintenanceCount();
            case "LAST_MAINTENANCE_MS" -> snapshot.lastMaintenanceMillis();
            default -> throw new IllegalArgumentException("Unknown maintenance metric: " + metric);
        };
    }

    private static double amount(CompiledValue value, ExecutionContext context, String field) {
        double resolved = value.resolveDouble(context, 0.0);
        if (!Double.isFinite(resolved) || resolved < 0.0) {
            throw new IllegalArgumentException(field + " must resolve to a finite value >= 0");
        }
        return resolved;
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        return String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static ValueConfig valueConfig(Object raw) {
        if (raw instanceof ValueConfig config) return config;
        if (raw instanceof Map<?, ?> map && map.get("type") != null) {
            var values = new LinkedHashMap<String, Object>();
            Object nested = map.get("values");
            if (nested instanceof Map<?, ?> nestedMap) {
                for (var entry : nestedMap.entrySet()) {
                    if (entry.getKey() != null) values.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            } else {
                for (var entry : map.entrySet()) {
                    if (entry.getKey() != null && !"type".equals(String.valueOf(entry.getKey()))) {
                        values.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            }
            return new ValueConfig(String.valueOf(map.get("type")), values);
        }
        return ValueConfig.constant(raw);
    }
}
