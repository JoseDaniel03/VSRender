package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.behavior.ConditionConfig;

@FunctionalInterface
public interface ConditionType {
    CompiledCondition compile(ConditionConfig config, BehaviorCompiler compiler);
}
