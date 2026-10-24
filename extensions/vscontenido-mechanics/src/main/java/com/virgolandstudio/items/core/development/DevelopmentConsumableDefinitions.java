package com.virgolandstudio.items.core.development;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.behavior.ActionConfig;
import com.virgolandstudio.items.api.behavior.BehaviorDefinition;
import com.virgolandstudio.items.api.behavior.ConditionConfig;
import com.virgolandstudio.items.api.behavior.Trigger;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.core.registry.BuiltinSimulationSchemas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Development-only pure-data definitions that exercise generic consumable contracts end to end. */
public final class DevelopmentConsumableDefinitions {
    private DevelopmentConsumableDefinitions() {}

    public static List<ItemDefinition> consumables() {
        return List.of(healingTonic(), fieldRation(), waterFlask(), strongCoffee());
    }

    /** Pure-data medicine; Player owns the actual heal effect. */
    private static ItemDefinition healingTonic() {
        return actorConsumable(
                "virgoland:healing_tonic",
                "Tónico medicinal de prueba",
                "HONEY_BOTTLE",
                Set.of("consumable", "medicine", "development"),
                "DRINK",
                "virgoland:heal",
                20.0,
                "hp",
                3,
                true
        );
    }

    /** Pure-data food; Hunger owns nutrition state. */
    private static ItemDefinition fieldRation() {
        return actorConsumable(
                "virgoland:field_ration",
                "Ración de pan de prueba",
                "BREAD",
                Set.of("consumable", "food", "development"),
                "EAT",
                "virgoland:nutrition",
                25.0,
                "points",
                2,
                true
        );
    }

    /** Pure-data drink with a persistent empty container; Hunger owns hydration state. */
    private static ItemDefinition waterFlask() {
        return actorConsumable(
                "virgoland:water_flask",
                "Cantimplora de agua de prueba",
                "GLASS_BOTTLE",
                Set.of("consumable", "drink", "container", "development"),
                "DRINK",
                "virgoland:hydration",
                30.0,
                "points",
                4,
                false
        );
    }

    /** Pure-data substance carrier; Substances owns the actor exposure. */
    private static ItemDefinition strongCoffee() {
        var components = new ArrayList<ComponentConfig>();
        components.add(new ComponentConfig(BuiltinSimulationSchemas.USES, Map.of(
                "capacity", 3,
                "initial", 3,
                "consume", 1
        )));
        components.add(new ComponentConfig(BuiltinSimulationSchemas.CONSUMABLE, Map.of(
                "enabled", true,
                "route", "DRINK",
                "effectProfile", "",
                "usesPerConsumption", 1,
                "removeWhenEmpty", true,
                "durationMillis", 0
        )));
        components.add(new ComponentConfig(BuiltinSimulationSchemas.DOSE, Map.of(
                "enabled", true,
                "amount", 1.0,
                "unit", "unit"
        )));
        components.add(new ComponentConfig(BuiltinSimulationSchemas.SUBSTANCE, Map.of(
                "enabled", true,
                "id", "virgoland:caffeine",
                "amount", 1.0,
                "unit", "unit"
        )));
        return new ItemDefinition(
                1,
                NamespacedId.parse("virgoland:strong_coffee_test"),
                "Café fuerte de prueba",
                "POTION",
                "consumable",
                Set.of("consumable", "drink", "substance", "development"),
                Map.of(),
                List.of(),
                List.copyOf(components),
                consumeOnRightClick()
        );
    }

    private static ItemDefinition actorConsumable(
            String id,
            String displayName,
            String material,
            Set<String> tags,
            String route,
            String effectProfile,
            double amount,
            String unit,
            int uses,
            boolean removeWhenEmpty
    ) {
        return new ItemDefinition(
                1,
                NamespacedId.parse(id),
                displayName,
                material,
                "consumable",
                tags,
                Map.of(),
                List.of(),
                List.of(
                        new ComponentConfig(BuiltinSimulationSchemas.USES, Map.of(
                                "capacity", uses,
                                "initial", uses,
                                "consume", 1
                        )),
                        new ComponentConfig(BuiltinSimulationSchemas.CONSUMABLE, Map.of(
                                "enabled", true,
                                "route", route,
                                "effectProfile", effectProfile,
                                "usesPerConsumption", 1,
                                "removeWhenEmpty", removeWhenEmpty,
                                "durationMillis", 0
                        )),
                        new ComponentConfig(BuiltinSimulationSchemas.DOSE, Map.of(
                                "enabled", true,
                                "amount", amount,
                                "unit", unit
                        ))
                ),
                consumeOnRightClick()
        );
    }

    private static List<BehaviorDefinition> consumeOnRightClick() {
        return List.of(new BehaviorDefinition(
                Trigger.INPUT_RIGHT_CLICK,
                List.of(new ConditionConfig("consumable_available", Map.of())),
                List.of(new ActionConfig("consume_consumable", Map.of("cancelOnFailure", true)))
        ));
    }
}
