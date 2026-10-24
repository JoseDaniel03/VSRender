package com.virgolandstudio.items.core.migration;

import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.api.definition.v2.ItemComponentSet;
import com.virgolandstudio.items.api.definition.v2.ItemDefinitionV2;
import com.virgolandstudio.items.api.definition.v2.ItemIdentity;
import com.virgolandstudio.items.api.definition.v2.ItemRepresentation;
import com.virgolandstudio.items.api.definition.v2.ItemStateSchema;
import com.virgolandstudio.items.api.definition.v2.StackPolicy;

import java.util.Objects;

/**
 * Explicit bridge between the current root definition and Item v2.
 * Representation and stack policy are required inputs because v1 does not contain enough information to infer them safely.
 */
public final class LegacyItemDefinitionV2Migration {
    private LegacyItemDefinitionV2Migration() {}

    public static ItemDefinitionV2 migrate(
            ItemDefinition legacy,
            ItemRepresentation representation,
            StackPolicy stackPolicy
    ) {
        Objects.requireNonNull(legacy, "legacy");
        Objects.requireNonNull(representation, "representation");
        Objects.requireNonNull(stackPolicy, "stackPolicy");

        return new ItemDefinitionV2(
                2,
                new ItemIdentity(legacy.id(), legacy.displayName(), legacy.family(), legacy.tags()),
                representation,
                stackPolicy,
                new ItemComponentSet(legacy.components(), legacy.baseStats()),
                new ItemStateSchema(legacy.state()),
                legacy.behaviors()
        );
    }

    public static ItemDefinition restoreLegacy(ItemDefinitionV2 definition) {
        Objects.requireNonNull(definition, "definition");
        return new ItemDefinition(
                definition.schemaVersion(),
                definition.identity().id(),
                definition.identity().displayName(),
                definition.representation().fallback().material(),
                definition.identity().family(),
                definition.identity().tags(),
                definition.components().compatibilityBaseStats(),
                definition.state().fields(),
                definition.components().configs(),
                definition.behaviors()
        );
    }
}
