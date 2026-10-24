package com.virgolandstudio.items.api.consumable;

import java.util.Objects;

/**
 * Provider response for a consumable actor effect.
 *
 * <p>{@link Status#UNHANDLED} is intentionally different from rejection: it means the provider
 * does not own the requested effect and a router may continue with the next provider. Rejection
 * means the provider owns the effect but refused it, so routing must stop and the Item dose must
 * not be committed.</p>
 */
public record ConsumableEffectResult(Status status, String detail) {
    public enum Status {
        APPLIED,
        REJECTED,
        UNHANDLED
    }

    public ConsumableEffectResult {
        Objects.requireNonNull(status, "status");
        detail = detail == null ? "" : detail;
    }

    /** Source-compatible constructor for callers that used the former boolean result. */
    public ConsumableEffectResult(boolean accepted, String detail) {
        this(accepted ? Status.APPLIED : Status.REJECTED, detail);
    }

    public boolean accepted() {
        return status == Status.APPLIED;
    }

    public boolean handled() {
        return status != Status.UNHANDLED;
    }

    public static ConsumableEffectResult accepted(String detail) {
        return new ConsumableEffectResult(Status.APPLIED, detail);
    }

    public static ConsumableEffectResult rejected(String detail) {
        return new ConsumableEffectResult(Status.REJECTED, detail);
    }

    public static ConsumableEffectResult unhandled(String detail) {
        return new ConsumableEffectResult(Status.UNHANDLED, detail);
    }
}
