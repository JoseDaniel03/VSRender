package com.virgolandstudio.items.core.instance;

import com.virgolandstudio.items.core.compile.CompiledItemDefinition;
import com.virgolandstudio.items.core.state.StateContainer;

import java.util.Objects;
import java.util.UUID;

public final class ItemInstance {
    private final UUID id;
    private final CompiledItemDefinition definition;
    private final StateContainer state;

    public ItemInstance(UUID id, CompiledItemDefinition definition) {
        this.id = Objects.requireNonNull(id, "id");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.state = new StateContainer(definition.stateLayout());
    }

    public UUID id() { return id; }
    public CompiledItemDefinition definition() { return definition; }
    public StateContainer state() { return state; }
}
