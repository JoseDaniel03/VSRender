package com.virgolandstudio.items.core.studio;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.api.definition.v2.ItemDefinitionV2;

import java.util.Objects;
import java.util.Optional;

/** One decoded Studio definition in its original root format. It does not imply migration readiness. */
public record StudioItemDefinitionDocument(
        StudioItemDefinitionFormat format,
        Optional<ItemDefinition> legacy,
        Optional<ItemDefinitionV2> canonical
) {
    public StudioItemDefinitionDocument {
        Objects.requireNonNull(format, "format");
        legacy = legacy == null ? Optional.empty() : legacy;
        canonical = canonical == null ? Optional.empty() : canonical;
        if (format == StudioItemDefinitionFormat.LEGACY_V1 && (legacy.isEmpty() || canonical.isPresent())) {
            throw new IllegalArgumentException("LEGACY_V1 document requires only a legacy definition");
        }
        if (format == StudioItemDefinitionFormat.CANONICAL_V2 && (canonical.isEmpty() || legacy.isPresent())) {
            throw new IllegalArgumentException("CANONICAL_V2 document requires only a canonical definition");
        }
    }

    public static StudioItemDefinitionDocument legacy(ItemDefinition definition) {
        return new StudioItemDefinitionDocument(
                StudioItemDefinitionFormat.LEGACY_V1,
                Optional.of(Objects.requireNonNull(definition, "definition")),
                Optional.empty()
        );
    }

    public static StudioItemDefinitionDocument canonical(ItemDefinitionV2 definition) {
        return new StudioItemDefinitionDocument(
                StudioItemDefinitionFormat.CANONICAL_V2,
                Optional.empty(),
                Optional.of(Objects.requireNonNull(definition, "definition"))
        );
    }

    public NamespacedId id() {
        return legacy.map(ItemDefinition::id)
                .orElseGet(() -> canonical.orElseThrow().identity().id());
    }

    public int schemaVersion() {
        return legacy.map(ItemDefinition::schemaVersion)
                .orElseGet(() -> canonical.orElseThrow().schemaVersion());
    }
}
