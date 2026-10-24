package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

@FunctionalInterface
public interface CompiledCondition {
    boolean test(ExecutionContext context);
}
