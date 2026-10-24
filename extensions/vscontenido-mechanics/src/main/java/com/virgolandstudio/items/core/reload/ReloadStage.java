package com.virgolandstudio.items.core.reload;

import com.virgolandstudio.items.core.behavior.CompiledAction;
import com.virgolandstudio.items.core.behavior.CompiledCondition;

import java.util.List;

/** Immutable compiled stage. Conditions/actions are already resolved before runtime. */
public record ReloadStage(
        String id,
        int durationTicks,
        boolean interruptible,
        List<CompiledCondition> conditions,
        List<CompiledAction> actions,
        List<CompiledCondition> repeatWhile,
        int maxIterations
) {
    public ReloadStage {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("stage id cannot be blank");
        if (durationTicks < 0) throw new IllegalArgumentException("durationTicks must be >= 0");
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
        repeatWhile = repeatWhile == null ? List.of() : List.copyOf(repeatWhile);
        if (maxIterations < 1) throw new IllegalArgumentException("maxIterations must be >= 1");
        if (repeatWhile.isEmpty() && maxIterations != 1) {
            throw new IllegalArgumentException("maxIterations > 1 requires repeatWhile conditions");
        }
    }

    public ReloadStage(String id, int durationTicks) {
        this(id, durationTicks, true, List.of(), List.of(), List.of(), 1);
    }

    public ReloadStage(String id, int durationTicks, boolean interruptible, List<CompiledAction> actions) {
        this(id, durationTicks, interruptible, List.of(), actions, List.of(), 1);
    }
}
