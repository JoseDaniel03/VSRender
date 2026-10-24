package com.virgolandstudio.items.core.studio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-only starter compositions for Studio.
 *
 * <p>Templates never introduce ThermosItem, KnifeItem, TicketItem, CameraItem or provider-specific
 * runtime classes. They are ordinary ItemDefinition payloads assembled from generic
 * components and behaviors.</p>
 */
public final class ItemTemplateCatalog {
    private ItemTemplateCatalog() {}

    public static List<Map<String, Object>> all() {
        return List.of(
                thermos(), knife(), smokeable(), ticket(), film(), camera(),
                maintenanceOil(), cleaningKit(), spareParts(), gunsmithTool()
        );
    }

    private static Map<String, Object> thermos() {
        return template(
                "thermos",
                "Termo con agua caliente",
                "Recipiente de laboratorio listo para transferencias atómicas de fluido y temperatura.",
                definition(
                        "virgoland:termo",
                        "Termo",
                        "IRON_NUGGET",
                        "container",
                        List.of(
                                component("virgoland:fluid_storage", Map.of(
                                        "fluid", "water",
                                        "capacity", 1000D,
                                        "initialAmount", 1000D,
                                        "allowedFluids", List.of("water"),
                                        "mixing", false
                                )),
                                component("virgoland:temperature", Map.of(
                                        "initialC", 80D,
                                        "minC", 0D,
                                        "maxC", 100D,
                                        "ambientExchange", 0.05D
                                )),
                                component("virgoland:container", Map.of("enabled", true, "config", Map.of()))
                        ),
                        List.of()
                )
        );
    }

    private static Map<String, Object> knife() {
        return template(
                "cutting_tool",
                "Cuchillo / herramienta de corte",
                "Herramienta genérica que publica la categoría cutting para cualquier bloque que la requiera.",
                definition(
                        "virgoland:cuchillo_prueba",
                        "Cuchillo de prueba",
                        "IRON_NUGGET",
                        "tool",
                        List.of(
                                component("virgoland:tool_category", Map.of(
                                        "enabled", true,
                                        "config", Map.of("categories", List.of("cutting"))
                                )),
                                component("virgoland:durability", Map.of(
                                        "max", 100D,
                                        "initial", 100D,
                                        "breakAtZero", true
                                ))
                        ),
                        List.of()
                )
        );
    }

    private static Map<String, Object> smokeable() {
        return template(
                "smokeable",
                "Consumible fumable",
                "Ejemplo mínimo de usos persistentes. Efectos, partículas e ignición pueden componerse aparte.",
                definition(
                        "virgoland:cigarrillo_prueba",
                        "Cigarrillo de prueba",
                        "STICK",
                        "consumable",
                        List.of(component("virgoland:smokeable", Map.of(
                                "uses", 8,
                                "requiresIgnition", false,
                                "burnSeconds", 120D,
                                "effectProfile", ""
                        ))),
                        List.of(behavior(
                                "INPUT_RIGHT_CLICK",
                                List.of(condition("state_available", Map.of("key", "smoke_uses_remaining", "amount", 1))),
                                List.of(action("consume_state", Map.of("key", "smoke_uses_remaining", "amount", 1)))
                        ))
                )
        );
    }

    private static Map<String, Object> ticket() {
        return template(
                "single_use_ticket",
                "Boleto de un uso",
                "Boleto neutral que conserva su estado consumido; trenes, puertas o sesiones deciden cuándo validarlo.",
                definition(
                        "virgoland:boleto_prueba",
                        "Boleto de prueba",
                        "PAPER",
                        "ticket",
                        List.of(component("virgoland:ticket", Map.of(
                                "service", "rail",
                                "origin", "",
                                "destination", "",
                                "class", "standard",
                                "singleUse", true
                        ))),
                        List.of(behavior(
                                "INPUT_RIGHT_CLICK",
                                List.of(condition("state_equals", Map.of("key", "ticket_consumed", "value", false))),
                                List.of(action("set_state", Map.of("key", "ticket_consumed", "value", true)))
                        ))
                )
        );
    }

    private static Map<String, Object> film() {
        return template(
                "camera_film",
                "Rollo fotográfico",
                "Rollo con exposiciones persistentes. La cámara o un futuro laboratorio fotográfico consumen este contrato.",
                definition(
                        "virgoland:rollo_12",
                        "Rollo de 12 exposiciones",
                        "PAPER",
                        "camera_film",
                        List.of(component("virgoland:film", Map.of(
                                "filmType", "standard",
                                "capacity", 12,
                                "exposed", 0,
                                "developed", false
                        ))),
                        List.of(behavior(
                                "INPUT_RIGHT_CLICK",
                                List.of(condition("state_between", Map.of("key", "film_exposed", "min", 0, "max", 11))),
                                List.of(action("add_state", Map.of("key", "film_exposed", "value", 1)))
                        ))
                )
        );
    }

    private static Map<String, Object> camera() {
        return template(
                "camera",
                "Cámara con rollo reemplazable",
                "Contrato de cámara preparado para un adapter de inventario/film; no acopla la cámara a ningún renderer.",
                definition(
                        "virgoland:camara_prueba",
                        "Cámara de prueba",
                        "SPYGLASS",
                        "camera",
                        List.of(
                                component("virgoland:camera", Map.of(
                                        "filmType", "standard",
                                        "shotsPerFilm", 12,
                                        "exposureSeconds", 0.1D,
                                        "flashSupported", false
                                )),
                                component("virgoland:item_storage", Map.of(
                                        "enabled", true,
                                        "config", Map.of("slots", 1, "purpose", "film")
                                ))
                        ),
                        List.of()
                )
        );
    }

    private static Map<String, Object> maintenanceOil() {
        return template(
                "maintenance_oil",
                "Aceite de mantenimiento",
                "Consumible genérico que recupera lubricación. No conoce armas ni maquinaria concreta.",
                definition(
                        "virgoland:aceite_mantenimiento_prueba",
                        "Aceite de mantenimiento",
                        "HONEY_BOTTLE",
                        "maintenance_supply",
                        List.of(
                                component("virgoland:uses", Map.of("capacity", 8, "initial", 8, "consume", 1)),
                                component("virgoland:maintenance_supply", Map.of(
                                        "dirtRemoval", 0D,
                                        "rustRemoval", 0D,
                                        "lubricationAdded", 25D,
                                        "durabilityRepair", 0D,
                                        "clearJam", false,
                                        "usesPerOperation", 1
                                ))
                        ),
                        List.of()
                )
        );
    }

    private static Map<String, Object> cleaningKit() {
        return template(
                "cleaning_kit",
                "Trapo / kit de limpieza",
                "Consumible de limpieza que baja suciedad y una pequeña cantidad de corrosión sin reparar durabilidad.",
                definition(
                        "virgoland:kit_limpieza_prueba",
                        "Kit de limpieza",
                        "RABBIT_HIDE",
                        "maintenance_supply",
                        List.of(
                                component("virgoland:uses", Map.of("capacity", 12, "initial", 12, "consume", 1)),
                                component("virgoland:tool_category", Map.of(
                                        "enabled", true,
                                        "config", Map.of("categories", List.of("cleaning"))
                                )),
                                component("virgoland:maintenance_supply", Map.of(
                                        "dirtRemoval", 30D,
                                        "rustRemoval", 2D,
                                        "lubricationAdded", 0D,
                                        "durabilityRepair", 0D,
                                        "clearJam", false,
                                        "usesPerOperation", 1
                                ))
                        ),
                        List.of()
                )
        );
    }

    private static Map<String, Object> spareParts() {
        return template(
                "maintenance_spare_parts",
                "Materiales / repuestos",
                "Repuestos consumibles que recuperan durabilidad estructural sin limpiar ni lubricar.",
                definition(
                        "virgoland:repuestos_mecanicos_prueba",
                        "Repuestos mecánicos",
                        "IRON_NUGGET",
                        "maintenance_supply",
                        List.of(
                                component("virgoland:uses", Map.of("capacity", 6, "initial", 6, "consume", 1)),
                                component("virgoland:maintenance_supply", Map.of(
                                        "dirtRemoval", 0D,
                                        "rustRemoval", 0D,
                                        "lubricationAdded", 0D,
                                        "durabilityRepair", 20D,
                                        "clearJam", false,
                                        "usesPerOperation", 1
                                ))
                        ),
                        List.of()
                )
        );
    }

    private static Map<String, Object> gunsmithTool() {
        return template(
                "maintenance_tool",
                "Herramienta de mantenimiento",
                "Herramienta reusable que publica capabilities de mantenimiento y puede despejar un atasco mecánico.",
                definition(
                        "virgoland:herramienta_mantenimiento_prueba",
                        "Herramienta de mantenimiento",
                        "SHEARS",
                        "tool",
                        List.of(
                                component("virgoland:tool_category", Map.of(
                                        "enabled", true,
                                        "config", Map.of("categories", List.of("maintenance", "gunsmithing"))
                                )),
                                component("virgoland:maintenance_supply", Map.of(
                                        "dirtRemoval", 0D,
                                        "rustRemoval", 0D,
                                        "lubricationAdded", 0D,
                                        "durabilityRepair", 0D,
                                        "clearJam", true,
                                        "usesPerOperation", 0
                                ))
                        ),
                        List.of()
                )
        );
    }

    private static Map<String, Object> definition(
            String id,
            String name,
            String material,
            String family,
            List<Map<String, Object>> components,
            List<Map<String, Object>> behaviors
    ) {
        var out = new LinkedHashMap<String, Object>();
        out.put("schemaVersion", 1);
        out.put("id", id);
        out.put("name", name);
        out.put("material", material);
        out.put("family", family);
        out.put("tags", List.of("template"));
        out.put("baseStats", Map.of());
        out.put("state", List.of());
        out.put("componentConfigs", components);
        out.put("behaviors", behaviors);
        return Map.copyOf(out);
    }

    private static Map<String, Object> template(String id, String name, String description, Map<String, Object> definition) {
        return Map.of("id", id, "name", name, "description", description, "definition", definition);
    }

    private static Map<String, Object> component(String type, Map<String, Object> values) {
        return Map.of("type", type, "values", values);
    }

    private static Map<String, Object> behavior(String trigger, List<Map<String, Object>> conditions, List<Map<String, Object>> actions) {
        return Map.of("trigger", trigger, "conditions", conditions, "actions", actions);
    }

    private static Map<String, Object> condition(String type, Map<String, Object> values) {
        return Map.of("type", type, "values", values);
    }

    private static Map<String, Object> action(String type, Map<String, Object> values) {
        return Map.of("type", type, "values", values);
    }
}
