package com.virgolandstudio.items.core.registry;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.schema.ComponentSchema;
import com.virgolandstudio.items.api.schema.FieldSchema;
import com.virgolandstudio.items.api.schema.FieldType;
import com.virgolandstudio.items.api.schema.RuntimeCost;
import com.virgolandstudio.items.api.schema.RuntimeMode;
import com.virgolandstudio.items.api.schema.UiControl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public final class BuiltinSchemas {
    public static final NamespacedId MELEE = NamespacedId.parse("virgoland:melee");
    public static final NamespacedId COOLDOWN = NamespacedId.parse("virgoland:cooldown");
    public static final NamespacedId AMMO = NamespacedId.parse("virgoland:ammo");
    public static final NamespacedId AMMO_CONTAINER = NamespacedId.parse("virgoland:ammo_container");
    public static final NamespacedId FEED = NamespacedId.parse("virgoland:feed_mechanism");
    public static final NamespacedId CHAMBER = NamespacedId.parse("virgoland:chamber");
    public static final NamespacedId RELOAD = NamespacedId.parse("virgoland:reload");
    public static final NamespacedId FIRE_CONTROL = NamespacedId.parse("virgoland:fire_control");
    public static final NamespacedId PROJECTILE = NamespacedId.parse("virgoland:projectile");
    public static final NamespacedId SPREAD = NamespacedId.parse("virgoland:spread");
    public static final NamespacedId RECOIL = NamespacedId.parse("virgoland:recoil");
    public static final NamespacedId ZOOM = NamespacedId.parse("virgoland:zoom");
    public static final NamespacedId IMPACT_RESPONSE = NamespacedId.parse("virgoland:impact_response");
    public static final NamespacedId TRAIL = NamespacedId.parse("virgoland:trail");
    public static final NamespacedId BLOCK_GRAB = NamespacedId.parse("virgoland:block_grab");
    public static final NamespacedId BLOCK_HOLD = NamespacedId.parse("virgoland:block_hold");
    public static final NamespacedId BLOCK_THROW = NamespacedId.parse("virgoland:block_throw");
    public static final NamespacedId ENTITY_PULL = NamespacedId.parse("virgoland:entity_pull");
    public static final NamespacedId AREA_DAMAGE = NamespacedId.parse("virgoland:area_damage");
    public static final NamespacedId IGNITABLE = NamespacedId.parse("virgoland:ignitable");
    public static final NamespacedId ENERGY = NamespacedId.parse("virgoland:energy");
    public static final NamespacedId INVENTORY_FOOTPRINT = NamespacedId.parse("virgoland:inventory_footprint");
    public static final NamespacedId ATTACHMENT_SLOTS = NamespacedId.parse("virgoland:attachment_slots");
    public static final NamespacedId TOOL_CAPABILITIES = NamespacedId.parse("virgoland:tool_capabilities");

    private BuiltinSchemas() {}

    public static void registerAll(ComponentSchemaRegistry registry) {
        var meleeFields = new LinkedHashMap<String, FieldSchema>();
        meleeFields.put("damage", FieldSchema.number("damage", "Damage", 6.0, 0.0, 1000.0, 0.25, "damage", UiControl.SLIDER, "combat"));
        meleeFields.put("reach", FieldSchema.number("reach", "Reach", 3.0, 0.5, 12.0, 0.1, "blocks", UiControl.SLIDER, "combat"));
        registry.register(new ComponentSchema(
                1, MELEE, "Melee", "Adds melee combat behavior.", "combat.melee",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(COOLDOWN), Set.of(), meleeFields, List.of("MELEE_HIT")
        ));

        var cooldownFields = new LinkedHashMap<String, FieldSchema>();
        cooldownFields.put("duration", new FieldSchema(
                "duration", "Duration", FieldType.DURATION, 0.5,
                0.0, 60.0, 0.05, "seconds", UiControl.SLIDER, "timing", List.of()
        ));
        registry.register(new ComponentSchema(
                1, COOLDOWN, "Cooldown", "Reusable timestamp-based cooldown.", "logic",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), cooldownFields, List.of()
        ));

        var footprintFields = new LinkedHashMap<String, FieldSchema>();
        footprintFields.put("slots", FieldSchema.number(
                "slots", "Inventory slots", 1, 1, 9, 1, "slots", UiControl.SLIDER, "inventory.footprint"
        ));
        footprintFields.put("mustBeConsecutive", new FieldSchema(
                "mustBeConsecutive", "Reserve consecutive slots", FieldType.BOOLEAN, true,
                null, null, null, null, UiControl.SWITCH, "inventory.footprint", List.of()
        ));
        footprintFields.put("allowHotbar", new FieldSchema(
                "allowHotbar", "Allow hotbar placement", FieldType.BOOLEAN, true,
                null, null, null, null, UiControl.SWITCH, "inventory.footprint", List.of()
        ));
        registry.register(new ComponentSchema(
                1, INVENTORY_FOOTPRINT, "Inventory Footprint",
                "Reserves additional inventory slots for a single logical item instance.", "inventory",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), footprintFields, List.of("INVENTORY_PLACE", "INVENTORY_REMOVE")
        ));

        var attachmentSlotFields = new LinkedHashMap<String, FieldSchema>();
        attachmentSlotFields.put("slots", new FieldSchema(
                "slots", "Attachment slots", FieldType.TAGS, List.of(),
                null, null, null, null, UiControl.MULTI_SELECT, "customization.attachments",
                List.of("OPTIC", "MUZZLE", "UNDERBARREL", "STOCK", "SIDE", "MAGAZINE", "GRIP")
        ));
        registry.register(new ComponentSchema(
                1, ATTACHMENT_SLOTS, "Attachment Slots",
                "Declares the customization sockets exposed by an item. Slot ids are data, not hardcoded runtime classes.",
                "customization", RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), attachmentSlotFields, List.of("CUSTOMIZATION_OPEN", "ACCESSORY_INSTALL")
        ));

        var toolCapabilityFields = new LinkedHashMap<String, FieldSchema>();
        toolCapabilityFields.put("capabilities", new FieldSchema(
                "capabilities", "Tool capabilities", FieldType.TAGS, List.of(),
                null, null, null, null, UiControl.MULTI_SELECT, "minigame.tools",
                List.of("WRENCH", "MULTIMETER", "PRECISION", "CUTTER", "LIGHT", "MAGNIFIER")
        ));
        toolCapabilityFields.put("quality", FieldSchema.number(
                "quality", "Tool quality", 1.0, 0.0, 100.0, 0.1, "quality", UiControl.SLIDER, "minigame.tools"
        ));
        registry.register(new ComponentSchema(
                1, TOOL_CAPABILITIES, "Tool Capabilities",
                "Declares generic capabilities and context modifiers contributed to minigames. Modifier payloads are data-driven target + operation + value entries.",
                "minigame.tools", RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), toolCapabilityFields, List.of("MINIGAME_CONTEXT_BUILD")
        ));

        var ammoFields = new LinkedHashMap<String, FieldSchema>();
        ammoFields.put("ammoType", new FieldSchema(
                "ammoType", "Ammo type", FieldType.STRING, "generic",
                null, null, null, null, UiControl.INPUT, "ammo", List.of()
        ));
        registry.register(new ComponentSchema(
                1, AMMO, "Ammo", "Declares the ammunition/resource family consumed by the item.", "combat.ammo",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), ammoFields, List.of("FIRE")
        ));

        var containerFields = new LinkedHashMap<String, FieldSchema>();
        containerFields.put("kind", new FieldSchema(
                "kind", "Container kind", FieldType.ENUM, "DETACHABLE_MAGAZINE",
                null, null, null, null, UiControl.SELECT, "ammo.container",
                List.of("DETACHABLE_MAGAZINE", "INTERNAL_MAGAZINE", "CYLINDER", "BREAK_ACTION", "SINGLE_CHAMBER", "BATTERY", "BELT")
        ));
        containerFields.put("storage", new FieldSchema(
                "storage", "Storage model", FieldType.ENUM, "COUNT",
                null, null, null, null, UiControl.SELECT, "ammo.container",
                List.of("COUNT", "SLOTS", "RESOURCE")
        ));
        containerFields.put("capacity", FieldSchema.number(
                "capacity", "Capacity", 30, 1, 10000, 1, "units", UiControl.SLIDER, "ammo.container"
        ));
        containerFields.put("detachable", new FieldSchema(
                "detachable", "Detachable", FieldType.BOOLEAN, true,
                null, null, null, null, UiControl.SWITCH, "ammo.container", List.of()
        ));
        registry.register(new ComponentSchema(
                1, AMMO_CONTAINER, "Ammo Container",
                "Describes where ammunition is stored: magazine, tube, cylinder, chambers, battery or belt.",
                "combat.ammo",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(AMMO), Set.of(FEED), Set.of(), containerFields, List.of("FIRE", "RELOAD_REQUEST")
        ));

        var feedFields = new LinkedHashMap<String, FieldSchema>();
        feedFields.put("mode", new FieldSchema(
                "mode", "Feed mechanism", FieldType.ENUM, "AUTO_CHAMBER",
                null, null, null, null, UiControl.SELECT, "ammo.feed",
                List.of("AUTO_CHAMBER", "MANUAL_CHAMBER", "ROTATING", "DIRECT", "ENERGY_DIRECT")
        ));
        registry.register(new ComponentSchema(
                1, FEED, "Feed Mechanism",
                "Controls how stored ammunition reaches the firing position.", "combat.ammo",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(AMMO, AMMO_CONTAINER), Set.of(CHAMBER), Set.of(), feedFields,
                List.of("FIRE", "RELOAD_STEP")
        ));

        var chamberFields = new LinkedHashMap<String, FieldSchema>();
        chamberFields.put("capacity", FieldSchema.number("capacity", "Capacity", 1, 1, 16, 1, "rounds", UiControl.SLIDER, "ammo.feed"));
        registry.register(new ComponentSchema(
                1, CHAMBER, "Chamber", "Optional separate ready-to-fire chamber.", "combat.ammo",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(AMMO), Set.of(FEED), Set.of(), chamberFields, List.of("FIRE", "RELOAD_REQUEST")
        ));


        var fireFields = new LinkedHashMap<String, FieldSchema>();
        fireFields.put("mode", new FieldSchema(
                "mode", "Default fire mode", FieldType.ENUM, "SEMI",
                null, null, null, null, UiControl.SELECT, "fire.control",
                List.of("SEMI", "BURST", "AUTO")
        ));
        fireFields.put("modes", new FieldSchema(
                "modes", "Available fire modes", FieldType.ENUM, List.of("SEMI"),
                null, null, null, null, UiControl.MULTI_SELECT, "fire.control",
                List.of("SEMI", "BURST", "AUTO")
        ));
        fireFields.put("modeStateKey", new FieldSchema(
                "modeStateKey", "Selected mode state key", FieldType.STRING, "fire_mode",
                null, null, null, null, UiControl.INPUT, "fire.control.advanced", List.of()
        ));
        fireFields.put("rpm", FieldSchema.number("rpm", "Fire rate", 600, 1, 1200, 1, "rpm", UiControl.SLIDER, "fire.control"));
        fireFields.put("burstSize", FieldSchema.number("burstSize", "Burst size", 3, 1, 16, 1, "shots", UiControl.SLIDER, "fire.control"));
        fireFields.put("autoLeaseTicks", FieldSchema.number("autoLeaseTicks", "Auto input lease", 4, 1, 20, 1, "ticks", UiControl.SLIDER, "fire.control"));
        fireFields.put("patternResetTicks", FieldSchema.number("patternResetTicks", "Pattern reset", 8, 1, 1200, 1, "ticks", UiControl.SLIDER, "fire.pattern"));
        fireFields.put("reloadFirePolicy", new FieldSchema(
                "reloadFirePolicy", "Fire while reloading", FieldType.ENUM, "BLOCK",
                null, null, null, null, UiControl.SELECT, "fire.control",
                List.of("BLOCK", "INTERRUPT")
        ));
        registry.register(new ComponentSchema(
                1, FIRE_CONTROL, "Fire Control", "Rate-limits shots and drives semi, burst and automatic cadence.", "combat.ranged",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), fireFields, List.of("FIRE")
        ));

        var projectileFields = new LinkedHashMap<String, FieldSchema>();
        projectileFields.put("speed", FieldSchema.number("speed", "Speed", 4.0, 0.05, 64.0, 0.05, "blocks/tick", UiControl.SLIDER, "projectile.physics"));
        projectileFields.put("gravity", FieldSchema.number("gravity", "Gravity", 0.03, 0.0, 2.0, 0.005, "blocks/tick²", UiControl.SLIDER, "projectile.physics"));
        projectileFields.put("drag", FieldSchema.number("drag", "Drag", 0.995, 0.01, 1.0, 0.001, "multiplier", UiControl.SLIDER, "projectile.physics"));
        projectileFields.put("maxLifetimeTicks", FieldSchema.number("maxLifetimeTicks", "Lifetime", 100, 1, 1200, 1, "ticks", UiControl.SLIDER, "projectile.limits"));
        projectileFields.put("maxDistance", FieldSchema.number("maxDistance", "Max distance", 120.0, 1.0, 1000.0, 1.0, "blocks", UiControl.SLIDER, "projectile.limits"));
        projectileFields.put("raySize", FieldSchema.number("raySize", "Collision radius", 0.08, 0.0, 2.0, 0.01, "blocks", UiControl.SLIDER, "projectile.collision"));
        projectileFields.put("maxEntityPenetrations", FieldSchema.number("maxEntityPenetrations", "Entity penetrations", 0, 0, 16, 1, "entities", UiControl.SLIDER, "projectile.penetration"));
        projectileFields.put("penetrationVelocityMultiplier", FieldSchema.number("penetrationVelocityMultiplier", "Velocity kept after penetration", 0.82, 0.05, 1.0, 0.01, "multiplier", UiControl.SLIDER, "projectile.penetration"));
        registry.register(new ComponentSchema(
                1, PROJECTILE, "Projectile", "Simulates a virtual swept projectile without spawning a Minecraft projectile entity.", "combat.projectile",
                RuntimeMode.ACTIVE_TICK, 1, RuntimeCost.MEDIUM,
                Set.of(FIRE_CONTROL), Set.of(), Set.of(), projectileFields,
                List.of("FIRE", "PROJECTILE_SPAWN", "PROJECTILE_HIT_BLOCK", "PROJECTILE_HIT_ENTITY", "PROJECTILE_EXPIRE")
        ));

        var spreadFields = new LinkedHashMap<String, FieldSchema>();
        spreadFields.put("angle", FieldSchema.number("angle", "Spread cone", 0.0, 0.0, 45.0, 0.05, "degrees", UiControl.SLIDER, "projectile.accuracy"));
        spreadFields.put("angleCurve", new FieldSchema(
                "angleCurve", "Spread by shot", FieldType.CURVE, null,
                null, null, null, "degrees", UiControl.CURVE_EDITOR, "projectile.accuracy.pattern", List.of()
        ));
        spreadFields.put("sneakMultiplier", FieldSchema.number(
                "sneakMultiplier", "Spread while sneaking", 1.0, 0.0, 4.0, 0.05, "multiplier", UiControl.SLIDER, "projectile.accuracy.stance"
        ));
        spreadFields.put("sprintMultiplier", FieldSchema.number(
                "sprintMultiplier", "Spread while sprinting", 1.0, 0.0, 8.0, 0.05, "multiplier", UiControl.SLIDER, "projectile.accuracy.stance"
        ));
        registry.register(new ComponentSchema(
                1, SPREAD, "Spread", "Random cone applied once when a projectile is spawned.", "combat.projectile",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(PROJECTILE), Set.of(), Set.of(), spreadFields, List.of("FIRE")
        ));

        var recoilFields = new LinkedHashMap<String, FieldSchema>();
        recoilFields.put("pitchKick", FieldSchema.number("pitchKick", "Vertical kick", 1.0, 0.0, 45.0, 0.05, "degrees", UiControl.SLIDER, "handling.recoil"));
        recoilFields.put("pitchJitter", FieldSchema.number("pitchJitter", "Vertical variance", 0.0, 0.0, 45.0, 0.05, "degrees", UiControl.SLIDER, "handling.recoil"));
        recoilFields.put("yawKick", FieldSchema.number("yawKick", "Horizontal kick", 0.0, -45.0, 45.0, 0.05, "degrees", UiControl.SLIDER, "handling.recoil"));
        recoilFields.put("yawJitter", FieldSchema.number("yawJitter", "Horizontal variance", 0.25, 0.0, 45.0, 0.05, "degrees", UiControl.SLIDER, "handling.recoil"));
        recoilFields.put("maxPitchKick", FieldSchema.number("maxPitchKick", "Maximum vertical kick", 45.0, 0.0, 90.0, 0.05, "degrees", UiControl.SLIDER, "handling.recoil.limits"));
        recoilFields.put("maxYawKick", FieldSchema.number("maxYawKick", "Maximum horizontal kick", 45.0, 0.0, 180.0, 0.05, "degrees", UiControl.SLIDER, "handling.recoil.limits"));
        recoilFields.put("pitchCurve", new FieldSchema(
                "pitchCurve", "Vertical pattern", FieldType.CURVE, null,
                null, null, null, "degrees", UiControl.CURVE_EDITOR, "handling.recoil.pattern", List.of()
        ));
        recoilFields.put("yawCurve", new FieldSchema(
                "yawCurve", "Horizontal pattern", FieldType.CURVE, null,
                null, null, null, "degrees", UiControl.CURVE_EDITOR, "handling.recoil.pattern", List.of()
        ));
        registry.register(new ComponentSchema(
                1, RECOIL, "Recoil", "Applies authored view kick after an accepted shot.", "combat.handling",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(FIRE_CONTROL), Set.of(), Set.of(), recoilFields, List.of("FIRE")
        ));

        var zoomFields = new LinkedHashMap<String, FieldSchema>();
        zoomFields.put("preset", new FieldSchema(
                "preset", "Zoom preset", FieldType.ENUM, "RED_DOT",
                null, null, null, null, UiControl.SELECT, "ranged.zoom",
                List.of("IRON_SIGHTS", "RED_DOT", "SCOPE_2X", "SCOPE_4X", "SCOPE_6X", "SCOPE_8X", "CUSTOM")
        ));
        zoomFields.put("magnification", FieldSchema.number(
                "magnification", "Magnification", 1.5, 1.0, 10.0, 0.05, "x", UiControl.SLIDER, "ranged.zoom.custom"
        ));
        zoomFields.put("movementMultiplier", FieldSchema.number(
                "movementMultiplier", "Movement while aiming", 0.8, 0.05, 1.0, 0.05, "multiplier", UiControl.SLIDER, "ranged.zoom.custom"
        ));
        zoomFields.put("spreadMultiplier", FieldSchema.number(
                "spreadMultiplier", "Spread while aiming", 0.65, 0.05, 1.0, 0.05, "multiplier", UiControl.SLIDER, "ranged.zoom.custom"
        ));
        zoomFields.put("sneakToZoom", new FieldSchema(
                "sneakToZoom", "Sneak to aim", FieldType.BOOLEAN, true,
                null, null, null, null, UiControl.SWITCH, "ranged.zoom", List.of()
        ));
        zoomFields.put("vanillaSpyglass", new FieldSchema(
                "vanillaSpyglass", "Use legacy vanilla spyglass camera", FieldType.BOOLEAN, false,
                null, null, null, null, UiControl.SWITCH, "ranged.zoom", List.of()
        ));
        zoomFields.put("activationDelaySeconds", FieldSchema.number(
                "activationDelaySeconds", "Aim hold delay", 0.40, 0.0, 10.0, 0.05, "seconds", UiControl.SLIDER, "ranged.zoom"
        ));
        zoomFields.put("reentryDelaySeconds", FieldSchema.number(
                "reentryDelaySeconds", "Scope re-entry after firing", 0.35, 0.0, 10.0, 0.05, "seconds", UiControl.SLIDER, "ranged.zoom"
        ));
        registry.register(new ComponentSchema(
                1, ZOOM, "Zoom", "Reusable ADS/zoom preset. Platform adapters control the visual implementation.", "combat.ranged",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(FIRE_CONTROL), Set.of(), Set.of(), zoomFields, List.of("INPUT_SNEAK_START", "INPUT_SNEAK_END")
        ));

        var impactFields = new LinkedHashMap<String, FieldSchema>();
        impactFields.put("profile", new FieldSchema(
                "profile", "Impact profile", FieldType.STRING, "",
                null, null, null, null, UiControl.INPUT, "projectile.impact.profile", List.of()
        ));
        impactFields.put("blockPenetrationPower", FieldSchema.number(
                "blockPenetrationPower", "Block penetration power", 0.0, 0.0, 1000.0, 0.05, "resistance", UiControl.SLIDER, "projectile.impact"
        ));
        impactFields.put("maxBlockPenetrations", FieldSchema.number(
                "maxBlockPenetrations", "Maximum block penetrations", 0, 0, 16, 1, "blocks", UiControl.SLIDER, "projectile.impact"
        ));
        impactFields.put("maxRicochets", FieldSchema.number(
                "maxRicochets", "Maximum ricochets", 0, 0, 16, 1, "bounces", UiControl.SLIDER, "projectile.impact"
        ));
        impactFields.put("defaultResistancePerBlock", FieldSchema.number(
                "defaultResistancePerBlock", "Default resistance", 10.0, 0.0, 1000.0, 0.05, "resistance/block", UiControl.SLIDER, "projectile.impact.default"
        ));
        impactFields.put("defaultVelocityRetention", FieldSchema.number(
                "defaultVelocityRetention", "Default velocity retention", 0.5, 0.01, 1.0, 0.01, "multiplier", UiControl.SLIDER, "projectile.impact.default"
        ));
        impactFields.put("defaultPenetrable", new FieldSchema(
                "defaultPenetrable", "Unknown materials penetrable", FieldType.BOOLEAN, false,
                null, null, null, null, UiControl.SWITCH, "projectile.impact.default", List.of()
        ));
        impactFields.put("defaultRicochetEnabled", new FieldSchema(
                "defaultRicochetEnabled", "Unknown materials can ricochet", FieldType.BOOLEAN, false,
                null, null, null, null, UiControl.SWITCH, "projectile.impact.default", List.of()
        ));
        impactFields.put("defaultRicochetMinAngle", FieldSchema.number(
                "defaultRicochetMinAngle", "Default ricochet minimum angle", 70.0, 0.0, 90.0, 0.5, "degrees from normal", UiControl.SLIDER, "projectile.impact.default"
        ));
        impactFields.put("defaultRicochetVelocityRetention", FieldSchema.number(
                "defaultRicochetVelocityRetention", "Default ricochet speed retention", 0.65, 0.01, 1.0, 0.01, "multiplier", UiControl.SLIDER, "projectile.impact.default"
        ));
        impactFields.put("rules", new FieldSchema(
                "rules", "Material rules", FieldType.MATERIAL_RULES, List.of(),
                null, null, null, null, UiControl.MATERIAL_RULE_EDITOR, "projectile.impact.materials", List.of()
        ));
        impactFields.put("groupRules", new FieldSchema(
                "groupRules", "Material group rules", FieldType.MATERIAL_GROUP_RULES, List.of(),
                null, null, null, null, UiControl.MATERIAL_GROUP_RULE_EDITOR, "projectile.impact.groups", List.of()
        ));
        registry.register(new ComponentSchema(
                1, IMPACT_RESPONSE, "Impact Response",
                "Resolves block resistance, angle-sensitive thickness and velocity loss without hardcoding materials in Paper.",
                "combat.projectile", RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(PROJECTILE), Set.of(), Set.of(), impactFields, List.of("PROJECTILE_HIT_BLOCK", "PROJECTILE_RICOCHET")
        ));

        var trailFields = new LinkedHashMap<String, FieldSchema>();
        trailFields.put("preset", new FieldSchema(
                "preset", "Sampling preset", FieldType.ENUM, "MEDIUM",
                null, null, null, null, UiControl.SELECT, "projectile.trail", List.of("LOW", "MEDIUM", "HIGH", "CUSTOM")
        ));
        trailFields.put("particle", new FieldSchema(
                "particle", "Particle", FieldType.STRING, "minecraft:crit",
                null, null, null, null, UiControl.INPUT, "projectile.trail", List.of()
        ));
        trailFields.put("count", FieldSchema.number("count", "Particles per sample", 1, 1, 64, 1, "particles", UiControl.SLIDER, "projectile.trail"));
        trailFields.put("intervalTicks", FieldSchema.number("intervalTicks", "Sample interval", 1, 1, 100, 1, "ticks", UiControl.SLIDER, "projectile.trail"));
        trailFields.put("sampleSpacing", FieldSchema.number("sampleSpacing", "Custom sample spacing", 0.50, 0.05, 16.0, 0.05, "blocks", UiControl.SLIDER, "projectile.trail.custom"));
        trailFields.put("maxSamplesPerSegment", FieldSchema.number("maxSamplesPerSegment", "Custom max samples/segment", 16, 1, 128, 1, "samples", UiControl.SLIDER, "projectile.trail.custom"));
        trailFields.put("offset", FieldSchema.number("offset", "Random offset", 0.0, 0.0, 8.0, 0.01, "blocks", UiControl.SLIDER, "projectile.trail"));
        trailFields.put("extra", FieldSchema.number("extra", "Particle extra", 0.0, 0.0, 8.0, 0.01, "value", UiControl.SLIDER, "projectile.trail"));
        registry.register(new ComponentSchema(
                1, TRAIL, "Projectile Trail",
                "Budgeted platform-side trail rendered from virtual projectile segments.",
                "visual.projectile", RuntimeMode.ACTIVE_TICK, 1, RuntimeCost.MEDIUM,
                Set.of(PROJECTILE), Set.of(), Set.of(), trailFields, List.of("PROJECTILE_SPAWN", "PROJECTILE_EXPIRE")
        ));

        var blockGrabFields = new LinkedHashMap<String, FieldSchema>();
        blockGrabFields.put("maxDistance", FieldSchema.number(
                "maxDistance", "Grab distance", 6.0, 0.5, 32.0, 0.1, "blocks", UiControl.SLIDER, "world.block_grab"
        ));
        blockGrabFields.put("allowBlockEntities", new FieldSchema(
                "allowBlockEntities", "Allow block entities", FieldType.BOOLEAN, false,
                null, null, null, null, UiControl.SWITCH, "world.block_grab.safety", List.of()
        ));
        blockGrabFields.put("allowedMaterials", new FieldSchema(
                "allowedMaterials", "Allowed materials", FieldType.BLOCK_SET, List.of(),
                null, null, null, null, UiControl.REFERENCE_PICKER, "world.block_grab.selection", List.of()
        ));
        blockGrabFields.put("deniedMaterials", new FieldSchema(
                "deniedMaterials", "Denied materials", FieldType.BLOCK_SET, List.of(),
                null, null, null, null, UiControl.REFERENCE_PICKER, "world.block_grab.selection", List.of()
        ));
        registry.register(new ComponentSchema(
                1, BLOCK_GRAB, "Block Grab", "Selects and detaches a world block through a platform bridge.", "world.manipulation",
                RuntimeMode.EVENT, 1, RuntimeCost.MEDIUM,
                Set.of(), Set.of(BLOCK_HOLD), Set.of(), blockGrabFields, List.of("INPUT_RIGHT_CLICK", "BLOCK_GRAB")
        ));

        var blockHoldFields = new LinkedHashMap<String, FieldSchema>();
        blockHoldFields.put("distance", FieldSchema.number(
                "distance", "Hold distance", 2.5, 0.5, 12.0, 0.1, "blocks", UiControl.SLIDER, "world.block_hold"
        ));
        blockHoldFields.put("verticalOffset", FieldSchema.number(
                "verticalOffset", "Vertical offset", 0.0, -6.0, 6.0, 0.05, "blocks", UiControl.SLIDER, "world.block_hold"
        ));
        registry.register(new ComponentSchema(
                1, BLOCK_HOLD, "Block Hold", "Keeps a grabbed block attached to the holder using the shared runtime ticker.", "world.manipulation",
                RuntimeMode.ACTIVE_TICK, 1, RuntimeCost.MEDIUM,
                Set.of(BLOCK_GRAB), Set.of(BLOCK_THROW), Set.of(), blockHoldFields, List.of("ACTIVE_TICK", "BLOCK_RELEASE")
        ));

        var blockThrowFields = new LinkedHashMap<String, FieldSchema>();
        blockThrowFields.put("speed", FieldSchema.number(
                "speed", "Throw speed", 1.6, 0.1, 8.0, 0.05, "blocks/tick", UiControl.SLIDER, "world.block_throw"
        ));
        blockThrowFields.put("hurtEntities", new FieldSchema(
                "hurtEntities", "Hurt entities", FieldType.BOOLEAN, true,
                null, null, null, null, UiControl.SWITCH, "world.block_throw", List.of()
        ));
        blockThrowFields.put("damagePerBlock", FieldSchema.number(
                "damagePerBlock", "Falling damage scale", 2.0, 0.0, 100.0, 0.1, "damage/block", UiControl.SLIDER, "world.block_throw"
        ));
        blockThrowFields.put("maxDamage", FieldSchema.number(
                "maxDamage", "Maximum falling damage", 12, 0, 1000, 1, "damage", UiControl.SLIDER, "world.block_throw"
        ));
        blockThrowFields.put("collisionRadius", FieldSchema.number(
                "collisionRadius", "Impact ray size", 0.65, 0.0, 4.0, 0.05, "blocks", UiControl.SLIDER, "world.block_throw.impact"
        ));
        blockThrowFields.put("maxLifetimeTicks", FieldSchema.number(
                "maxLifetimeTicks", "Tracked lifetime", 120, 1, 1200, 1, "ticks", UiControl.SLIDER, "world.block_throw.impact"
        ));
        blockThrowFields.put("maxEntityHits", FieldSchema.number(
                "maxEntityHits", "Maximum entity impacts", 3, 0, 32, 1, "entities", UiControl.SLIDER, "world.block_throw.impact"
        ));
        registry.register(new ComponentSchema(
                1, BLOCK_THROW, "Block Throw", "Releases a held block and tracks its authored kinetic impacts.", "world.manipulation",
                RuntimeMode.ACTIVE_TICK, 1, RuntimeCost.MEDIUM,
                Set.of(BLOCK_HOLD), Set.of(), Set.of(), blockThrowFields,
                List.of("INPUT_LEFT_CLICK", "INPUT_RIGHT_CLICK", "BLOCK_THROW", "BLOCK_THROW_HIT_ENTITY", "BLOCK_THROW_LAND", "BLOCK_THROW_EXPIRE")
        ));

        var entityPullFields = new LinkedHashMap<String, FieldSchema>();
        entityPullFields.put("maxDistance", FieldSchema.number(
                "maxDistance", "Pull target distance", 9.0, 0.5, 64.0, 0.1, "blocks", UiControl.SLIDER, "world.entity_pull"
        ));
        entityPullFields.put("raySize", FieldSchema.number(
                "raySize", "Target assist radius", 0.75, 0.0, 4.0, 0.05, "blocks", UiControl.SLIDER, "world.entity_pull"
        ));
        entityPullFields.put("strength", FieldSchema.number(
                "strength", "Pull strength", 1.1, 0.05, 8.0, 0.05, "velocity", UiControl.SLIDER, "world.entity_pull"
        ));
        entityPullFields.put("verticalLift", FieldSchema.number(
                "verticalLift", "Vertical lift", 0.12, -4.0, 4.0, 0.05, "velocity", UiControl.SLIDER, "world.entity_pull"
        ));
        entityPullFields.put("includePlayers", new FieldSchema(
                "includePlayers", "Can pull players", FieldType.BOOLEAN, false,
                null, null, null, null, UiControl.SWITCH, "world.entity_pull", List.of()
        ));
        registry.register(new ComponentSchema(
                1, ENTITY_PULL, "Entity Pull", "Pulls a ray-selected entity toward the holder through a platform bridge.", "world.manipulation",
                RuntimeMode.EVENT, 1, RuntimeCost.MEDIUM,
                Set.of(), Set.of(), Set.of(), entityPullFields, List.of("INPUT_RIGHT_CLICK", "ENTITY_PULL")
        ));

        var areaDamageFields = new LinkedHashMap<String, FieldSchema>();
        areaDamageFields.put("radius", FieldSchema.number(
                "radius", "Radius", 4.5, 0.1, 64.0, 0.1, "blocks", UiControl.SLIDER, "combat.area"
        ));
        areaDamageFields.put("maxDamage", FieldSchema.number(
                "maxDamage", "Center damage", 18.0, 0.0, 1000.0, 0.25, "damage", UiControl.SLIDER, "combat.area"
        ));
        areaDamageFields.put("minDamage", FieldSchema.number(
                "minDamage", "Edge damage", 4.0, 0.0, 1000.0, 0.25, "damage", UiControl.SLIDER, "combat.area"
        ));
        areaDamageFields.put("maxTargets", FieldSchema.number(
                "maxTargets", "Maximum targets", 32, 1, 256, 1, "entities", UiControl.SLIDER, "combat.area.limits"
        ));
        areaDamageFields.put("includePlayers", new FieldSchema(
                "includePlayers", "Damage players", FieldType.BOOLEAN, true,
                null, null, null, null, UiControl.SWITCH, "combat.area", List.of()
        ));
        areaDamageFields.put("linearFalloff", new FieldSchema(
                "linearFalloff", "Linear falloff", FieldType.BOOLEAN, true,
                null, null, null, null, UiControl.SWITCH, "combat.area", List.of()
        ));
        registry.register(new ComponentSchema(
                1, AREA_DAMAGE, "Area Damage", "Damages multiple nearby entities with a bounded target count and optional linear falloff.",
                "combat.area", RuntimeMode.EVENT, 1, RuntimeCost.MEDIUM,
                Set.of(), Set.of(), Set.of(), areaDamageFields, List.of("FUSE_COMPLETE", "AREA_IMPACT")
        ));

        var ignitableFields = new LinkedHashMap<String, FieldSchema>();
        ignitableFields.put("fuseTicks", FieldSchema.number(
                "fuseTicks", "Fuse", 60, 1, 1200, 1, "ticks", UiControl.SLIDER, "explosive.fuse"
        ));
        ignitableFields.put("ignitionMaterial", new FieldSchema(
                "ignitionMaterial", "Ignition tool", FieldType.STRING, "FLINT_AND_STEEL",
                null, null, null, null, UiControl.INPUT, "explosive.ignition", List.of()
        ));
        ignitableFields.put("throwSpeed", FieldSchema.number(
                "throwSpeed", "Throw speed", 0.9, 0.0, 8.0, 0.05, "velocity", UiControl.SLIDER, "explosive.throw"
        ));
        registry.register(new ComponentSchema(
                1, IGNITABLE, "Ignitable", "Requires an authored tool before starting a tracked fuse.",
                "explosive", RuntimeMode.ACTIVE_TICK, 1, RuntimeCost.MEDIUM,
                Set.of(AREA_DAMAGE), Set.of(), Set.of(), ignitableFields, List.of("INPUT_RIGHT_CLICK", "FUSE_COMPLETE")
        ));

        var energyFields = new LinkedHashMap<String, FieldSchema>();
        energyFields.put("stateKey", new FieldSchema(
                "stateKey", "State key", FieldType.STRING, "energy",
                null, null, null, null, UiControl.INPUT, "resource.energy", List.of()
        ));
        energyFields.put("capacity", FieldSchema.number(
                "capacity", "Capacity", 100.0, 0.0, 100000.0, 1.0, "energy", UiControl.SLIDER, "resource.energy"
        ));
        energyFields.put("regenPerSecond", FieldSchema.number(
                "regenPerSecond", "Regeneration", 0.0, 0.0, 100000.0, 0.1, "energy/second", UiControl.SLIDER, "resource.energy.regen"
        ));
        energyFields.put("regenDelayTicks", FieldSchema.number(
                "regenDelayTicks", "Regeneration delay", 0, 0, 72000, 1, "ticks", UiControl.SLIDER, "resource.energy.regen"
        ));
        registry.register(new ComponentSchema(
                1, ENERGY, "Energy", "Declares an item energy pool backed by typed item-instance state.", "resource",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), energyFields, List.of()
        ));

        var reloadFields = new LinkedHashMap<String, FieldSchema>();
        reloadFields.put("mechanism", new FieldSchema(
                "mechanism", "Reload mechanism", FieldType.ENUM, "SWAP_CONTAINER",
                null, null, null, null, UiControl.SELECT, "reload",
                List.of("SWAP_CONTAINER", "TOP_OFF", "PER_SLOT", "REPLACE_RESOURCE", "CUSTOM")
        ));
        reloadFields.put("sequence", new FieldSchema(
                "sequence", "Reload sequence", FieldType.RELOAD_SEQUENCE, List.of(),
                null, null, null, null, UiControl.RELOAD_SEQUENCE_EDITOR, "reload.sequence", List.of()
        ));
        registry.register(new ComponentSchema(
                1, RELOAD, "Reload",
                "Controls replenishment/replacement timing. The actual sequence remains fully declarative.",
                "combat.reload",
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(AMMO, AMMO_CONTAINER, FEED), Set.of(), Set.of(), reloadFields, List.of("RELOAD_REQUEST")
        ));
    }
}
