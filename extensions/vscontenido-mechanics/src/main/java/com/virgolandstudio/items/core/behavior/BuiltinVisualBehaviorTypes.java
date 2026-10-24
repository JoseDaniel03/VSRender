package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.core.runtime.ItemVisualActionBridge;

import java.util.Map;

/** Provider-neutral behavior actions for transient visual variants and animations. */
public final class BuiltinVisualBehaviorTypes {
    private BuiltinVisualBehaviorTypes() {}

    public static void registerAll(BehaviorCompiler compiler) {
        compiler.registerAction("set_visual_variant", (config, c) -> {
            String variant = required(config.values(), "variant");
            long durationTicks = durationTicks(config.values());
            return context -> {
                context.requireService(ItemVisualActionBridge.class)
                        .setVariant(context.requireInstance(), variant, durationTicks);
                context.put("visual.variant", variant);
                context.put("visual.duration_ticks", durationTicks);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("set_visual_animation", (config, c) -> {
            String animation = required(config.values(), "animation");
            long durationTicks = durationTicks(config.values());
            return context -> {
                context.requireService(ItemVisualActionBridge.class)
                        .setAnimation(context.requireInstance(), animation, durationTicks);
                context.put("visual.animation", animation);
                context.put("visual.duration_ticks", durationTicks);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("clear_visual_override", (config, c) -> context -> {
            context.requireService(ItemVisualActionBridge.class).clear(context.requireInstance());
            context.put("visual.cleared", true);
            return ActionResult.CONTINUE;
        });
    }

    private static String required(Map<String, Object> values, String key) {
        Object raw = values.get(key);
        String value = raw == null ? "" : String.valueOf(raw).trim();
        if (value.isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static long durationTicks(Map<String, Object> values) {
        Object raw = values.get("durationTicks");
        if (raw == null) return 0L;
        long value;
        if (raw instanceof Number number) value = number.longValue();
        else value = Long.parseLong(String.valueOf(raw).trim());
        if (value < 0L) throw new IllegalArgumentException("durationTicks must be >= 0");
        return value;
    }
}
