package com.virgolandstudio.items.core.runtime;

import java.util.Map;

/** Fully resolved, provider-neutral visual presentation for one item render context. */
public record ItemVisualPresentation(
        String type,
        String reference,
        String itemModelKey,
        String provider,
        String modelId,
        String animation,
        Map<String, Object> transform,
        Map<String, Object> payload
) {
    public ItemVisualPresentation {
        type = normalize(type, "VANILLA").toUpperCase(java.util.Locale.ROOT);
        reference = normalize(reference, "");
        itemModelKey = normalize(itemModelKey, "");
        provider = normalize(provider, "");
        modelId = normalize(modelId, "");
        animation = normalize(animation, "");
        transform = transform == null ? Map.of() : Map.copyOf(transform);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public boolean directItemModel() {
        return !itemModelKey.isBlank();
    }

    public boolean sprite2d() {
        return type.equals("SPRITE_2D") && !reference.isBlank();
    }

    public boolean model3d() {
        return type.equals("MODEL_3D") && !provider.isBlank() && !modelId.isBlank();
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
