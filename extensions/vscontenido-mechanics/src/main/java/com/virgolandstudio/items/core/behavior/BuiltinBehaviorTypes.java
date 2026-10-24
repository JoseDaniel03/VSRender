package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.behavior.ValueConfig;
import com.virgolandstudio.items.core.state.StateSlot;
import com.virgolandstudio.items.core.accessory.AccessoryModifierSet;
import com.virgolandstudio.items.core.curve.CurveCompiler;
import com.virgolandstudio.items.core.reload.ReloadActionBridge;
import com.virgolandstudio.items.core.ammo.AmmoActionBridge;
import com.virgolandstudio.items.core.projectile.ProjectileHitBridge;
import com.virgolandstudio.items.core.projectile.ProjectileSpawnBridge;
import com.virgolandstudio.items.core.ranged.RecoilBridge;
import com.virgolandstudio.items.core.effect.EffectBridge;
import com.virgolandstudio.items.core.effect.EffectOrigin;
import com.virgolandstudio.items.core.world.BlockActionBridge;
import com.virgolandstudio.items.core.world.EntityActionBridge;
import com.virgolandstudio.items.core.world.EntityImpactBridge;
import com.virgolandstudio.items.core.runtime.CooldownBridge;
import com.virgolandstudio.items.core.resource.EnergyBridge;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class BuiltinBehaviorTypes {
    private BuiltinBehaviorTypes() {}

    public static void registerAll(BehaviorCompiler compiler) {
        compiler.registerValue("constant", config -> context -> config.values().get("value"));
        compiler.registerValue("stat", config -> {
            String key = string(config.values(), "key");
            double fallback = number(config.values(), "fallback", 0.0);
            return context -> context.item().stat(key, fallback);
        });
        compiler.registerValue("variable", config -> {
            String key = string(config.values(), "key");
            return context -> context.get(key);
        });
        compiler.registerValue("state", config -> {
            String key = string(config.values(), "key");
            var slot = cachedSlot(key);
            return context -> context.requireInstance().state().get(slot.apply(context));
        });
        compiler.registerValue("curve", config -> {
            var curve = CurveCompiler.compile(config.values().get("curve"));
            CompiledValue input = compiler.compileValue(valueConfig(config.values().get("input")));
            return context -> curve.evaluate(input.resolveDouble(context, 0.0));
        });

        compiler.registerCondition("has_component", (config, c) -> {
            var id = NamespacedId.parse(string(config.values(), "component"));
            return context -> context.item().componentTypes().contains(id);
        });
        compiler.registerCondition("variable_equals", (config, c) -> {
            String key = string(config.values(), "key");
            Object expected = config.values().get("value");
            return context -> valuesEqual(context.get(key), expected);
        });
        compiler.registerCondition("variable_compare", (config, c) -> {
            String key = string(config.values(), "key");
            String operator = string(config.values(), "operator");
            CompiledValue expected = c.compileValue(valueConfig(config.values().get("value")));
            return context -> {
                Object raw = context.get(key);
                if (!(raw instanceof Number actual)) return false;
                double left = actual.doubleValue();
                double right = expected.resolveDouble(context, 0.0);
                return compare(left, right, operator);
            };
        });
        compiler.registerCondition("state_equals", (config, c) -> {
            String key = string(config.values(), "key");
            Object expected = config.values().get("value");
            var slot = cachedSlot(key);
            return context -> valuesEqual(context.requireInstance().state().get(slot.apply(context)), expected);
        });
        compiler.registerCondition("state_compare", (config, c) -> {
            String key = string(config.values(), "key");
            String operator = string(config.values(), "operator");
            CompiledValue expected = c.compileValue(valueConfig(config.values().get("value")));
            var slot = cachedSlot(key);
            return context -> {
                Object raw = context.requireInstance().state().get(slot.apply(context));
                if (!(raw instanceof Number actual)) return false;
                double left = actual.doubleValue();
                double right = expected.resolveDouble(context, 0.0);
                return compare(left, right, operator);
            };
        });
        compiler.registerCondition("reload_can_load_unit", (config, c) -> context ->
                context.requireService(ReloadActionBridge.class).canLoadUnit(context));
        compiler.registerCondition("reload_can_replace_resource", (config, c) -> context ->
                context.requireService(ReloadActionBridge.class).canReplaceResource(context));

        compiler.registerCondition("block_held", (config, c) -> context ->
                context.requireService(BlockActionBridge.class).isHolding(context));
        compiler.registerCondition("block_can_grab", (config, c) -> context ->
                context.requireService(BlockActionBridge.class).canGrab(context));
        compiler.registerCondition("entity_can_pull", (config, c) -> context ->
                context.requireService(EntityActionBridge.class).canPull(context));
        compiler.registerCondition("cooldown_ready", (config, c) -> {
            String key = string(config.values(), "key", "default");
            return context -> {
                CooldownBridge bridge = context.requireService(CooldownBridge.class);
                boolean ready = bridge.ready(key);
                context.put("cooldown.ready", ready);
                context.put("cooldown.remaining_ticks", bridge.remainingTicks(key));
                return ready;
            };
        });
        compiler.registerCondition("energy_has", (config, c) -> {
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", ValueConfig.constant(0.0))));
            return context -> {
                double required = Math.max(0.0, amount.resolveDouble(context, 0.0));
                EnergyBridge bridge = context.requireService(EnergyBridge.class);
                boolean has = bridge.has(context, required);
                context.put("energy.current", bridge.current(context));
                context.put("energy.required", required);
                return has;
            };
        });

        compiler.registerAction("set_variable", (config, c) -> {
            String key = string(config.values(), "key");
            ValueConfig valueConfig = valueConfig(config.values().get("value"));
            CompiledValue value = c.compileValue(valueConfig);
            return context -> {
                context.put(key, value.resolve(context));
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("add_variable", (config, c) -> {
            String key = string(config.values(), "key");
            CompiledValue value = c.compileValue(valueConfig(config.values().get("value")));
            return context -> {
                double current = context.get(key) instanceof Number number ? number.doubleValue() : 0.0;
                context.put(key, current + value.resolveDouble(context, 0.0));
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("set_state", (config, c) -> {
            String key = string(config.values(), "key");
            CompiledValue value = c.compileValue(valueConfig(config.values().get("value")));
            var slot = cachedSlot(key);
            return context -> {
                context.requireInstance().state().set(slot.apply(context), value.resolve(context));
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("add_state", (config, c) -> {
            String key = string(config.values(), "key");
            CompiledValue value = c.compileValue(valueConfig(config.values().get("value")));
            var slot = cachedSlot(key);
            return context -> {
                double delta = value.resolveDouble(context, 0.0);
                context.requireInstance().state().add(slot.apply(context), delta);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("reload_detach_container", (config, c) -> context -> {
            context.requireService(ReloadActionBridge.class).detachContainer(context);
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("reload_insert_reserved_container", (config, c) -> context -> {
            context.requireService(ReloadActionBridge.class).insertReservedContainer(context);
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("feed_chamber", (config, c) -> context -> {
            context.requireService(ReloadActionBridge.class).feedChamber(context);
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("reload_load_one", (config, c) -> context -> {
            context.requireService(ReloadActionBridge.class).loadOne(context);
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("reload_replace_resource", (config, c) -> context -> {
            context.requireService(ReloadActionBridge.class).replaceResource(context);
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("ammo_fire", (config, c) -> {
            boolean cycle = bool(config.values(), "cycle", true);
            return context -> {
                AmmoActionBridge bridge = context.requireService(AmmoActionBridge.class);
                boolean accepted = bridge.fire(context, cycle);
                context.put("fire.accepted", accepted);
                context.put("ammo.container.available", bridge.available(context));
                context.put("ammo.container.capacity", bridge.capacity(context));
                context.put("ammo.chambered", bridge.chambered(context));
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("ammo_feed_one", (config, c) -> context -> {
            AmmoActionBridge bridge = context.requireService(AmmoActionBridge.class);
            boolean fed = bridge.feedOne(context);
            context.put("ammo.feed.accepted", fed);
            context.put("ammo.container.available", bridge.available(context));
            context.put("ammo.container.capacity", bridge.capacity(context));
            context.put("ammo.chambered", bridge.chambered(context));
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("spawn_projectile", (config, c) -> context -> {
            ProjectileSpawnBridge bridge = context.requireService(ProjectileSpawnBridge.class);
            int count = accessoryProjectileCount(context, 1);
            int spawned = 0;
            for (int i = 0; i < count; i++) {
                if (i > 0) context.budget().consumeOperation();
                if (bridge.spawn(context)) spawned++;
            }
            context.put("projectile.spawned", spawned > 0);
            context.put("projectile.spawned_count", spawned);
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("spawn_projectiles", (config, c) -> {
            int count = integer(config.values(), "count", 1);
            if (count < 1 || count > 32) throw new IllegalArgumentException("spawn_projectiles.count must be between 1 and 32");
            return context -> {
                ProjectileSpawnBridge bridge = context.requireService(ProjectileSpawnBridge.class);
                int effectiveCount = accessoryProjectileCount(context, count);
                int spawned = 0;
                for (int i = 0; i < effectiveCount; i++) {
                    // The action itself already consumes one operation; each pellet/bolt also spends budget.
                    context.budget().consumeOperation();
                    if (bridge.spawn(context)) spawned++;
                }
                context.put("projectile.spawned", spawned > 0);
                context.put("projectile.spawned_count", spawned);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("projectile_damage", (config, c) -> {
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", ValueConfig.constant(1.0))));
            return context -> {
                double damage = Math.max(0.0, amount.resolveDouble(context, 0.0));
                boolean applied = context.requireService(ProjectileHitBridge.class).damage(context, damage);
                context.put("projectile.damage.applied", applied);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("apply_recoil", (config, c) -> context -> {
            context.requireService(RecoilBridge.class).apply(context);
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("block_grab", (config, c) -> context -> {
            boolean grabbed = context.requireService(BlockActionBridge.class).grab(context);
            context.put("block.grabbed", grabbed);
            context.put("block.held", context.requireService(BlockActionBridge.class).isHolding(context));
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("block_throw", (config, c) -> context -> {
            boolean thrown = context.requireService(BlockActionBridge.class).throwHeld(context);
            context.put("block.thrown", thrown);
            context.put("block.held", context.requireService(BlockActionBridge.class).isHolding(context));
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("block_release", (config, c) -> context -> {
            boolean released = context.requireService(BlockActionBridge.class).releaseHeld(context);
            context.put("block.released", released);
            context.put("block.held", context.requireService(BlockActionBridge.class).isHolding(context));
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("entity_pull", (config, c) -> context -> {
            boolean pulled = context.requireService(EntityActionBridge.class).pull(context);
            context.put("entity.pulled", pulled);
            return ActionResult.CONTINUE;
        });
        compiler.registerAction("impact_damage", (config, c) -> {
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", ValueConfig.constant(1.0))));
            return context -> {
                double damage = Math.max(0.0, amount.resolveDouble(context, 0.0));
                boolean applied = context.requireService(EntityImpactBridge.class).damage(context, damage);
                context.put("impact.damage.applied", applied);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("impact_knockback", (config, c) -> {
            CompiledValue horizontal = c.compileValue(valueConfig(config.values().getOrDefault("horizontal", ValueConfig.constant(0.8))));
            CompiledValue vertical = c.compileValue(valueConfig(config.values().getOrDefault("vertical", ValueConfig.constant(0.2))));
            return context -> {
                boolean applied = context.requireService(EntityImpactBridge.class).knockback(
                        context, Math.max(0.0, horizontal.resolveDouble(context, 0.0)), vertical.resolveDouble(context, 0.0)
                );
                context.put("impact.knockback.applied", applied);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("start_cooldown", (config, c) -> {
            String key = string(config.values(), "key", "default");
            long durationTicks = Math.max(0L, Math.round(number(config.values(), "durationTicks", 10.0)));
            return context -> {
                context.requireService(CooldownBridge.class).start(key, durationTicks);
                context.put("cooldown.started", true);
                context.put("cooldown.remaining_ticks", durationTicks);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("consume_energy", (config, c) -> {
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", ValueConfig.constant(0.0))));
            return context -> {
                double requested = Math.max(0.0, amount.resolveDouble(context, 0.0));
                EnergyBridge bridge = context.requireService(EnergyBridge.class);
                boolean consumed = bridge.consume(context, requested);
                context.put("energy.consumed", consumed);
                context.put("energy.current", bridge.current(context));
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("spawn_particle", (config, c) -> {
            String particle = string(config.values(), "particle");
            EffectOrigin origin = EffectOrigin.parse(string(config.values(), "origin", "HIT"));
            int count = integer(config.values(), "count", 1);
            double offsetX = number(config.values(), "offsetX", number(config.values(), "offset", 0.0));
            double offsetY = number(config.values(), "offsetY", number(config.values(), "offset", 0.0));
            double offsetZ = number(config.values(), "offsetZ", number(config.values(), "offset", 0.0));
            double extra = number(config.values(), "extra", 0.0);
            if (count < 1 || count > 256) throw new IllegalArgumentException("spawn_particle.count must be between 1 and 256");
            return context -> {
                boolean emitted = context.requireService(EffectBridge.class).particle(
                        context, origin, particle, count, offsetX, offsetY, offsetZ, extra
                );
                context.put("effect.particle.emitted", emitted);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("play_sound", (config, c) -> {
            String sound = string(config.values(), "sound");
            String category = string(config.values(), "category", "PLAYERS");
            EffectOrigin origin = EffectOrigin.parse(string(config.values(), "origin", "HIT"));
            double volume = number(config.values(), "volume", 1.0);
            double pitch = number(config.values(), "pitch", 1.0);
            if (volume < 0.0 || volume > 16.0 || !Double.isFinite(volume)) throw new IllegalArgumentException("play_sound.volume must be finite and between 0 and 16");
            if (pitch <= 0.0 || pitch > 2.0 || !Double.isFinite(pitch)) throw new IllegalArgumentException("play_sound.pitch must be finite and in (0, 2]");
            return context -> {
                boolean acceptedFire = context.trigger() == com.virgolandstudio.items.api.behavior.Trigger.FIRE
                        && Boolean.TRUE.equals(context.get("fire.accepted"));
                AccessoryModifierSet modifiers = acceptedFire
                        ? context.service(AccessoryModifierSet.class).orElse(AccessoryModifierSet.empty())
                        : AccessoryModifierSet.empty();
                String effectiveSound = modifiers.string("fire.sound", sound);
                double effectiveVolume = clamp(modifiers.number("fire.sound.volume", volume), 0.0, 16.0);
                double effectivePitch = clamp(modifiers.number("fire.sound.pitch", pitch), 0.01, 2.0);
                boolean emitted = context.requireService(EffectBridge.class).sound(
                        context, origin, effectiveSound, category, effectiveVolume, effectivePitch
                );
                context.put("effect.sound.emitted", emitted);
                return ActionResult.CONTINUE;
            };
        });
        compiler.registerAction("stop", (config, c) -> context -> ActionResult.STOP);
        compiler.registerAction("cancel_trigger", (config, c) -> context -> ActionResult.CANCEL_TRIGGER);
    }

    private static int accessoryProjectileCount(com.virgolandstudio.items.core.runtime.ExecutionContext context, int base) {
        return context.service(AccessoryModifierSet.class)
                .map(modifiers -> modifiers.integer("projectile.count", base, 1, 32))
                .orElse(base);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static Function<com.virgolandstudio.items.core.runtime.ExecutionContext, StateSlot> cachedSlot(String key) {
        var cached = new AtomicReference<StateSlot>();
        return context -> {
            StateSlot slot = cached.get();
            if (slot != null) return slot;
            StateSlot resolved = context.item().stateLayout().require(key);
            cached.compareAndSet(null, resolved);
            return cached.get();
        };
    }

    private static boolean compare(double left, double right, String operator) {
        return switch (operator) {
            case ">" -> left > right;
            case ">=" -> left >= right;
            case "<" -> left < right;
            case "<=" -> left <= right;
            case "==" -> Double.compare(left, right) == 0;
            case "!=" -> Double.compare(left, right) != 0;
            default -> throw new IllegalArgumentException("Unsupported numeric operator: " + operator);
        };
    }

    private static boolean valuesEqual(Object left, Object right) {
        if (left instanceof Number a && right instanceof Number b) {
            return Double.compare(a.doubleValue(), b.doubleValue()) == 0;
        }
        return java.util.Objects.equals(left, right);
    }

    @SuppressWarnings("unchecked")
    private static ValueConfig valueConfig(Object raw) {
        if (raw instanceof ValueConfig config) return config;
        if (raw instanceof Map<?, ?> map) {
            Object type = map.get("type");
            Object values = map.get("values");
            if (!(type instanceof String string)) throw new IllegalArgumentException("value.type must be a string");
            return new ValueConfig(string, values instanceof Map<?, ?> v ? (Map<String, Object>) v : Map.of());
        }
        return ValueConfig.constant(raw);
    }

    private static String string(Map<String, Object> values, String key) {
        Object raw = values.get(key);
        if (!(raw instanceof String value) || value.isBlank()) throw new IllegalArgumentException(key + " must be a non-blank string");
        return value;
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object raw = values.get(key);
        return raw instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object raw = values.get(key);
        return raw instanceof Boolean value ? value : fallback;
    }

    private static double number(Map<String, Object> values, String key, double fallback) {
        Object raw = values.get(key);
        return raw instanceof Number number ? number.doubleValue() : fallback;
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        Object raw = values.get(key);
        return raw instanceof Number number ? number.intValue() : fallback;
    }
}
