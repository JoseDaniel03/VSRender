package com.virgolandstudio.items.core.ranged;

import java.util.Objects;

public record ResolvedMaterialImpact(
        CompiledMaterialImpact impact,
        RuleSource source,
        String groupId
) {
    public ResolvedMaterialImpact {
        Objects.requireNonNull(impact, "impact");
        Objects.requireNonNull(source, "source");
        groupId = groupId == null ? "" : groupId;
    }

    public enum RuleSource {
        EXACT,
        GROUP,
        DEFAULT
    }
}
