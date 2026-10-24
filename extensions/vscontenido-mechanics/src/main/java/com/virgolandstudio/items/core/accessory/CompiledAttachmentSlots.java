package com.virgolandstudio.items.core.accessory;

import java.util.LinkedHashSet;
import java.util.List;

/** Ordered customization sockets compiled from the attachment_slots component. */
public record CompiledAttachmentSlots(List<String> slots) {
    public CompiledAttachmentSlots {
        var normalized = new LinkedHashSet<String>();
        if (slots != null) {
            for (String slot : slots) {
                if (slot == null || slot.isBlank()) continue;
                normalized.add(slot.trim().toUpperCase(java.util.Locale.ROOT));
            }
        }
        if (normalized.size() > 12) {
            throw new IllegalArgumentException("AttachmentSlots supports at most 12 slots per item");
        }
        slots = List.copyOf(normalized);
    }

    public boolean isEmpty() { return slots.isEmpty(); }
    public boolean has(String slot) {
        return slot != null && slots.contains(slot.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
