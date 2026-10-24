package com.virgolandstudio.items.core.accessory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Mutable-in-session attachment map; persistence is handled by AttachmentLoadoutCodec. */
public final class AttachmentLoadout {
    public static final String STATE_KEY = "attachment_loadout";

    private final LinkedHashMap<String, InstalledAccessory> installed = new LinkedHashMap<>();

    public AttachmentLoadout() {}

    public AttachmentLoadout(Map<String, InstalledAccessory> initial) {
        if (initial != null) initial.forEach((slot, accessory) -> installed.put(normalize(slot), accessory));
    }

    public Optional<InstalledAccessory> find(String slot) {
        return Optional.ofNullable(installed.get(normalize(slot)));
    }

    public boolean occupied(String slot) {
        return installed.containsKey(normalize(slot));
    }

    public void install(String slot, InstalledAccessory accessory) {
        String normalized = normalize(slot);
        if (installed.containsKey(normalized)) throw new IllegalStateException("Attachment slot already occupied: " + normalized);
        installed.put(normalized, accessory);
    }

    public Optional<InstalledAccessory> remove(String slot) {
        return Optional.ofNullable(installed.remove(normalize(slot)));
    }

    public Map<String, InstalledAccessory> snapshot() {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(installed));
    }

    public int size() { return installed.size(); }
    public boolean isEmpty() { return installed.isEmpty(); }

    private static String normalize(String slot) {
        if (slot == null || slot.isBlank()) throw new IllegalArgumentException("slot cannot be blank");
        return slot.trim().toUpperCase(Locale.ROOT);
    }
}
