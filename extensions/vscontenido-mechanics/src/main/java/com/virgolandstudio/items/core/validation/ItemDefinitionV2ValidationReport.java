package com.virgolandstudio.items.core.validation;

import java.util.List;

public record ItemDefinitionV2ValidationReport(List<ItemDefinitionV2Issue> issues) {
    public ItemDefinitionV2ValidationReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean valid() {
        return issues.stream().noneMatch(issue -> issue.severity() == ItemDefinitionV2IssueSeverity.ERROR);
    }

    public List<ItemDefinitionV2Issue> errors() {
        return issues.stream().filter(issue -> issue.severity() == ItemDefinitionV2IssueSeverity.ERROR).toList();
    }

    public List<ItemDefinitionV2Issue> warnings() {
        return issues.stream().filter(issue -> issue.severity() == ItemDefinitionV2IssueSeverity.WARNING).toList();
    }
}
