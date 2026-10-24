package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.behavior.ActionConfig;

@FunctionalInterface
public interface ActionType {
    CompiledAction compile(ActionConfig config, BehaviorCompiler compiler);
}
