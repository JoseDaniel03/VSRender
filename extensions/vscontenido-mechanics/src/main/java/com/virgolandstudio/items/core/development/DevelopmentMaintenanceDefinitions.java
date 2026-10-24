package com.virgolandstudio.items.core.development;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.core.registry.BuiltinMaintenanceSchemas;
import com.virgolandstudio.items.core.registry.BuiltinSimulationSchemas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Development-only definitions that exercise generic maintenance contracts end to end. */
public final class DevelopmentMaintenanceDefinitions {
    private static final NamespacedId WEAR = NamespacedId.parse("virgoland:wear");
    private static final NamespacedId REPAIRABLE = NamespacedId.parse("virgoland:repairable");
    private static final NamespacedId MAINTENANCE = NamespacedId.parse("virgoland:maintenance");

    private DevelopmentMaintenanceDefinitions() {}

    public static List<ItemDefinition> supplies() {
        return List.of(gunOil(), cleaningKit(), spareParts(), maintenanceTool());
    }

    /**
     * One neutral development profile shared by all test firearms. The values are deliberately
     * visible over a short smoke-test session; production balancing belongs in authored definitions.
     */
    public static List<ComponentConfig> firearmProfile() {
        return List.of(
                new ComponentConfig(BuiltinSimulationSchemas.DURABILITY, Map.of(
                        "max", 100.0,
                        "initial", 100.0,
                        "breakAtZero", true
                )),
                new ComponentConfig(WEAR, Map.of(
                        "enabled", true,
                        "config", Map.of("initial", 0.0, "max", 100.0)
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.DIRT, Map.of(
                        "max", 100.0,
                        "initial", 0.0
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.RUST, Map.of(
                        "max", 100.0,
                        "initial", 0.0
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.LUBRICATION, Map.of(
                        "max", 100.0,
                        "initial", 100.0
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.CONDITION, Map.of(
                        "durabilityWeight", 0.45,
                        "wearWeight", 0.20,
                        "dirtWeight", 0.15,
                        "rustWeight", 0.10,
                        "lubricationWeight", 0.10,
                        "minimumOperational", 5.0
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.CLEANABLE, Map.of(
                        "dirtPerOperation", 25.0,
                        "rustPerOperation", 8.0,
                        "requiresTool", "maintenance"
                )),
                new ComponentConfig(REPAIRABLE, Map.of(
                        "enabled", true,
                        "config", Map.of()
                )),
                new ComponentConfig(MAINTENANCE, Map.of(
                        "enabled", true,
                        "config", Map.of()
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.MECHANICAL_FAILURE, Map.of(
                        "baseChance", 0.001,
                        "wearFactor", 0.03,
                        "dirtFactor", 0.04,
                        "rustFactor", 0.05,
                        "lowLubricationFactor", 0.04,
                        "lowConditionFactor", 0.03,
                        "maxChance", 0.20
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.DEGRADATION, Map.of(
                        "wearPerUse", 0.35,
                        "dirtPerUse", 0.45,
                        "rustPerUse", 0.02,
                        "lubricationLossPerUse", 0.30,
                        "durabilityDamagePerUse", 0.08
                ))
        );
    }

    /**
     * Generic worn-tool profile used by the reusable maintenance tool itself.
     *
     * <p>It intentionally starts partially worn/dirty so a Paper smoke can prove that the same
     * capability-driven workbench accepts a non-weapon Item as a maintenance target. The tool is
     * still a maintenance supply for other objects; those two roles are independent components.</p>
     */
    public static List<ComponentConfig> toolProfile() {
        return List.of(
                new ComponentConfig(BuiltinSimulationSchemas.DURABILITY, Map.of(
                        "max", 100.0,
                        "initial", 72.0,
                        "breakAtZero", true
                )),
                new ComponentConfig(WEAR, Map.of(
                        "enabled", true,
                        "config", Map.of("initial", 28.0, "max", 100.0)
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.DIRT, Map.of(
                        "max", 100.0,
                        "initial", 35.0
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.RUST, Map.of(
                        "max", 100.0,
                        "initial", 12.0
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.LUBRICATION, Map.of(
                        "max", 100.0,
                        "initial", 45.0
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.CONDITION, Map.of(
                        "durabilityWeight", 0.50,
                        "wearWeight", 0.20,
                        "dirtWeight", 0.12,
                        "rustWeight", 0.08,
                        "lubricationWeight", 0.10,
                        "minimumOperational", 5.0
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.CLEANABLE, Map.of(
                        "dirtPerOperation", 20.0,
                        "rustPerOperation", 6.0,
                        "requiresTool", "maintenance"
                )),
                new ComponentConfig(REPAIRABLE, Map.of(
                        "enabled", true,
                        "config", Map.of()
                )),
                new ComponentConfig(MAINTENANCE, Map.of(
                        "enabled", true,
                        "config", Map.of()
                )),
                new ComponentConfig(BuiltinMaintenanceSchemas.DEGRADATION, Map.of(
                        "wearPerUse", 0.20,
                        "dirtPerUse", 0.25,
                        "rustPerUse", 0.01,
                        "lubricationLossPerUse", 0.15,
                        "durabilityDamagePerUse", 0.05
                ))
        );
    }

    private static ItemDefinition gunOil() {
        return supply(
                "virgoland:gun_oil",
                "Aceite para armas",
                "GLASS_BOTTLE",
                Set.of("maintenance_supply", "lubricant", "development"),
                Map.of("lubricationAdded", 35.0),
                8
        );
    }

    private static ItemDefinition cleaningKit() {
        return supply(
                "virgoland:cleaning_kit",
                "Kit de limpieza",
                "PAPER",
                Set.of("maintenance_supply", "cleaning", "development"),
                Map.of("dirtRemoval", 20.0, "rustRemoval", 6.0),
                6
        );
    }

    private static ItemDefinition spareParts() {
        return supply(
                "virgoland:spare_parts",
                "Repuestos",
                "IRON_INGOT",
                Set.of("maintenance_supply", "repair", "development"),
                Map.of("durabilityRepair", 25.0),
                4
        );
    }

    private static ItemDefinition maintenanceTool() {
        var components = new ArrayList<ComponentConfig>();
        components.add(new ComponentConfig(BuiltinMaintenanceSchemas.MAINTENANCE_SUPPLY, Map.of(
                "dirtRemoval", 8.0,
                "rustRemoval", 3.0,
                "lubricationAdded", 8.0,
                "clearJam", true,
                "usesPerOperation", 1
        )));
        components.addAll(toolProfile());
        return new ItemDefinition(
                1,
                NamespacedId.parse("virgoland:maintenance_tool"),
                "Herramienta de mantenimiento",
                "SHEARS",
                "maintenance_supply",
                Set.of("maintenance_supply", "tool", "maintenance_target", "development"),
                Map.of(),
                List.of(),
                List.copyOf(components),
                List.of()
        );
    }

    private static ItemDefinition supply(
            String id,
            String name,
            String material,
            Set<String> tags,
            Map<String, Object> effect,
            int uses
    ) {
        var profile = new java.util.LinkedHashMap<String, Object>(effect);
        profile.put("usesPerOperation", 1);
        return new ItemDefinition(
                1,
                NamespacedId.parse(id),
                name,
                material,
                "maintenance_supply",
                tags,
                Map.of(),
                List.of(),
                List.of(
                        new ComponentConfig(BuiltinMaintenanceSchemas.MAINTENANCE_SUPPLY, Map.copyOf(profile)),
                        new ComponentConfig(BuiltinSimulationSchemas.USES, Map.of(
                                "capacity", uses,
                                "initial", uses,
                                "consume", 1
                        ))
                ),
                List.of()
        );
    }
}
