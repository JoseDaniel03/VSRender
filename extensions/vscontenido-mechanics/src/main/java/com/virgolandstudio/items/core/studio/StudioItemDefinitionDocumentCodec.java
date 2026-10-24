package com.virgolandstudio.items.core.studio;

import java.util.Map;

/** Dual-reader for migration tooling. It never converts v1 into v2 or fills missing v2 semantics. */
public final class StudioItemDefinitionDocumentCodec {
    private StudioItemDefinitionDocumentCodec() {}

    public static StudioItemDefinitionDocument decode(Map<String, Object> raw) {
        if (raw == null) throw new IllegalArgumentException("definition body is required");
        if (isCanonicalV2(raw)) {
            return StudioItemDefinitionDocument.canonical(StudioItemDefinitionV2Codec.decode(raw));
        }
        return StudioItemDefinitionDocument.legacy(StudioItemDefinitionCodec.decode(raw));
    }

    public static Map<String, Object> encode(StudioItemDefinitionDocument document) {
        if (document == null) throw new IllegalArgumentException("document is required");
        return switch (document.format()) {
            case LEGACY_V1 -> StudioItemDefinitionCodec.encode(document.legacy().orElseThrow());
            case CANONICAL_V2 -> StudioItemDefinitionV2Codec.encode(document.canonical().orElseThrow());
        };
    }

    public static StudioItemDefinitionFormat detectFormat(Map<String, Object> raw) {
        if (raw == null) throw new IllegalArgumentException("definition body is required");
        return isCanonicalV2(raw)
                ? StudioItemDefinitionFormat.CANONICAL_V2
                : StudioItemDefinitionFormat.LEGACY_V1;
    }

    private static boolean isCanonicalV2(Map<String, Object> raw) {
        Object schema = raw.get("schemaVersion");
        int version = schema instanceof Number number
                ? number.intValue()
                : schema == null ? (raw.containsKey("identity") ? 2 : 1) : Integer.parseInt(String.valueOf(schema));
        return version >= 2 && raw.containsKey("identity");
    }
}
