package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.core.runtime.ItemTimedProcessRuntime;

import java.util.Map;

/** Generic behavior primitives backed by the timestamp-based Timed Process component. */
public final class BuiltinTimedProcessBehaviorTypes {
    private BuiltinTimedProcessBehaviorTypes() {}

    public static void registerAll(BehaviorCompiler compiler) {
        compiler.registerValue("process_progress", config -> context -> {
            var snapshot = ItemTimedProcessRuntime.snapshot(context.requireInstance(), now(context));
            publish(context, snapshot, false);
            return snapshot.progress();
        });

        compiler.registerCondition("process_active", (config, c) -> context -> {
            var snapshot = ItemTimedProcessRuntime.snapshot(context.requireInstance(), now(context));
            publish(context, snapshot, false);
            return snapshot.active();
        });
        compiler.registerCondition("process_ready", (config, c) -> context -> {
            var snapshot = ItemTimedProcessRuntime.snapshot(context.requireInstance(), now(context));
            publish(context, snapshot, false);
            return snapshot.active() && snapshot.progress() >= 1.0;
        });

        compiler.registerAction("process_start", (config, c) -> {
            String processId = string(config.values(), "processId", "");
            Long durationMillis = durationMillis(config.values());
            return context -> {
                var snapshot = ItemTimedProcessRuntime.start(
                        context.requireInstance(), processId, durationMillis, now(context)
                );
                publish(context, snapshot, false);
                context.put("process.started", true);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("process_advance", (config, c) -> context -> {
            var result = ItemTimedProcessRuntime.advance(context.requireInstance(), now(context));
            publish(context, result.snapshot(), result.completedNow());
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("process_cancel", (config, c) -> context -> {
            var snapshot = ItemTimedProcessRuntime.cancel(context.requireInstance(), now(context));
            publish(context, snapshot, false);
            context.put("process.cancelled", true);
            return ActionResult.CONTINUE;
        });
    }

    private static void publish(
            com.virgolandstudio.items.core.runtime.ExecutionContext context,
            ItemTimedProcessRuntime.Snapshot snapshot,
            boolean completedNow
    ) {
        context.put("process.active", snapshot.active());
        context.put("process.id", snapshot.processId());
        context.put("process.started_at_ms", snapshot.startedAtMillis());
        context.put("process.deadline_ms", snapshot.deadlineMillis());
        context.put("process.duration_ms", snapshot.durationMillis());
        context.put("process.progress", snapshot.progress());
        context.put("process.completed", completedNow);
    }

    private static long now(com.virgolandstudio.items.core.runtime.ExecutionContext context) {
        Object value = context.get("runtime.now_ms");
        return value instanceof Number number ? number.longValue() : System.currentTimeMillis();
    }

    private static Long durationMillis(Map<String, Object> values) {
        Object millis = values.get("durationMillis");
        if (millis instanceof Number number) return Math.max(0L, number.longValue());
        Object seconds = values.get("durationSeconds");
        if (!(seconds instanceof Number number)) return null;
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("process_start.durationSeconds must be finite and >= 0");
        }
        if (value >= Long.MAX_VALUE / 1000.0) return Long.MAX_VALUE;
        return Math.max(0L, Math.round(value * 1000.0));
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        return value instanceof String text ? text.trim() : fallback;
    }
}
