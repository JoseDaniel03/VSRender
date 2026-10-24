package com.virgolandstudio.items.core.world;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.core.compile.ValidationException;
import com.virgolandstudio.items.core.registry.BuiltinSchemas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class WorldInteractionCompiler {
    public CompiledWorldInteractionSystem compile(ItemDefinition definition) {
        ComponentConfig grab = find(definition, BuiltinSchemas.BLOCK_GRAB);
        ComponentConfig hold = find(definition, BuiltinSchemas.BLOCK_HOLD);
        ComponentConfig blockThrow = find(definition, BuiltinSchemas.BLOCK_THROW);
        ComponentConfig entityPull = find(definition, BuiltinSchemas.ENTITY_PULL);
        ComponentConfig areaDamage = find(definition, BuiltinSchemas.AREA_DAMAGE);
        ComponentConfig ignitable = find(definition, BuiltinSchemas.IGNITABLE);
        if (grab == null && hold == null && blockThrow == null && entityPull == null && areaDamage == null && ignitable == null) return null;

        var errors = new ArrayList<String>();
        CompiledBlockGrab compiledGrab = null;
        CompiledBlockHold compiledHold = null;
        CompiledBlockThrow compiledThrow = null;
        CompiledEntityPull compiledPull = null;
        CompiledAreaDamage compiledAreaDamage = null;
        CompiledIgnitable compiledIgnitable = null;
        try {
            if (grab != null) {
                compiledGrab = new CompiledBlockGrab(
                        number(grab.values(), "maxDistance", 6.0),
                        bool(grab.values(), "allowBlockEntities", false),
                        stringSet(grab.values(), "allowedMaterials"),
                        stringSet(grab.values(), "deniedMaterials")
                );
            }
            if (hold != null) {
                compiledHold = new CompiledBlockHold(
                        number(hold.values(), "distance", 2.5),
                        number(hold.values(), "verticalOffset", 0.0)
                );
            }
            if (blockThrow != null) {
                compiledThrow = new CompiledBlockThrow(
                        number(blockThrow.values(), "speed", 1.6),
                        bool(blockThrow.values(), "hurtEntities", false),
                        number(blockThrow.values(), "damagePerBlock", 2.0),
                        integer(blockThrow.values(), "maxDamage", 12),
                        number(blockThrow.values(), "collisionRadius", 0.65),
                        integer(blockThrow.values(), "maxLifetimeTicks", 120),
                        integer(blockThrow.values(), "maxEntityHits", 3)
                );
            }
            if (entityPull != null) {
                compiledPull = new CompiledEntityPull(
                        number(entityPull.values(), "maxDistance", 9.0),
                        number(entityPull.values(), "raySize", 0.75),
                        number(entityPull.values(), "strength", 1.1),
                        number(entityPull.values(), "verticalLift", 0.12),
                        bool(entityPull.values(), "includePlayers", false)
                );
            }
            if (areaDamage != null) {
                compiledAreaDamage = new CompiledAreaDamage(
                        number(areaDamage.values(), "radius", 4.5),
                        number(areaDamage.values(), "maxDamage", 18.0),
                        number(areaDamage.values(), "minDamage", 4.0),
                        integer(areaDamage.values(), "maxTargets", 32),
                        bool(areaDamage.values(), "includePlayers", true),
                        bool(areaDamage.values(), "linearFalloff", true)
                );
            }
            if (ignitable != null) {
                compiledIgnitable = new CompiledIgnitable(
                        integer(ignitable.values(), "fuseTicks", 60),
                        string(ignitable.values(), "ignitionMaterial", "FLINT_AND_STEEL"),
                        number(ignitable.values(), "throwSpeed", 0.9)
                );
            }
        } catch (IllegalArgumentException ex) {
            errors.add(ex.getMessage());
        }

        if (hold != null && grab == null) errors.add("BlockHold requires BlockGrab");
        if (blockThrow != null && hold == null) errors.add("BlockThrow requires BlockHold");
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return new CompiledWorldInteractionSystem(compiledGrab, compiledHold, compiledThrow, compiledPull, compiledAreaDamage, compiledIgnitable);
    }

    private static ComponentConfig find(ItemDefinition definition, NamespacedId type) {
        return definition.components().stream().filter(component -> component.type().equals(type)).findFirst().orElse(null);
    }

    private static double number(Map<String, Object> values, String key, double fallback) {
        Object raw = values.get(key);
        return raw instanceof Number number ? number.doubleValue() : fallback;
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        Object raw = values.get(key);
        return raw instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object raw = values.get(key);
        return raw instanceof Boolean value ? value : fallback;
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object raw = values.get(key);
        return raw instanceof String value ? value : fallback;
    }

    private static Set<String> stringSet(Map<String, Object> values, String key) {
        Object raw = values.get(key);
        if (raw == null) return Set.of();
        if (!(raw instanceof Collection<?> collection)) {
            throw new IllegalArgumentException("BlockGrab." + key + " must be a collection of material ids");
        }
        var result = new LinkedHashSet<String>();
        for (Object value : collection) {
            if (!(value instanceof String text)) {
                throw new IllegalArgumentException("BlockGrab." + key + " must contain only material ids");
            }
            result.add(text);
        }
        return result;
    }
}
