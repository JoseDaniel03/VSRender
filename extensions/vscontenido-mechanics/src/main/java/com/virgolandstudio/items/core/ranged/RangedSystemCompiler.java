package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.api.ranged.FireMode;
import com.virgolandstudio.items.api.ranged.ReloadFirePolicy;
import com.virgolandstudio.items.api.ranged.ImpactMaterialGroupRule;
import com.virgolandstudio.items.api.ranged.ImpactMaterialRule;
import com.virgolandstudio.items.api.ranged.ImpactProfileDefinition;
import com.virgolandstudio.items.api.ranged.TrailPreset;
import com.virgolandstudio.items.api.ranged.ZoomPreset;
import com.virgolandstudio.items.core.compile.ValidationException;
import com.virgolandstudio.items.core.curve.CompiledCurve;
import com.virgolandstudio.items.core.curve.CurveCompiler;
import com.virgolandstudio.items.core.registry.BuiltinSchemas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RangedSystemCompiler {
    private final MaterialGroupRegistry materialGroups;
    private final ImpactProfileRegistry impactProfiles;

    public RangedSystemCompiler() { this(new MaterialGroupRegistry(), new ImpactProfileRegistry()); }

    public RangedSystemCompiler(MaterialGroupRegistry materialGroups) {
        this(materialGroups, new ImpactProfileRegistry());
    }

    public RangedSystemCompiler(MaterialGroupRegistry materialGroups, ImpactProfileRegistry impactProfiles) {
        this.materialGroups = materialGroups;
        this.impactProfiles = impactProfiles;
    }

    public CompiledRangedSystem compile(ItemDefinition definition) {
        ComponentConfig fire = find(definition, BuiltinSchemas.FIRE_CONTROL);
        ComponentConfig projectile = find(definition, BuiltinSchemas.PROJECTILE);
        ComponentConfig spread = find(definition, BuiltinSchemas.SPREAD);
        ComponentConfig recoil = find(definition, BuiltinSchemas.RECOIL);
        ComponentConfig impact = find(definition, BuiltinSchemas.IMPACT_RESPONSE);
        ComponentConfig trail = find(definition, BuiltinSchemas.TRAIL);
        ComponentConfig zoom = find(definition, BuiltinSchemas.ZOOM);
        if (fire == null && projectile == null && spread == null && recoil == null && impact == null && trail == null && zoom == null) return null;

        var errors = new ArrayList<String>();
        CompiledFireControl fireControl = null;
        CompiledProjectile compiledProjectile = null;
        CompiledSpread compiledSpread = null;
        CompiledRecoil compiledRecoil = null;
        CompiledImpactResponse compiledImpact = null;
        CompiledTrail compiledTrail = null;
        CompiledZoom compiledZoom = null;

        try {
            if (fire != null) {
                FireMode mode = enumValue(fire.values(), "mode", FireMode.class, FireMode.SEMI);
                List<FireMode> modes = enumList(fire.values(), "modes", FireMode.class, List.of(mode));
                String modeStateKey = string(fire.values(), "modeStateKey", modes.size() > 1 ? "fire_mode" : "");
                double rpm = number(fire.values(), "rpm", 600.0);
                int burstSize = integer(fire.values(), "burstSize", 3);
                int autoLeaseTicks = integer(fire.values(), "autoLeaseTicks", 4);
                int patternResetTicks = integer(fire.values(), "patternResetTicks", 8);
                if (rpm > 1200.0) errors.add("FireControl.rpm currently supports up to 1200 RPM (one shot per server tick)");
                ReloadFirePolicy reloadFirePolicy = enumValue(
                        fire.values(), "reloadFirePolicy", ReloadFirePolicy.class, ReloadFirePolicy.BLOCK
                );

                String secondaryRaw = string(fire.values(), "secondaryMode", "NONE").toUpperCase(Locale.ROOT);
                FireMode secondaryMode = "NONE".equals(secondaryRaw)
                        ? null
                        : enumValue(Map.of("secondaryMode", secondaryRaw), "secondaryMode", FireMode.class, FireMode.SEMI);
                double secondaryRpm = number(fire.values(), "secondaryRpm", rpm);
                int secondaryBurstSize = integer(fire.values(), "secondaryBurstSize", burstSize);
                if (secondaryMode != null && secondaryRpm > 1200.0) {
                    errors.add("FireControl.secondaryRpm currently supports up to 1200 RPM (one shot per server tick)");
                }

                fireControl = new CompiledFireControl(
                        mode, modes, modeStateKey, rpm, burstSize, autoLeaseTicks, patternResetTicks,
                        reloadFirePolicy, secondaryMode, secondaryRpm, secondaryBurstSize
                );
            }
            if (projectile != null) {
                compiledProjectile = new CompiledProjectile(
                        number(projectile.values(), "speed", 4.0),
                        number(projectile.values(), "gravity", 0.03),
                        number(projectile.values(), "drag", 0.995),
                        integer(projectile.values(), "maxLifetimeTicks", 100),
                        number(projectile.values(), "maxDistance", 120.0),
                        number(projectile.values(), "raySize", 0.08),
                        integer(projectile.values(), "maxEntityPenetrations", 0),
                        number(projectile.values(), "penetrationVelocityMultiplier", 0.82)
                );
            }
            if (spread != null) {
                compiledSpread = new CompiledSpread(
                        number(spread.values(), "angle", 0.0),
                        curveOrNull(spread.values().get("angleCurve")),
                        number(spread.values(), "sneakMultiplier", 1.0),
                        number(spread.values(), "sprintMultiplier", 1.0)
                );
            }
            if (recoil != null) {
                compiledRecoil = new CompiledRecoil(
                        number(recoil.values(), "pitchKick", 0.0),
                        number(recoil.values(), "pitchJitter", 0.0),
                        number(recoil.values(), "yawKick", 0.0),
                        number(recoil.values(), "yawJitter", 0.0),
                        curveOrNull(recoil.values().get("pitchCurve")),
                        curveOrNull(recoil.values().get("yawCurve")),
                        number(recoil.values(), "maxPitchKick", 45.0),
                        number(recoil.values(), "maxYawKick", 45.0)
                );
            }
            if (impact != null) compiledImpact = compileImpact(impact.values());
            if (trail != null) {
                TrailPreset preset = enumValue(trail.values(), "preset", TrailPreset.class, TrailPreset.MEDIUM);
                double sampleSpacing = preset == TrailPreset.CUSTOM
                        ? number(trail.values(), "sampleSpacing", 0.50)
                        : preset.sampleSpacing();
                int maxSamplesPerSegment = preset == TrailPreset.CUSTOM
                        ? integer(trail.values(), "maxSamplesPerSegment", 16)
                        : preset.maxSamplesPerSegment();
                compiledTrail = new CompiledTrail(
                        preset,
                        string(trail.values(), "particle", "minecraft:crit"),
                        integer(trail.values(), "count", 1),
                        integer(trail.values(), "intervalTicks", 1),
                        number(trail.values(), "offset", 0.0),
                        number(trail.values(), "extra", 0.0),
                        sampleSpacing,
                        maxSamplesPerSegment
                );
            }
            if (zoom != null) {
                ZoomPreset preset = enumValue(zoom.values(), "preset", ZoomPreset.class, ZoomPreset.RED_DOT);
                double magnification = preset == ZoomPreset.CUSTOM
                        ? number(zoom.values(), "magnification", 1.5)
                        : preset.magnification();
                double movementMultiplier = preset == ZoomPreset.CUSTOM
                        ? number(zoom.values(), "movementMultiplier", 0.8)
                        : preset.movementMultiplier();
                double spreadMultiplier = preset == ZoomPreset.CUSTOM
                        ? number(zoom.values(), "spreadMultiplier", 0.65)
                        : preset.spreadMultiplier();
                compiledZoom = new CompiledZoom(
                        preset,
                        magnification,
                        movementMultiplier,
                        spreadMultiplier,
                        bool(zoom.values(), "sneakToZoom", true),
                        bool(zoom.values(), "vanillaSpyglass", false),
                        secondsToTicks(number(zoom.values(), "activationDelaySeconds", 0.40)),
                        secondsToTicks(number(zoom.values(), "reentryDelaySeconds", 0.35))
                );
            }
        } catch (IllegalArgumentException ex) {
            errors.add(ex.getMessage());
        }

        if (projectile != null && fire == null) errors.add("Projectile requires FireControl");
        if (spread != null && projectile == null) errors.add("Spread requires Projectile");
        if (recoil != null && fire == null) errors.add("Recoil requires FireControl");
        if (impact != null && projectile == null) errors.add("ImpactResponse requires Projectile");
        if (trail != null && projectile == null) errors.add("Trail requires Projectile");
        if (zoom != null && fire == null) errors.add("Zoom requires FireControl");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        return new CompiledRangedSystem(fireControl, compiledProjectile, compiledSpread, compiledRecoil, compiledImpact, compiledTrail, compiledZoom);
    }

    private CompiledImpactResponse compileImpact(Map<String, Object> values) {
        ImpactProfileDefinition profile = null;
        String profileId = string(values, "profile", "");
        if (!profileId.isBlank()) {
            profile = impactProfiles.find(profileId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown impact profile: " + profileId));
        }

        double power = number(values, "blockPenetrationPower", profile == null ? 0.0 : profile.blockPenetrationPower());
        int maxBlocks = integer(values, "maxBlockPenetrations", profile == null ? 0 : profile.maxBlockPenetrations());
        int maxRicochets = integer(values, "maxRicochets", profile == null ? 0 : profile.maxRicochets());
        var defaultResponse = new CompiledMaterialImpact(
                number(values, "defaultResistancePerBlock", profile == null ? 10.0 : profile.defaultResistancePerBlock()),
                number(values, "defaultVelocityRetention", profile == null ? 0.5 : profile.defaultVelocityRetention()),
                bool(values, "defaultPenetrable", profile != null && profile.defaultPenetrable()),
                bool(values, "defaultRicochetEnabled", profile != null && profile.defaultRicochetEnabled()),
                number(values, "defaultRicochetMinAngle", profile == null ? 70.0 : profile.defaultRicochetMinAngleDegrees()),
                number(values, "defaultRicochetVelocityRetention", profile == null ? 0.65 : profile.defaultRicochetVelocityRetention())
        );

        var exactRules = new LinkedHashMap<String, CompiledMaterialImpact>();
        if (profile != null) {
            for (ImpactMaterialRule rule : profile.rules()) exactRules.put(rule.materialId(), compileRule(rule));
        }
        Object rawRules = values.get("rules");
        if (rawRules != null) {
            if (!(rawRules instanceof List<?> list)) throw new IllegalArgumentException("ImpactResponse.rules must be a list");
            if (list.size() > 128) throw new IllegalArgumentException("ImpactResponse.rules supports at most 128 exact material rules");
            var localIds = new HashSet<String>();
            for (Object raw : list) {
                ImpactMaterialRule rule = parseImpactRule(raw);
                if (!localIds.add(rule.materialId())) {
                    throw new IllegalArgumentException("Duplicate ImpactResponse material rule: " + rule.materialId());
                }
                exactRules.put(rule.materialId(), compileRule(rule)); // Local authored rule overrides profile exact rule.
            }
        }

        var localGroups = new ArrayList<ImpactMaterialGroupRule>();
        Object rawGroupRules = values.get("groupRules");
        if (rawGroupRules != null) {
            if (!(rawGroupRules instanceof List<?> list)) throw new IllegalArgumentException("ImpactResponse.groupRules must be a list");
            if (list.size() > 64) throw new IllegalArgumentException("ImpactResponse.groupRules supports at most 64 material-group rules");
            var seenGroups = new HashSet<String>();
            for (Object raw : list) {
                ImpactMaterialGroupRule rule = parseImpactGroupRule(raw);
                if (!seenGroups.add(rule.groupId())) {
                    throw new IllegalArgumentException("Duplicate ImpactResponse material group rule: " + rule.groupId());
                }
                localGroups.add(rule);
            }
        }

        var authoredGroups = new ArrayList<ImpactMaterialGroupRule>(localGroups);
        if (profile != null) {
            var overridden = new HashSet<String>();
            for (ImpactMaterialGroupRule rule : localGroups) overridden.add(rule.groupId());
            for (ImpactMaterialGroupRule rule : profile.groupRules()) {
                if (!overridden.contains(rule.groupId())) authoredGroups.add(rule);
            }
        }

        var expandedGroupRules = new LinkedHashMap<String, CompiledImpactResponse.GroupMatch>();
        for (ImpactMaterialGroupRule rule : authoredGroups) {
            var materials = materialGroups.find(rule.groupId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown material group: " + rule.groupId()));
            var compiled = compileRule(rule);
            for (String materialId : materials) {
                // Local groups are expanded first, so they override overlapping profile groups without hot-path work.
                expandedGroupRules.putIfAbsent(materialId, new CompiledImpactResponse.GroupMatch(rule.groupId(), compiled));
            }
        }

        return new CompiledImpactResponse(power, maxBlocks, maxRicochets, defaultResponse, exactRules, expandedGroupRules);
    }

    private static CompiledMaterialImpact compileRule(ImpactMaterialRule rule) {
        return new CompiledMaterialImpact(
                rule.resistancePerBlock(), rule.velocityRetention(), rule.penetrable(),
                rule.ricochetEnabled(), rule.ricochetMinAngleDegrees(), rule.ricochetVelocityRetention()
        );
    }

    private static CompiledMaterialImpact compileRule(ImpactMaterialGroupRule rule) {
        return new CompiledMaterialImpact(
                rule.resistancePerBlock(), rule.velocityRetention(), rule.penetrable(),
                rule.ricochetEnabled(), rule.ricochetMinAngleDegrees(), rule.ricochetVelocityRetention()
        );
    }

    private static ImpactMaterialRule parseImpactRule(Object raw) {
        if (raw instanceof ImpactMaterialRule rule) return rule;
        if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("ImpactResponse rule must be an ImpactMaterialRule or map");
        Object materialRaw = map.get("materialId");
        if (!(materialRaw instanceof String material) || material.isBlank()) {
            throw new IllegalArgumentException("ImpactResponse rule materialId must be a non-blank string");
        }
        return new ImpactMaterialRule(
                material,
                mapNumber(map, "resistancePerBlock", 1.0),
                mapNumber(map, "velocityRetention", 0.75),
                mapBoolean(map, "penetrable", true),
                mapBoolean(map, "ricochetEnabled", false),
                mapNumber(map, "ricochetMinAngle", 70.0),
                mapNumber(map, "ricochetVelocityRetention", 0.65)
        );
    }

    private static ImpactMaterialGroupRule parseImpactGroupRule(Object raw) {
        if (raw instanceof ImpactMaterialGroupRule rule) return rule;
        if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("ImpactResponse group rule must be an ImpactMaterialGroupRule or map");
        Object groupRaw = map.get("groupId");
        if (!(groupRaw instanceof String group) || group.isBlank()) {
            throw new IllegalArgumentException("ImpactResponse group rule groupId must be a non-blank string");
        }
        return new ImpactMaterialGroupRule(
                group,
                mapNumber(map, "resistancePerBlock", 1.0),
                mapNumber(map, "velocityRetention", 0.75),
                mapBoolean(map, "penetrable", true),
                mapBoolean(map, "ricochetEnabled", false),
                mapNumber(map, "ricochetMinAngle", 70.0),
                mapNumber(map, "ricochetVelocityRetention", 0.65)
        );
    }

    private static CompiledCurve curveOrNull(Object raw) {
        return raw == null ? null : CurveCompiler.compile(raw);
    }

    private static ComponentConfig find(ItemDefinition definition, com.virgolandstudio.items.api.NamespacedId type) {
        return definition.components().stream().filter(component -> component.type().equals(type)).findFirst().orElse(null);
    }

    private static int secondsToTicks(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0.0 || seconds > 10.0) {
            throw new IllegalArgumentException("Zoom activationDelaySeconds must be finite and within [0,10]");
        }
        return (int) Math.round(seconds * 20.0);
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
        return raw instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static double mapNumber(Map<?, ?> values, String key, double fallback) {
        Object raw = values.get(key);
        return raw instanceof Number number ? number.doubleValue() : fallback;
    }

    private static boolean mapBoolean(Map<?, ?> values, String key, boolean fallback) {
        Object raw = values.get(key);
        return raw instanceof Boolean value ? value : fallback;
    }

    private static <E extends Enum<E>> List<E> enumList(Map<String, Object> values, String key, Class<E> type, List<E> fallback) {
        Object raw = values.get(key);
        if (raw == null) return fallback;
        if (!(raw instanceof List<?> list)) throw new IllegalArgumentException(key + " must be a list");
        if (list.isEmpty()) return fallback;
        var out = new ArrayList<E>(list.size());
        for (Object entry : list) {
            if (!(entry instanceof String text) || text.isBlank()) {
                throw new IllegalArgumentException(key + " entries must be non-blank strings");
            }
            try {
                out.add(Enum.valueOf(type, text.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(key + " has unsupported value: " + text);
            }
        }
        return List.copyOf(out);
    }

    private static <E extends Enum<E>> E enumValue(Map<String, Object> values, String key, Class<E> type, E fallback) {
        Object raw = values.get(key);
        if (!(raw instanceof String string) || string.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, string.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(key + " has unsupported value: " + string);
        }
    }
}