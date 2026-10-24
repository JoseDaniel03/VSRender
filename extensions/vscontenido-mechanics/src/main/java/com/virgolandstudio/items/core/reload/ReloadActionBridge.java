package com.virgolandstudio.items.core.reload;

import com.virgolandstudio.items.core.runtime.ExecutionContext;

/**
 * Platform capability used by compiled reload/feed actions and conditions.
 * The core decides what operation happens; Paper only performs inventory/PDC mutations.
 */
public interface ReloadActionBridge {
    void detachContainer(ExecutionContext context);
    void insertReservedContainer(ExecutionContext context);
    void feedChamber(ExecutionContext context);

    /** True when one loose unit can be consumed and the embedded container has room. */
    boolean canLoadUnit(ExecutionContext context);

    /** Consume one compatible loose unit and load it into the embedded container. */
    boolean loadOne(ExecutionContext context);

    /** True when a compatible replacement resource (for example a battery/cell) is available. */
    boolean canReplaceResource(ExecutionContext context);

    /** Replace/refill the current resource from a compatible inventory resource. */
    boolean replaceResource(ExecutionContext context);
}
