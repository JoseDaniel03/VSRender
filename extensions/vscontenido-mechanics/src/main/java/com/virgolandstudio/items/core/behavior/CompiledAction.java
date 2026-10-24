package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

@FunctionalInterface
public interface CompiledAction {
    ActionResult execute(ExecutionContext context);
}
