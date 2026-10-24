package com.virgolandstudio.items.api.reload;

import java.util.List;

/** Ordered reload sequence authored independently of the Paper runtime. */
public record ReloadSequenceDefinition(List<ReloadStageDefinition> stages) {
    public ReloadSequenceDefinition {
        stages = stages == null ? List.of() : List.copyOf(stages);
        if (stages.isEmpty()) throw new IllegalArgumentException("reload sequence requires at least one stage");
    }
}
