package com.virgolandstudio.items.core.validation;

import com.virgolandstudio.items.api.definition.v2.ItemDefinitionV2;
import com.virgolandstudio.items.api.definition.v2.StackMode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Cross-field validation not expressible safely inside individual v2 value objects. */
public final class ItemDefinitionV2Validator {
    public ItemDefinitionV2ValidationReport validate(ItemDefinitionV2 definition) {
        Objects.requireNonNull(definition, "definition");
        var issues = new ArrayList<ItemDefinitionV2Issue>();
        Set<String> stateKeys = new LinkedHashSet<>();
        boolean hasPersistentState = false;

        for (var field : definition.state().fields()) {
            if (!stateKeys.add(field.key())) {
                issues.add(new ItemDefinitionV2Issue(
                        ItemDefinitionV2IssueSeverity.ERROR,
                        "state.fields." + field.key(),
                        "Duplicate state field key"
                ));
            }
            hasPersistentState |= field.persistent();
        }

        for (String key : definition.stackPolicy().mergeStateKeys()) {
            if (!stateKeys.contains(key)) {
                issues.add(new ItemDefinitionV2Issue(
                        ItemDefinitionV2IssueSeverity.WARNING,
                        "stackPolicy.mergeStateKeys." + key,
                        "Merge key is not declared in the v2 state schema; it is treated as compatibility state"
                ));
            }
        }

        if (definition.stackPolicy().mode() == StackMode.STACKED
                && definition.stackPolicy().maxStack() > 1
                && hasPersistentState
                && definition.stackPolicy().mergeStateKeys().isEmpty()) {
            issues.add(new ItemDefinitionV2Issue(
                    ItemDefinitionV2IssueSeverity.WARNING,
                    "stackPolicy.mergeStateKeys",
                    "STACKED item has persistent state but no merge keys; that state will not split stacks"
            ));
        }

        return new ItemDefinitionV2ValidationReport(issues);
    }
}
