package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.ammo.AmmoStorageMode;

/**
 * Platform-independent extraction policy for ammo/resources stored directly in a weapon.
 * Paper is responsible only for turning the returned contents into inventory items.
 */
public final class AmmoExtractionRuntime {
    private AmmoExtractionRuntime() {}

    public static EmbeddedExtraction extract(AmmoContainerState state, int chamberedUnits) {
        if (state == null) throw new IllegalArgumentException("state cannot be null");
        if (chamberedUnits < 0) throw new IllegalArgumentException("chamberedUnits cannot be negative");

        if (state.mode() == AmmoStorageMode.RESOURCE) {
            if (chamberedUnits != 0) {
                throw new IllegalArgumentException("RESOURCE storage cannot extract discrete chambered units");
            }
            double resource = state.available();
            state.setAmount(0.0);
            return new EmbeddedExtraction(0, resource);
        }

        int live = (int) Math.rint(state.available());
        if (state.mode() == AmmoStorageMode.SLOTS) {
            // SPENT slots are discarded; only LIVE units are returned to inventory.
            for (int i = 0; i < state.capacity(); i++) state.clearSlot(i);
        } else {
            state.setAmount(0.0);
        }
        return new EmbeddedExtraction(live + chamberedUnits, 0.0);
    }

    public record EmbeddedExtraction(int looseUnits, double resourceUnits) {
        public EmbeddedExtraction {
            if (looseUnits < 0) throw new IllegalArgumentException("looseUnits cannot be negative");
            if (!Double.isFinite(resourceUnits) || resourceUnits < 0.0) {
                throw new IllegalArgumentException("resourceUnits must be finite and non-negative");
            }
        }
    }
}
