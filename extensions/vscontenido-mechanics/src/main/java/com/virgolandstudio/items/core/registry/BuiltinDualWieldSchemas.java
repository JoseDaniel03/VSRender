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

/** Schema-driven authoring contract for pairing independently stateful items in both hands. */
public final class BuiltinDualWieldSchemas {
    public static final NamespacedId DUAL_WIELD = NamespacedId.parse("virgoland:dual_wield");

    private BuiltinDualWieldSchemas() {}

    public static void registerAll(ComponentSchemaRegistry registry) {
        if (registry.find(DUAL_WIELD).isPresent()) return;

        var fields = new LinkedHashMap<String, FieldSchema>();
        fields.put("compatibilityTags", new FieldSchema(
                "compatibilityTags", "Compatibility tags", FieldType.TAGS, List.of("pistol"),
                null, null, null, null, UiControl.AUTO, "dual_wield.compatibility", List.of()
        ));
        fields.put("pairingMode", new FieldSchema(
                "pairingMode", "Pairing mode", FieldType.ENUM, "SAME_TAG",
                null, null, null, null, UiControl.SELECT, "dual_wield.compatibility",
                List.of("SAME_TAG", "ANY_DUAL_WIELD")
        ));
        fields.put("reloadPolicy", new FieldSchema(
                "reloadPolicy", "Reload policy", FieldType.ENUM, "SEQUENTIAL",
                null, null, null, null, UiControl.SELECT, "dual_wield.reload",
                List.of("SEQUENTIAL", "ACTIVE_HAND_ONLY")
        ));
        fields.put("spreadMultiplier", FieldSchema.number(
                "spreadMultiplier", "Dual-wield spread", 1.25, 0.1, 8.0, 0.05,
                "multiplier", UiControl.SLIDER, "dual_wield.handling"
        ));
        fields.put("recoilMultiplier", FieldSchema.number(
                "recoilMultiplier", "Dual-wield recoil", 1.15, 0.1, 8.0, 0.05,
                "multiplier", UiControl.SLIDER, "dual_wield.handling"
        ));

        registry.register(new ComponentSchema(
                1, DUAL_WIELD, "Dual Wield",
                "Allows this item to pair with a compatible independently stateful item in the other hand.",
                "combat.handling", RuntimeMode.EVENT, 1, RuntimeCost.LOW,
                Set.of(), Set.of(), Set.of(), fields, List.of()
        ));
    }
}
