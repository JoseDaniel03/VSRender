package com.virgolandstudio.items.core.accessory;

import com.virgolandstudio.items.api.accessory.AccessoryDefinition;
import com.virgolandstudio.items.core.compile.CompiledItemDefinition;

import java.util.Locale;

/** Pure compatibility checks shared by any future inventory, Shadr or web renderer. */
public final class AccessoryCompatibility {
    private AccessoryCompatibility() {}

    public static boolean supports(AccessoryDefinition accessory, CompiledItemDefinition weapon, String requestedSlot) {
        if (accessory == null || weapon == null || requestedSlot == null) return false;
        boolean weaponTag = weapon.source().tags().stream()
                .anyMatch(tag -> tag != null && tag.trim().equalsIgnoreCase("weapon"));
        if (!weaponTag) return false;
        if (weapon.attachmentSlots() == null || !weapon.attachmentSlots().has(requestedSlot)) return false;
        if (!accessory.slot().equals(requestedSlot.trim().toUpperCase(Locale.ROOT))) return false;

        String family = weapon.source().family() == null ? "" : weapon.source().family().trim().toLowerCase(Locale.ROOT);
        if (!accessory.compatibleFamilies().isEmpty() && !accessory.compatibleFamilies().contains(family)) return false;

        for (String required : accessory.requiredWeaponTags()) {
            boolean found = weapon.source().tags().stream()
                    .anyMatch(tag -> tag != null && tag.trim().toLowerCase(Locale.ROOT).equals(required));
            if (!found) return false;
        }
        return true;
    }
}
