package com.virgolandstudio.items.core.inventory;

/** Inventory space consumed by one logical item instance. */
public record CompiledInventoryFootprint(int slots, boolean mustBeConsecutive, boolean allowHotbar) {
    public CompiledInventoryFootprint {
        if (slots < 1 || slots > 9) throw new IllegalArgumentException("InventoryFootprint.slots must be within [1,9]");
    }

    public boolean reservesExtraSlots() { return slots > 1; }
}
