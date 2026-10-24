package com.virgolandstudio.items.api;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record NamespacedId(String namespace, String value) {
    private static final Pattern PART = Pattern.compile("[a-z0-9._/-]+");

    public NamespacedId {
        namespace = normalize(namespace, "namespace");
        value = normalize(value, "value");
    }

    public static NamespacedId parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        int separator = raw.indexOf(':');
        if (separator <= 0 || separator == raw.length() - 1) {
            throw new IllegalArgumentException("Expected namespaced id like virgoland:item_name: " + raw);
        }
        return new NamespacedId(raw.substring(0, separator), raw.substring(separator + 1));
    }

    private static String normalize(String part, String label) {
        Objects.requireNonNull(part, label);
        String normalized = part.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !PART.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid " + label + ": " + part);
        }
        return normalized;
    }

    @Override
    public String toString() {
        return namespace + ':' + value;
    }
}
