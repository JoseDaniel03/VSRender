package com.virgolandstudio.items.core.studio;

import java.util.List;

/**
 * Read-side migration gate for the persisted Studio definition store.
 *
 * <p>The legacy writer is safe only while every persisted record is readable and no canonical v2
 * document is present. This prevents a v1 save from silently deleting v2 data.</p>
 */
public record StudioItemDefinitionMigrationStatus(
        int storeVersion,
        StudioItemDefinitionReadReport active,
        StudioItemDefinitionReadReport retired,
        List<String> storeIssues
) {
    public StudioItemDefinitionMigrationStatus {
        if (storeVersion < 0) throw new IllegalArgumentException("storeVersion must be >= 0");
        active = active == null ? new StudioItemDefinitionReadReport(List.of(), List.of()) : active;
        retired = retired == null ? new StudioItemDefinitionReadReport(List.of(), List.of()) : retired;
        storeIssues = storeIssues == null ? List.of() : List.copyOf(storeIssues);
    }

    public long legacyCount() {
        return active.legacyCount() + retired.legacyCount();
    }

    public long canonicalCount() {
        return active.canonicalCount() + retired.canonicalCount();
    }

    public long readIssueCount() {
        return (long) active.issues().size() + retired.issues().size();
    }

    public boolean losslessRead() {
        return storeIssues.isEmpty() && active.losslessRead() && retired.losslessRead();
    }

    public boolean legacyWriterSafe() {
        return losslessRead() && canonicalCount() == 0L;
    }
}
