package com.virgolandstudio.items.core.reload;

import java.util.List;

public record ReloadSequence(List<ReloadStage> stages) {
    public ReloadSequence {
        stages = List.copyOf(stages);
        if (stages.isEmpty()) throw new IllegalArgumentException("reload sequence requires at least one stage");
    }
}
