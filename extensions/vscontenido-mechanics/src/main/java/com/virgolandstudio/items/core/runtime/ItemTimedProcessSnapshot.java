package com.virgolandstudio.items.core.runtime;

import java.util.UUID;

/** Snapshot of one timestamp-backed generic item process. */
public record ItemTimedProcessSnapshot(
        UUID instanceId,
        String processId,
        Status status,
        long deadlineEpochMillis,
        long remainingMillis
) {
    public enum Status {
        IDLE,
        RUNNING,
        DUE
    }
}
