package com.virgolandstudio.items.api.definition.v2;

import com.virgolandstudio.items.api.behavior.BehaviorDefinition;

import java.util.List;
import java.util.Objects;

/**
 * Non-destructive Item v2 contract. The live v1 compiler is intentionally not switched by introducing this type.
 */
public record ItemDefinitionV2(
        int schemaVersion,
        ItemIdentity identity,
        ItemRepresentation representation,
        StackPolicy stackPolicy,
        ItemComponentSet components,
        ItemStateSchema state,
        List<BehaviorDefinition> behaviors
) {
    public ItemDefinitionV2 {
        if (schemaVersion < 2) throw new IllegalArgumentException("schemaVersion must be >= 2");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(representation, "representation");
        Objects.requireNonNull(stackPolicy, "stackPolicy");
        Objects.requireNonNull(components, "components");
        Objects.requireNonNull(state, "state");
        behaviors = behaviors == null ? List.of() : List.copyOf(behaviors);
    }
}
