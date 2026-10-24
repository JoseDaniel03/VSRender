package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.behavior.ValueConfig;

@FunctionalInterface
public interface ValueProviderType {
    CompiledValue compile(ValueConfig config);
}
