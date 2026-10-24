package com.virgolandstudio.items.core.runtime;

/** Result of an atomic generic item-state operation. */
public record ItemRuntimeMutationResult(
        Status status,
        Object previousValue,
        Object value,
        String detail
) {
    public enum Status {
        APPLIED,
        NO_CHANGE,
        NOT_FOUND,
        UNKNOWN_KEY,
        TYPE_MISMATCH,
        INSUFFICIENT,
        CAPACITY_EXCEEDED,
        INVALID_AMOUNT
    }

    public boolean applied() {
        return status == Status.APPLIED;
    }

    public static ItemRuntimeMutationResult failure(Status status, String detail) {
        return new ItemRuntimeMutationResult(status, null, null, detail);
    }
}
