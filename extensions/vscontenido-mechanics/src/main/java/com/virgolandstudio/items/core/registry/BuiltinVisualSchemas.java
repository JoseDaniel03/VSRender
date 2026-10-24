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

/** Foundational, provider-neutral visual contract for ordinary ItemStacks. */
public final class BuiltinVisualSchemas {
    public static final NamespacedId VISUAL = NamespacedId.parse("virgoland:visual");

    private BuiltinVisualSchemas() {}

    public static void registerAll(ComponentSchemaRegistry registry) {
        if (registry.find(VISUAL).isPresent()) return;

        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("contexts", new FieldSchema(
                "contexts", "Visuals by context", FieldType.MAP, Map.of(),
                null, null, null, null, UiControl.JSON_EDITOR, "visual", List.of()
        ));
        fields.put("variants", new FieldSchema(
                "variants", "Visual variants", FieldType.MAP, Map.of(),
                null, null, null, null, UiControl.JSON_EDITOR, "visual.runtime", List.of()
        ));
        fields.put("animations", new FieldSchema(
                "animations", "Animation visuals", FieldType.MAP, Map.of(),
                null, null, null, null, UiControl.JSON_EDITOR, "visual.runtime", List.of()
        ));

        registry.register(new ComponentSchema(
                1,
                VISUAL,
                "Item Visual",
                "Provider-neutral visual presentation for INVENTORY, HELD and WORLD. Studio owns the authoring flow; Paper applies direct item_model keys and delegates true 3D rigs only when requested.",
                "visual",
                RuntimeMode.EVENT,
                1,
                RuntimeCost.LOW,
                Set.of(),
                Set.of(),
                Set.of(),
                fields,
                List.of()
        ));
    }
}
