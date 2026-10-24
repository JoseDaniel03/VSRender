package com.virgolandstudio.items.api.reload;

import com.virgolandstudio.items.api.behavior.ActionConfig;
import com.virgolandstudio.items.api.behavior.ConditionConfig;

import java.util.List;

/** Declarative reload stage as authored by JSON/web/editor. */
public record ReloadStageDefinition(
        String id,
        double durationSeconds,
        boolean interruptible,
        List<ConditionConfig> conditions,
        List<ActionConfig> actions,
        List<ConditionConfig> repeatWhile,
        int maxIterations
) {
    public ReloadStageDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("reload stage id cannot be blank");
        if (!Double.isFinite(durationSeconds) || durationSeconds < 0.0) {
            throw new IllegalArgumentException("durationSeconds must be finite and >= 0");
        }
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
        repeatWhile = repeatWhile == null ? List.of() : List.copyOf(repeatWhile);
        if (maxIterations < 1) throw new IllegalArgumentException("maxIterations must be >= 1");
        if (repeatWhile.isEmpty() && maxIterations != 1) {
            throw new IllegalArgumentException("maxIterations > 1 requires repeatWhile conditions");
        }
    }

    /** Backwards-compatible simple stage: no conditions and no loop. */
    public ReloadStageDefinition(String id, double durationSeconds, boolean interruptible, List<ActionConfig> actions) {
        this(id, durationSeconds, interruptible, List.of(), actions, List.of(), 1);
    }

    /** Conditional but non-looping stage. */
    public ReloadStageDefinition(
            String id,
            double durationSeconds,
            boolean interruptible,
            List<ConditionConfig> conditions,
            List<ActionConfig> actions
    ) {
        this(id, durationSeconds, interruptible, conditions, actions, List.of(), 1);
    }
}
