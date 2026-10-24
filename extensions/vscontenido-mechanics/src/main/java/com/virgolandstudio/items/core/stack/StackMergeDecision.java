package com.virgolandstudio.items.core.stack;

import java.util.Objects;

/** Explainable stack decision for runtime and Studio diagnostics. */
public record StackMergeDecision(boolean mergeable, String reason) {
    public StackMergeDecision {
        reason = Objects.requireNonNull(reason, "reason").trim();
        if (reason.isEmpty()) throw new IllegalArgumentException("reason must not be blank");
    }

    public static StackMergeDecision merge(String reason) {
        return new StackMergeDecision(true, reason);
    }

    public static StackMergeDecision separate(String reason) {
        return new StackMergeDecision(false, reason);
    }
}
