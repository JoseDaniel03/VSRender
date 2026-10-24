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
import java.util.Map;
import java.util.Set;

/**
 * Broad provider-neutral item vocabulary used by Studio and future runtime layers.
 *
 * <p>These schemas are contracts, not claims that every component already has a
 * Paper-side executor. Existing native weapon/resource components remain owned by
 * {@link BuiltinSchemas}; this catalog gives new simulator systems stable data
 * contracts without coupling Items to Blocks, vehicle, railway or mount plugins.</p>
 */
public final class BuiltinSimulationSchemas {
    public static final NamespacedId STATE = id("state");
    public static final NamespacedId COUNTER = id("counter");
    public static final NamespacedId USES = id("uses");
    public static final NamespacedId DURABILITY = id("durability");
    public static final NamespacedId FLUID_STORAGE = id("fluid_storage");
    public static final NamespacedId TEMPERATURE = id("temperature");
    public static final NamespacedId TIMED_PROCESS = id("timed_process");
    public static final NamespacedId VISUAL_STATE = id("visual_state");
    public static final NamespacedId SMOKEABLE = id("smokeable");
    public static final NamespacedId CONSUMABLE = id("consumable");
    public static final NamespacedId DOSE = id("dose");
    public static final NamespacedId SUBSTANCE = id("substance");
    public static final NamespacedId DOCUMENT = id("document");
    public static final NamespacedId TICKET = id("ticket");
    public static final NamespacedId KEY = id("key");
    public static final NamespacedId CAMERA = id("camera");
    public static final NamespacedId FILM = id("film");
    public static final NamespacedId VEHICLE_BINDING = id("vehicle_binding");
    public static final NamespacedId RAIL_BINDING = id("rail_binding");
    public static final NamespacedId MOUNT_BINDING = id("mount_binding");

    private BuiltinSimulationSchemas() {}

    public static void registerAll(ComponentSchemaRegistry registry) {
        registerState(registry);
        registerCounter(registry);
        registerUses(registry);
        registerDurability(registry);
        registerFluid(registry);
        registerTemperature(registry);
        registerTimedProcess(registry);
        registerVisualState(registry);
        registerSmokeable(registry);
        registerConsumable(registry);
        registerDose(registry);
        registerSubstance(registry);
        registerDocument(registry);
        registerTicket(registry);
        registerKey(registry);
        registerCamera(registry);
        registerFilm(registry);
        registerIntegrationBindings(registry);

        // Generic composable contracts. Domain plugins may provide executors later;
        // Items owns the item-side schema and never imports those providers directly.
        String[][] contracts = {
                {"toggle", "Toggle", "state"},
                {"flag", "Flag", "state"},
                {"variable", "Variable", "state"},
                {"charges", "Charges", "state"},
                {"capacity", "Capacity", "state"},
                {"consume_on_use", "Consume On Use", "use"},
                {"refillable", "Refillable", "use"},
                {"wear", "Wear", "durability"},
                {"damageable", "Damageable", "durability"},
                {"repairable", "Repairable", "durability"},
                {"maintenance", "Maintenance", "durability"},
                {"item_storage", "Item Storage", "storage"},
                {"slot", "Slot", "storage"},
                {"input_slot", "Input Slot", "storage"},
                {"output_slot", "Output Slot", "storage"},
                {"filtered_slot", "Filtered Slot", "storage"},
                {"internal_inventory", "Internal Inventory", "storage"},
                {"fluid_input", "Fluid Input", "resource.fluid"},
                {"fluid_output", "Fluid Output", "resource.fluid"},
                {"fluid_transfer", "Fluid Transfer", "resource.fluid"},
                {"fluid_source", "Fluid Source", "resource.fluid"},
                {"fluid_drain", "Fluid Drain", "resource.fluid"},
                {"heat_source", "Heat Source", "resource.temperature"},
                {"cooling", "Cooling", "resource.temperature"},
                {"heat_transfer", "Heat Transfer", "resource.temperature"},
                {"boiling", "Boiling", "resource.temperature"},
                {"fuel_storage", "Fuel Storage", "resource.fuel"},
                {"fuel_consumer", "Fuel Consumer", "resource.fuel"},
                {"fuel_generator", "Fuel Generator", "resource.fuel"},
                {"energy_storage", "Energy Storage", "resource.energy"},
                {"energy_input", "Energy Input", "resource.energy"},
                {"energy_output", "Energy Output", "resource.energy"},
                {"energy_consumer", "Energy Consumer", "resource.energy"},
                {"energy_generator", "Energy Generator", "resource.energy"},
                {"gas_storage", "Gas Storage", "resource.gas"},
                {"gas_consumer", "Gas Consumer", "resource.gas"},
                {"gas_valve", "Gas Valve", "resource.gas"},
                {"processing", "Processing", "processing"},
                {"recipe", "Recipe", "processing"},
                {"progress", "Progress", "processing"},
                {"batch_process", "Batch Process", "processing"},
                {"input_output_process", "Input Output Process", "processing"},
                {"required_tool", "Required Tool", "tools"},
                {"tool_category", "Tool Category", "tools"},
                {"tool_damage", "Tool Damage", "tools"},
                {"tool_efficiency", "Tool Efficiency", "tools"},
                {"cuttable", "Cuttable", "food.preparation"},
                {"cookable", "Cookable", "food.preparation"},
                {"mixable", "Mixable", "food.preparation"},
                {"food_preparation", "Food Preparation", "food.preparation"},
                {"serving", "Serving", "food.preparation"},
                {"drinkable", "Drinkable", "consumable"},
                {"edible", "Edible", "consumable"},
                {"combustible", "Combustible", "consumable.substance"},
                {"container", "Container", "container"},
                {"container_fill", "Container Fill", "container"},
                {"container_drain", "Container Drain", "container"},
                {"container_lid", "Container Lid", "container"},
                {"container_lock", "Container Lock", "container"},
                {"pickup", "Pickup", "world"},
                {"carryable", "Carryable", "world"},
                {"placeable_again", "Placeable Again", "world"},
                {"owner", "Owner", "ownership"},
                {"access_list", "Access List", "ownership"},
                {"role_access", "Role Access", "ownership"},
                {"price", "Price", "economy"},
                {"currency_value", "Currency Value", "economy"},
                {"payment_token", "Payment Token", "economy"},
                {"receipt", "Receipt", "economy"},
                {"serial_number", "Serial Number", "identity"},
                {"certificate", "Certificate", "documents"},
                {"license", "License", "documents"},
                {"permit", "Permit", "documents"},
                {"identity_document", "Identity Document", "documents"},
                {"ownership_document", "Ownership Document", "documents"},
                {"contraband", "Contraband", "legal"},
                {"legality", "Legality", "legal"},
                {"evidence", "Evidence", "legal"},
                {"stolen_status", "Stolen Status", "legal"},
                {"tamper_seal", "Tamper Seal", "legal"},
                {"lock", "Lock", "access"},
                {"access_code", "Access Code", "access"},
                {"access_token", "Access Token", "access"},
                {"model_variant", "Model Variant", "visual"},
                {"animation", "Animation", "visual"},
                {"transform", "Transform", "visual"},
                {"visibility", "Visibility", "visual"},
                {"displayed_item", "Displayed Item", "visual"},
                {"sound", "Sound", "feedback"},
                {"looping_sound", "Looping Sound", "feedback"},
                {"particle", "Particle", "feedback"},
                {"indicator", "Indicator", "feedback"},
                {"display_text", "Display Text", "feedback"},
                {"light", "Light", "technology"},
                {"switchable_light", "Switchable Light", "technology"},
                {"battery", "Battery", "technology"},
                {"mechanical_clock", "Mechanical Clock", "technology"},
                {"stopwatch", "Stopwatch", "technology"},
                {"compass", "Compass", "technology"},
                {"map_item", "Map Item", "technology"},
                {"photo", "Photo", "technology.camera"},
                {"flash", "Flash", "technology.camera"},
                {"film_holder", "Film Holder", "technology.camera"},
                {"vehicle_key", "Vehicle Key", "transport.vehicle"},
                {"vehicle_part", "Vehicle Part", "transport.vehicle"},
                {"vehicle_fuel", "Vehicle Fuel", "transport.vehicle"},
                {"vehicle_battery", "Vehicle Battery", "transport.vehicle"},
                {"vehicle_repair_part", "Vehicle Repair Part", "transport.vehicle"},
                {"rail_ticket", "Rail Ticket", "transport.rail"},
                {"rail_pass", "Rail Pass", "transport.rail"},
                {"luggage_tag", "Luggage Tag", "transport.rail"},
                {"station_token", "Station Token", "transport.rail"},
                {"mount_key", "Mount Key", "transport.mount"},
                {"saddle_part", "Saddle Part", "transport.mount"},
                {"saddlebag", "Saddlebag", "transport.mount"},
                {"bridle", "Bridle", "transport.mount"},
                {"mount_feed", "Mount Feed", "transport.mount"},
                {"block_binding", "Block Binding", "integration.blocks"},
                {"session_binding", "Session Binding", "integration.sessions"},
                {"minigame_binding", "Minigame Binding", "integration.minigames"},
                {"commerce_binding", "Commerce Binding", "integration.commerce"},
                {"npc_binding", "NPC Binding", "integration.npc"},
                {"region_binding", "Region Binding", "integration.world"},
                {"world_condition", "World Condition", "integration.world"},
                {"biome_condition", "Biome Condition", "integration.world"},
                {"weather_condition", "Weather Condition", "integration.world"},
                {"timer", "Timer", "logic"},
                {"delay", "Delay", "logic"},
                {"scheduled_action", "Scheduled Action", "logic"},
                {"random_result", "Random Result", "logic"},
                {"chance", "Chance", "logic"},
                {"event_hook", "Event Hook", "logic"},
                {"condition_hook", "Condition Hook", "logic"},
                {"action_hook", "Action Hook", "logic"},
                {"modifier_hook", "Modifier Hook", "logic"},
                {"persistent_data", "Persistent Data", "persistence"},
                {"reset_policy", "Reset Policy", "persistence"},
                {"drop_table", "Drop Table", "world.drop"},
                {"conditional_drop", "Conditional Drop", "world.drop"},
                {"custom_property", "Custom Property", "extension"},
                {"enum_property", "Enum Property", "extension"},
                {"number_property", "Number Property", "extension"},
                {"boolean_property", "Boolean Property", "extension"}
        };

        for (String[] contract : contracts) {
            registerSimple(registry, contract[0], contract[1], contract[2]);
        }
    }

    private static void registerState(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("key", input("key", "State key", "state", "state"));
        fields.put("initial", input("initial", "Initial value", "default", "state"));
        fields.put("persistent", toggle("persistent", "Persistent", true, "state"));
        registry.register(schema(STATE, "State", "Declares a named logical item state contract.", "state", fields, List.of("STATE_CHANGE")));
    }

    private static void registerCounter(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("key", input("key", "Counter key", "count", "state.counter"));
        fields.put("initial", number("initial", "Initial", 0, -1_000_000, 1_000_000, 1, "", "state.counter"));
        fields.put("min", number("min", "Minimum", 0, -1_000_000, 1_000_000, 1, "", "state.counter"));
        fields.put("max", number("max", "Maximum", 100, -1_000_000, 1_000_000, 1, "", "state.counter"));
        registry.register(schema(COUNTER, "Counter", "Declares bounded count state for generic item mechanics.", "state", fields, List.of("COUNTER_CHANGE")));
    }

    private static void registerUses(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("capacity", number("capacity", "Maximum uses", 1, 1, 1_000_000, 1, "uses", "use"));
        fields.put("initial", number("initial", "Initial uses", 1, 0, 1_000_000, 1, "uses", "use"));
        fields.put("consume", number("consume", "Uses consumed", 1, 0, 1_000_000, 1, "uses", "use"));
        registry.register(schema(USES, "Uses", "Reusable finite-use contract backed by item instance state.", "use", fields, List.of("USE", "USES_EMPTY")));
    }

    private static void registerDurability(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("max", number("max", "Maximum durability", 100, 1, 1_000_000, 1, "durability", "durability"));
        fields.put("initial", number("initial", "Initial durability", 100, 0, 1_000_000, 1, "durability", "durability"));
        fields.put("breakAtZero", toggle("breakAtZero", "Break at zero", true, "durability"));
        registry.register(schema(DURABILITY, "Durability", "Generic authored durability contract independent of vanilla item damage.", "durability", fields, List.of("DURABILITY_CHANGE", "DURABILITY_EMPTY")));
    }

    private static void registerFluid(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("fluid", input("fluid", "Fluid id", "", "resource.fluid"));
        fields.put("capacity", number("capacity", "Capacity", 1000, 0, 1_000_000, 1, "mL", "resource.fluid"));
        fields.put("initialAmount", number("initialAmount", "Initial amount", 0, 0, 1_000_000, 1, "mL", "resource.fluid"));
        fields.put("allowedFluids", new FieldSchema("allowedFluids", "Allowed fluids", FieldType.TAGS, List.of(), null, null, null, "", UiControl.TAG_LIST, "resource.fluid", List.of()));
        fields.put("mixing", toggle("mixing", "Allow mixing", false, "resource.fluid"));
        registry.register(schema(FLUID_STORAGE, "Fluid Storage", "Item-side fluid capacity and content contract.", "resource.fluid", fields, List.of("FLUID_CHANGE", "FLUID_EMPTY", "FLUID_FULL")));
    }

    private static void registerTemperature(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("initialC", number("initialC", "Initial temperature", 20, -273.15, 3000, 0.5, "°C", "resource.temperature"));
        fields.put("minC", number("minC", "Minimum", -50, -273.15, 3000, 0.5, "°C", "resource.temperature"));
        fields.put("maxC", number("maxC", "Maximum", 150, -273.15, 3000, 0.5, "°C", "resource.temperature"));
        fields.put("ambientExchange", number("ambientExchange", "Ambient exchange", 0, 0, 1000, 0.01, "°C/s", "resource.temperature"));
        registry.register(schema(TEMPERATURE, "Temperature", "Persistent temperature state for liquids, food and equipment.", "resource.temperature", fields, List.of("TEMPERATURE_CHANGE", "TEMPERATURE_THRESHOLD")));
    }

    private static void registerTimedProcess(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("processId", input("processId", "Process id", "default", "processing"));
        fields.put("durationSeconds", number("durationSeconds", "Duration", 5, 0, 86_400, 0.05, "seconds", "processing"));
        fields.put("persistAcrossRestart", toggle("persistAcrossRestart", "Persist across restart", true, "processing"));
        fields.put("pauseWhenInactive", toggle("pauseWhenInactive", "Pause while inactive", false, "processing"));
        registry.register(schema(TIMED_PROCESS, "Timed Process", "Timestamp-based process contract; runtime executors may resume it without per-tick persistence.", "processing", fields, List.of("PROCESS_START", "PROCESS_PROGRESS", "PROCESS_COMPLETE", "PROCESS_CANCEL")));
    }

    private static void registerVisualState(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("enabled", toggle("enabled", "Enabled", false, "visual.state"));
        fields.put("source", select("source", "Source", "STATE", "visual.state", List.of("STATE", "COUNTER", "USES", "DURABILITY", "FLUID", "TEMPERATURE", "ENERGY", "CUSTOM")));
        fields.put("sourceKey", input("sourceKey", "Source key", "state", "visual.state"));
        fields.put("mappings", new FieldSchema("mappings", "Mappings", FieldType.MAP, Map.of(), null, null, null, "", UiControl.JSON_EDITOR, "visual.state", List.of()));
        fields.put("interpolate", toggle("interpolate", "Interpolate numeric ranges", false, "visual.state"));
        registry.register(schema(VISUAL_STATE, "Visual State", "Maps logical item state to pre-authored model/animation/display variants without generating geometry.", "visual", fields, List.of("VISUAL_STATE_CHANGE")));
    }

    private static void registerSmokeable(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("uses", number("uses", "Puffs/uses", 8, 1, 1000, 1, "uses", "consumable.smoke"));
        fields.put("requiresIgnition", toggle("requiresIgnition", "Requires ignition", true, "consumable.smoke"));
        fields.put("burnSeconds", number("burnSeconds", "Burn duration", 120, 0, 86_400, 1, "seconds", "consumable.smoke"));
        fields.put("effectProfile", input("effectProfile", "Effect profile", "", "consumable.smoke"));
        registry.register(schema(SMOKEABLE, "Smokeable", "Generic smokeable-item contract; substance/effects remain separate data.", "consumable.smoke", fields, List.of("IGNITE", "INHALE", "EXTINGUISH", "CONSUMED")));
    }

    private static void registerConsumable(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("enabled", toggle("enabled", "Enabled", true, "consumable"));
        fields.put("route", input("route", "Consumption route", "USE", "consumable"));
        fields.put("effectProfile", input("effectProfile", "Actor effect profile", "", "consumable"));
        fields.put("usesPerConsumption", number("usesPerConsumption", "Uses per consumption", 1, 1, 1_000_000, 1, "uses", "consumable"));
        fields.put("removeWhenEmpty", toggle("removeWhenEmpty", "Remove when empty", true, "consumable"));
        fields.put("durationMillis", number("durationMillis", "Effect duration", 0, 0, 86_400_000, 50, "ms", "consumable"));
        registry.register(schema(CONSUMABLE, "Consumable",
                "Generic atomic consumption contract. Actor effects are delegated through ConsumableEffectService before item uses are committed.",
                "consumable", fields, List.of("CONSUME_REQUEST", "CONSUMED", "CONSUMABLE_EMPTY")));
    }

    private static void registerDose(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("enabled", toggle("enabled", "Enabled", true, "consumable.dose"));
        fields.put("amount", number("amount", "Dose amount", 1, 0, 1_000_000, 0.01, "", "consumable.dose"));
        fields.put("unit", input("unit", "Dose unit", "dose", "consumable.dose"));
        registry.register(schema(DOSE, "Dose",
                "Describes the amount and unit transferred by each consumption independently from the substance or item type.",
                "consumable.dose", fields, List.of("CONSUMED")));
    }

    private static void registerSubstance(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("enabled", toggle("enabled", "Enabled", true, "consumable.substance"));
        fields.put("id", input("id", "Substance id", "", "consumable.substance"));
        fields.put("amount", number("amount", "Fallback amount", 1, 0, 1_000_000, 0.01, "", "consumable.substance"));
        fields.put("unit", input("unit", "Fallback unit", "dose", "consumable.substance"));
        registry.register(schema(SUBSTANCE, "Substance",
                "Identifies a substance carried by the item. Dose may override its fallback amount/unit without coupling Items to actor physiology.",
                "consumable.substance", fields, List.of("CONSUMED")));
    }

    private static void registerDocument(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("documentType", input("documentType", "Document type", "generic", "documents"));
        fields.put("issuer", input("issuer", "Issuer", "", "documents"));
        fields.put("holder", input("holder", "Holder", "", "documents"));
        fields.put("serial", input("serial", "Serial", "", "documents"));
        fields.put("metadata", new FieldSchema("metadata", "Metadata", FieldType.MAP, Map.of(), null, null, null, "", UiControl.PROPERTY_MAP, "documents", List.of()));
        registry.register(schema(DOCUMENT, "Document", "Structured document identity usable by tickets, permits, ownership papers and evidence.", "documents", fields, List.of("DOCUMENT_INSPECT")));
    }

    private static void registerTicket(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("service", input("service", "Service", "", "documents.ticket"));
        fields.put("origin", input("origin", "Origin", "", "documents.ticket"));
        fields.put("destination", input("destination", "Destination", "", "documents.ticket"));
        fields.put("class", input("class", "Class", "standard", "documents.ticket"));
        fields.put("singleUse", toggle("singleUse", "Single use", true, "documents.ticket"));
        registry.register(schema(TICKET, "Ticket", "Provider-neutral travel/admission entitlement contract.", "documents.ticket", fields, List.of("TICKET_VALIDATE", "TICKET_CONSUME")));
    }

    private static void registerKey(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("keyId", input("keyId", "Key id", "", "access.key"));
        fields.put("keyType", input("keyType", "Key type", "physical", "access.key"));
        fields.put("oneTime", toggle("oneTime", "One-time key", false, "access.key"));
        registry.register(schema(KEY, "Key", "Generic key/credential contract usable by Blocks, vehicles or other systems through adapters.", "access.key", fields, List.of("KEY_PRESENT", "KEY_USE")));
    }

    private static void registerCamera(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("filmType", input("filmType", "Accepted film", "generic", "technology.camera"));
        fields.put("shotsPerFilm", number("shotsPerFilm", "Shots per film", 12, 1, 1000, 1, "shots", "technology.camera"));
        fields.put("exposureSeconds", number("exposureSeconds", "Exposure", 0.1, 0.001, 60, 0.001, "seconds", "technology.camera"));
        fields.put("flashSupported", toggle("flashSupported", "Supports flash", false, "technology.camera"));
        registry.register(schema(CAMERA, "Camera", "Historical/modern camera item contract with replaceable film state.", "technology.camera", fields, List.of("CAMERA_SHUTTER", "FILM_INSERT", "FILM_REMOVE")));
    }

    private static void registerFilm(ComponentSchemaRegistry registry) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("filmType", input("filmType", "Film type", "generic", "technology.camera"));
        fields.put("capacity", number("capacity", "Exposures", 12, 1, 1000, 1, "shots", "technology.camera"));
        fields.put("exposed", number("exposed", "Already exposed", 0, 0, 1000, 1, "shots", "technology.camera"));
        fields.put("developed", toggle("developed", "Developed", false, "technology.camera"));
        registry.register(schema(FILM, "Film", "Replaceable camera film contract with persistent exposure count.", "technology.camera", fields, List.of("FILM_EXPOSE", "FILM_DEVELOP")));
    }

    private static void registerIntegrationBindings(ComponentSchemaRegistry registry) {
        registerBinding(registry, VEHICLE_BINDING, "Vehicle Binding", "transport.vehicle", "vehicle", List.of("VEHICLE_INTERACT", "VEHICLE_STATE"));
        registerBinding(registry, RAIL_BINDING, "Rail Binding", "transport.rail", "rail", List.of("RAIL_INTERACT", "RAIL_STATE"));
        registerBinding(registry, MOUNT_BINDING, "Mount Binding", "transport.mount", "mount", List.of("MOUNT_INTERACT", "MOUNT_STATE"));
    }

    private static void registerBinding(ComponentSchemaRegistry registry, NamespacedId id, String name, String category, String capability, List<String> triggers) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("capability", input("capability", "Capability", capability, category));
        fields.put("provider", input("provider", "Preferred provider", "", category));
        fields.put("target", input("target", "Target/reference", "", category));
        fields.put("config", new FieldSchema("config", "Provider-neutral config", FieldType.MAP, Map.of(), null, null, null, "", UiControl.JSON_EDITOR, category, List.of()));
        registry.register(schema(id, name, "Neutral integration contract. Provider-specific behavior belongs in a separate adapter plugin.", category, fields, triggers));
    }

    private static void registerSimple(ComponentSchemaRegistry registry, String path, String name, String category) {
        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("enabled", toggle("enabled", "Enabled", true, category));
        fields.put("config", new FieldSchema("config", "Configuration", FieldType.MAP, Map.of(), null, null, null, "", UiControl.JSON_EDITOR, category, List.of()));
        registry.register(schema(id(path), name, "Declarative Items component contract for " + name.toLowerCase() + ".", category, fields, List.of()));
    }

    private static ComponentSchema schema(NamespacedId id, String name, String description, String category, Map<String, FieldSchema> fields, List<String> triggers) {
        return new ComponentSchema(1, id, name, description, category, RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), fields, triggers);
    }

    private static FieldSchema input(String key, String label, String defaultValue, String group) {
        return new FieldSchema(key, label, FieldType.STRING, defaultValue, null, null, null, "", UiControl.INPUT, group, List.of());
    }

    private static FieldSchema toggle(String key, String label, boolean defaultValue, String group) {
        return new FieldSchema(key, label, FieldType.BOOLEAN, defaultValue, null, null, null, "", UiControl.SWITCH, group, List.of());
    }

    private static FieldSchema number(String key, String label, double defaultValue, double min, double max, double step, String unit, String group) {
        return new FieldSchema(key, label, FieldType.NUMBER, defaultValue, min, max, step, unit, UiControl.SLIDER, group, List.of());
    }

    private static FieldSchema select(String key, String label, String defaultValue, String group, List<String> options) {
        return new FieldSchema(key, label, FieldType.ENUM, defaultValue, null, null, null, "", UiControl.SELECT, group, options);
    }

    private static NamespacedId id(String path) {
        return NamespacedId.parse("virgoland:" + path);
    }
}
