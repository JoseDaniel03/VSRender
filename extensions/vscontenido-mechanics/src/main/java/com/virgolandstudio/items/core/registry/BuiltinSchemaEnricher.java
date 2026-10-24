package com.virgolandstudio.items.core.registry;

import com.virgolandstudio.items.api.schema.ComponentSchema;
import com.virgolandstudio.items.api.schema.FieldSchema;
import com.virgolandstudio.items.api.schema.FieldType;
import com.virgolandstudio.items.api.schema.UiControl;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Small enrichment layer for builtin component contracts that need to evolve without teaching
 * Studio any component-specific UI. Studio still receives one schema-driven component contract.
 */
final class BuiltinSchemaEnricher {
    private BuiltinSchemaEnricher() {}

    static ComponentSchema enrich(ComponentSchema schema) {
        if (!BuiltinSchemas.FIRE_CONTROL.equals(schema.id())) return schema;

        var fields = new LinkedHashMap<String, FieldSchema>(schema.fields());
        fields.putIfAbsent("secondaryMode", new FieldSchema(
                "secondaryMode", "Secondary fire mode", FieldType.ENUM, "NONE",
                null, null, null, null, UiControl.SELECT, "fire.secondary",
                List.of("NONE", "SEMI", "BURST", "AUTO")
        ));
        fields.putIfAbsent("secondaryRpm", FieldSchema.number(
                "secondaryRpm", "Secondary fire rate", 600.0, 1.0, 1200.0, 1.0,
                "rpm", UiControl.SLIDER, "fire.secondary"
        ));
        fields.putIfAbsent("secondaryBurstSize", FieldSchema.number(
                "secondaryBurstSize", "Secondary burst size", 3.0, 1.0, 16.0, 1.0,
                "shots", UiControl.SLIDER, "fire.secondary"
        ));
        fields.putIfAbsent("secondarySpreadMultiplier", FieldSchema.number(
                "secondarySpreadMultiplier", "Secondary spread", 1.0, 0.0, 8.0, 0.05,
                "multiplier", UiControl.SLIDER, "fire.secondary.handling"
        ));
        fields.putIfAbsent("secondaryRecoilMultiplier", FieldSchema.number(
                "secondaryRecoilMultiplier", "Secondary recoil", 1.0, 0.0, 8.0, 0.05,
                "multiplier", UiControl.SLIDER, "fire.secondary.handling"
        ));

        return new ComponentSchema(
                schema.schemaVersion(), schema.id(), schema.name(), schema.description(), schema.category(),
                schema.runtimeMode(), schema.tickInterval(), schema.cost(), schema.requires(), schema.optional(),
                schema.conflicts(), fields, schema.triggers()
        );
    }
}
