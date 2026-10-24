package com.virgolandstudio.items.api.definition.v2;

import com.virgolandstudio.items.api.state.StateFieldDefinition;

import java.util.List;

public record ItemStateSchema(List<StateFieldDefinition> fields) {
    public ItemStateSchema {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
