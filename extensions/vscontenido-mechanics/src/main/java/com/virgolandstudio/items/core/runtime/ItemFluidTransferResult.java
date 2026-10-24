package com.virgolandstudio.items.core.runtime;

/** Result of an atomic item-to-item fluid transfer. */
public record ItemFluidTransferResult(
        Status status,
        String fluid,
        double transferred,
        double sourceRemaining,
        double destinationAmount,
        Double destinationTemperatureC,
        String message
) {
    public enum Status {
        APPLIED,
        NOT_FOUND,
        NOT_SUPPORTED,
        INVALID_AMOUNT,
        EMPTY_SOURCE,
        INSUFFICIENT,
        CAPACITY_EXCEEDED,
        FLUID_NOT_ALLOWED,
        MIXING_NOT_ALLOWED
    }

    public boolean applied() { return status == Status.APPLIED; }

    public static ItemFluidTransferResult failure(Status status, String message) {
        return new ItemFluidTransferResult(status, "", 0.0, 0.0, 0.0, null, message == null ? "" : message);
    }
}
