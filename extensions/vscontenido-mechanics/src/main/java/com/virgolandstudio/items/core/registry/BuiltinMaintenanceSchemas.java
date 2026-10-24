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

/**
 * Structured, opt-in maintenance contracts shared by weapons, tools and future machinery.
 *
 * <p>No component in this catalog assumes the item is a firearm. Existing generic
 * {@code virgoland:wear}, {@code virgoland:repairable} and {@code virgoland:maintenance}
 * contracts remain valid and are intentionally reused instead of duplicated.</p>
 */
public final class BuiltinMaintenanceSchemas {
    public static final NamespacedId CONDITION = id("condition");
    public static final NamespacedId DIRT = id("dirt");
    public static final NamespacedId RUST = id("rust");
    public static final NamespacedId LUBRICATION = id("lubrication");
    public static final NamespacedId CLEANABLE = id("cleanable");
    public static final NamespacedId DEGRADATION = id("degradation");
    public static final NamespacedId MECHANICAL_FAILURE = id("mechanical_failure");
    public static final NamespacedId MAINTENANCE_SUPPLY = id("maintenance_supply");

    private BuiltinMaintenanceSchemas() {}

    public static void registerAll(ComponentSchemaRegistry registry) {
        if (registry.find(CONDITION).isEmpty()) registerCondition(registry);
        if (registry.find(DIRT).isEmpty()) registerDirt(registry);
        if (registry.find(RUST).isEmpty()) registerRust(registry);
        if (registry.find(LUBRICATION).isEmpty()) registerLubrication(registry);
        if (registry.find(CLEANABLE).isEmpty()) registerCleanable(registry);
        if (registry.find(DEGRADATION).isEmpty()) registerDegradation(registry);
        if (registry.find(MECHANICAL_FAILURE).isEmpty()) registerMechanicalFailure(registry);
        if (registry.find(MAINTENANCE_SUPPLY).isEmpty()) registerMaintenanceSupply(registry);
    }

    private static void registerCondition(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("durabilityWeight", number("durabilityWeight", "Durability weight", 0.45, 0, 10, 0.05, "weight", "maintenance.condition"));
        fields.put("wearWeight", number("wearWeight", "Wear weight", 0.20, 0, 10, 0.05, "weight", "maintenance.condition"));
        fields.put("dirtWeight", number("dirtWeight", "Dirt weight", 0.15, 0, 10, 0.05, "weight", "maintenance.condition"));
        fields.put("rustWeight", number("rustWeight", "Rust weight", 0.10, 0, 10, 0.05, "weight", "maintenance.condition"));
        fields.put("lubricationWeight", number("lubricationWeight", "Lubrication weight", 0.10, 0, 10, 0.05, "weight", "maintenance.condition"));
        fields.put("minimumOperational", number("minimumOperational", "Minimum operational condition", 5, 0, 100, 1, "%", "maintenance.condition"));
        registry.register(schema(
                CONDITION,
                "Condition",
                "Derived 0..100 condition profile. Weights are authored data; the score does not replace durability, dirt, rust or lubrication state.",
                "maintenance.condition",
                fields,
                List.of("CONDITION_CHANGE")
        ));
    }

    private static void registerDirt(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("max", number("max", "Maximum dirt", 100, 1, 1_000_000, 1, "dirt", "maintenance.dirt"));
        fields.put("initial", number("initial", "Initial dirt", 0, 0, 1_000_000, 1, "dirt", "maintenance.dirt"));
        registry.register(schema(DIRT, "Dirt", "Persistent dirt/soot/mud accumulation independent from structural damage.", "maintenance.dirt", fields, List.of("DIRT_CHANGE")));
    }

    private static void registerRust(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("max", number("max", "Maximum rust", 100, 1, 1_000_000, 1, "rust", "maintenance.rust"));
        fields.put("initial", number("initial", "Initial rust", 0, 0, 1_000_000, 1, "rust", "maintenance.rust"));
        registry.register(schema(RUST, "Rust / Corrosion", "Persistent corrosion state. Cleaning does not repair it unless an authored action explicitly does so.", "maintenance.rust", fields, List.of("RUST_CHANGE")));
    }

    private static void registerLubrication(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("max", number("max", "Maximum lubrication", 100, 1, 1_000_000, 1, "lubrication", "maintenance.lubrication"));
        fields.put("initial", number("initial", "Initial lubrication", 100, 0, 1_000_000, 1, "lubrication", "maintenance.lubrication"));
        registry.register(schema(LUBRICATION, "Lubrication", "Persistent lubrication/oil level, separate from repair and cleaning.", "maintenance.lubrication", fields, List.of("LUBRICATION_CHANGE")));
    }

    private static void registerCleanable(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("dirtPerOperation", number("dirtPerOperation", "Dirt removed per operation", 25, 0, 1_000_000, 1, "dirt", "maintenance.cleaning"));
        fields.put("rustPerOperation", number("rustPerOperation", "Rust removed per operation", 0, 0, 1_000_000, 1, "rust", "maintenance.cleaning"));
        fields.put("requiresTool", text("requiresTool", "Required tool/capability", "", "maintenance.cleaning"));
        registry.register(schema(CLEANABLE, "Cleanable", "Declares cleaning capacity. By default cleaning removes dirt only and never repairs durability.", "maintenance.cleaning", fields, List.of("CLEAN", "CLEAN_COMPLETE")));
    }

    private static void registerDegradation(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("wearPerUse", number("wearPerUse", "Wear per successful use", 0, 0, 1_000_000, 0.01, "wear", "maintenance.degradation"));
        fields.put("dirtPerUse", number("dirtPerUse", "Dirt per successful use", 0, 0, 1_000_000, 0.01, "dirt", "maintenance.degradation"));
        fields.put("rustPerUse", number("rustPerUse", "Rust per successful use", 0, 0, 1_000_000, 0.01, "rust", "maintenance.degradation"));
        fields.put("lubricationLossPerUse", number("lubricationLossPerUse", "Lubrication lost per successful use", 0, 0, 1_000_000, 0.01, "lubrication", "maintenance.degradation"));
        fields.put("durabilityDamagePerUse", number("durabilityDamagePerUse", "Durability damage per successful use", 0, 0, 1_000_000, 0.01, "durability", "maintenance.degradation"));
        registry.register(schema(
                DEGRADATION,
                "Degradation",
                "Generic cost paid only after a successful authored use. Firearms, knives and machinery can reuse the same profile.",
                "maintenance.degradation",
                fields,
                List.of("USE_SUCCESS", "DEGRADATION_APPLIED")
        ));
    }

    private static void registerMechanicalFailure(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("baseChance", probability("baseChance", "Base failure chance", 0.001));
        fields.put("wearFactor", probability("wearFactor", "Wear contribution", 0.08));
        fields.put("dirtFactor", probability("dirtFactor", "Dirt contribution", 0.12));
        fields.put("rustFactor", probability("rustFactor", "Rust contribution", 0.15));
        fields.put("lowLubricationFactor", probability("lowLubricationFactor", "Low lubrication contribution", 0.10));
        fields.put("lowConditionFactor", probability("lowConditionFactor", "Low condition contribution", 0.12));
        fields.put("maxChance", probability("maxChance", "Maximum failure chance", 0.65));
        registry.register(schema(
                MECHANICAL_FAILURE,
                "Mechanical Failure",
                "Data-driven failure/jam probability profile. The component only computes risk; a trigger decides when a roll is performed.",
                "maintenance.failure",
                fields,
                List.of("FAILURE_CHECK", "MECHANICAL_FAILURE", "JAM", "CLEAR_JAM")
        ));
    }

    private static void registerMaintenanceSupply(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("dirtRemoval", number("dirtRemoval", "Dirt removed", 0, 0, 1_000_000, 1, "dirt", "maintenance.supply"));
        fields.put("rustRemoval", number("rustRemoval", "Rust removed", 0, 0, 1_000_000, 1, "rust", "maintenance.supply"));
        fields.put("lubricationAdded", number("lubricationAdded", "Lubrication added", 0, 0, 1_000_000, 1, "lubrication", "maintenance.supply"));
        fields.put("durabilityRepair", number("durabilityRepair", "Durability repaired", 0, 0, 1_000_000, 1, "durability", "maintenance.supply"));
        fields.put("clearJam", toggle("clearJam", "Clear mechanical jam", false, "maintenance.supply"));
        fields.put("usesPerOperation", number("usesPerOperation", "Uses consumed per operation", 1, 0, 1_000_000, 1, "uses", "maintenance.supply"));
        registry.register(schema(
                MAINTENANCE_SUPPLY,
                "Maintenance Supply",
                "Generic service payload for oil, cleaning supplies, spare parts and reusable maintenance tools. If the item also has Uses, a successful operation consumes the authored amount.",
                "maintenance.supply",
                fields,
                List.of("MAINTENANCE_APPLY")
        ));
    }

    private static ComponentSchema schema(
            NamespacedId id,
            String name,
            String description,
            String category,
            LinkedHashMap<String, FieldSchema> fields,
            List<String> triggers
    ) {
        return new ComponentSchema(
                1, id, name, description, category,
                RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), fields, triggers
        );
    }

    private static FieldSchema number(
            String key,
            String label,
            double fallback,
            double min,
            double max,
            double step,
            String unit,
            String group
    ) {
        return FieldSchema.number(key, label, fallback, min, max, step, unit, UiControl.SLIDER, group);
    }

    private static FieldSchema probability(String key, String label, double fallback) {
        return FieldSchema.number(key, label, fallback, 0, 1, 0.001, "chance", UiControl.SLIDER, "maintenance.failure");
    }

    private static FieldSchema text(String key, String label, String fallback, String group) {
        return new FieldSchema(key, label, FieldType.STRING, fallback, null, null, null, "", UiControl.INPUT, group, List.of());
    }

    private static FieldSchema toggle(String key, String label, boolean fallback, String group) {
        return new FieldSchema(key, label, FieldType.BOOLEAN, fallback, null, null, null, "", UiControl.SWITCH, group, List.of());
    }

    private static NamespacedId id(String path) {
        return NamespacedId.parse("virgoland:" + path);
    }
}
