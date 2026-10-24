package com.virgolandstudio.items.api.definition.v2;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Explicit stacking semantics. Minecraft must not merge stateful Virgoland items blindly. */
public record StackPolicy(
        StackMode mode,
        int maxStack,
        Set<String> mergeStateKeys
) {
    public StackPolicy {
        Objects.requireNonNull(mode, "mode");
        if (maxStack < 1) throw new IllegalArgumentException("maxStack must be >= 1");
        if (mode == StackMode.INSTANCE && maxStack != 1) {
            throw new IllegalArgumentException("INSTANCE items must use maxStack=1");
        }
        var keys = new LinkedHashSet<String>();
        if (mergeStateKeys != null) {
            for (String key : mergeStateKeys) {
                if (key == null || key.isBlank()) throw new IllegalArgumentException("merge state key must not be blank");
                keys.add(key.trim());
            }
        }
        mergeStateKeys = Set.copyOf(keys);
    }

    public static StackPolicy stacked(int maxStack) {
        return new StackPolicy(StackMode.STACKED, maxStack, Set.of());
    }

    public static StackPolicy instance() {
        return new StackPolicy(StackMode.INSTANCE, 1, Set.of());
    }

    public static StackPolicy auto(int maxStack, Set<String> mergeStateKeys) {
        return new StackPolicy(StackMode.AUTO, maxStack, mergeStateKeys);
    }
}
