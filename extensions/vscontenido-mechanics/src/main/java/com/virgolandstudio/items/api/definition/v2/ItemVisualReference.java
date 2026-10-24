package com.virgolandstudio.items.api.definition.v2;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable Media reference. Provider/backend selection is intentionally not encoded here. */
public record ItemVisualReference(String value) {
    private static final Pattern MEDIA_REFERENCE = Pattern.compile("media:[a-z0-9._/-]+");

    public ItemVisualReference {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!MEDIA_REFERENCE.matcher(value).matches()) {
            throw new IllegalArgumentException("Expected Media reference like media:breda_model: " + value);
        }
    }

    public static ItemVisualReference parse(String value) {
        return new ItemVisualReference(value);
    }
}
