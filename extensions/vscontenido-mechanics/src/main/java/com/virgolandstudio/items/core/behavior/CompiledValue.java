package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

@FunctionalInterface
public interface CompiledValue {
    Object resolve(ExecutionContext context);

    default double resolveDouble(ExecutionContext context, double fallback) {
        Object value = resolve(context);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}
