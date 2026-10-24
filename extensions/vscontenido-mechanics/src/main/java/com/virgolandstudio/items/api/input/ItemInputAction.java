package com.virgolandstudio.items.api.input;

import java.util.Locale;
import java.util.Optional;

/**
 * Semantic input that belongs to VirgolandStudio Items.
 * Physical keys and transport protocols belong to platform/client adapters.
 */
public enum ItemInputAction {
    FIRE,
    SECONDARY_FIRE,
    RELOAD,
    EXTRACT_AMMO,
    FIRE_MODE,
    INSPECT;

    public static Optional<ItemInputAction> parse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "fire", "shoot", "disparar", "disparo" -> Optional.of(FIRE);
            case "secondary_fire", "alt_fire", "alternate_fire", "disparo_secundario", "fuego_secundario" -> Optional.of(SECONDARY_FIRE);
            case "reload", "recargar", "recarga" -> Optional.of(RELOAD);
            case "extract", "extract_ammo", "eject", "extraer", "extraer_municion", "extraer_munición" -> Optional.of(EXTRACT_AMMO);
            case "fire_mode", "firemode", "mode", "modo", "modo_fuego" -> Optional.of(FIRE_MODE);
            case "inspect", "inspection", "inspeccionar", "inspeccion", "inspección" -> Optional.of(INSPECT);
            default -> Optional.empty();
        };
    }
}
