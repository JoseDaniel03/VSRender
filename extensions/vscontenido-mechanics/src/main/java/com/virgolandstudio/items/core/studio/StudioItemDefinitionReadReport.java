package com.virgolandstudio.items.core.studio;

import java.util.List;

/** Batch dual-read result used before any write/migration step. */
public record StudioItemDefinitionReadReport(
        List<StudioItemDefinitionDocument> documents,
        List<StudioItemDefinitionReadIssue> issues
) {
    public StudioItemDefinitionReadReport {
        documents = documents == null ? List.of() : List.copyOf(documents);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public long legacyCount() {
        return documents.stream().filter(document -> document.format() == StudioItemDefinitionFormat.LEGACY_V1).count();
    }

    public long canonicalCount() {
        return documents.stream().filter(document -> document.format() == StudioItemDefinitionFormat.CANONICAL_V2).count();
    }

    public boolean losslessRead() {
        return issues.isEmpty();
    }
}
