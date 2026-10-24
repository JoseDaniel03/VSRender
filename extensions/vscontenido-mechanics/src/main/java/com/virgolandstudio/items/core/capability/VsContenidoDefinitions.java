package com.virgolandstudio.items.core.capability;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * General registry for VSContenido settings attached to existing VSRender object definitions.
 * No separate item, block, entity, renderer or scheduler is created by this registry.
 */
public final class VsContenidoDefinitions {
    private static final Pattern OBJECT_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private final Map<Target, Definition> definitions = new LinkedHashMap<>();

    public enum Kind { ITEM, BLOCK, FURNITURE, ENTITY }

    public record Target(Kind kind, String id) { }
    public record Definition(Target target, String packNamespace, VsContenidoCapabilities capabilities) { }

    public synchronized void clear() { definitions.clear(); }

    public synchronized int size() { return definitions.size(); }

    public synchronized Optional<Definition> find(Kind kind, String id) {
        return Optional.ofNullable(definitions.get(new Target(kind, id)));
    }

    public synchronized Map<Target, Definition> snapshot() { return Map.copyOf(definitions); }

    /** Index YAML "items" / "blocks" / "furniture" / "entities" without consuming renderer data. */
    public synchronized int ingest(String sectionName, String namespace, Map<?, ?> objects) {
        Kind kind = kindOf(sectionName);
        if (kind == null) return 0;
        if (namespace == null || !namespace.matches("[a-z0-9_.-]+"))
            throw new IllegalArgumentException("Namespace invalido de pack VSContenido");
        // Validate the whole file before publishing any capability, to avoid partial registration.
        var staged = new LinkedHashMap<Target, Definition>();
        for (var object : objects.entrySet()) {
            if (!(object.getValue() instanceof Map<?, ?> config) || !config.containsKey("vscontenido")) continue;
            if (!(object.getKey() instanceof String itemKey)) throw new IllegalArgumentException("ID de objeto VSContenido no es texto");
            String id = itemKey.contains(":") ? itemKey : namespace + ":" + itemKey;
            if (!OBJECT_ID.matcher(id).matches() || id.contains("..")) throw new IllegalArgumentException("ID de objeto VSContenido invalido: " + id);
            VsContenidoCapabilities capabilities = VsContenidoCapabilities.parse(config.get("vscontenido"));
            Target target = new Target(kind, id);
            if (definitions.containsKey(target) || staged.putIfAbsent(target,
                    new Definition(target, namespace, capabilities)) != null)
                throw new IllegalArgumentException("vscontenido repetido en " + kind + " " + id);
        }
        definitions.putAll(staged);
        return staged.size();
    }

    public static Kind kindOf(String raw) {
        if (raw == null) return null;
        return switch(raw.trim().toLowerCase(Locale.ROOT)) {
            case "item", "items" -> Kind.ITEM;
            case "block", "blocks" -> Kind.BLOCK;
            case "furniture" -> Kind.FURNITURE;
            case "entity", "entities" -> Kind.ENTITY;
            default -> null;
        };
    }
}
