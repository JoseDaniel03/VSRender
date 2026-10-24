package com.virgolandstudio.items.core.studio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Batch migration scanner. Invalid records are reported by index; valid v1/v2 records remain untouched. */
public final class StudioItemDefinitionDualReader {
    public StudioItemDefinitionReadReport read(List<?> rawDefinitions) {
        if (rawDefinitions == null || rawDefinitions.isEmpty()) {
            return new StudioItemDefinitionReadReport(List.of(), List.of());
        }
        var documents = new ArrayList<StudioItemDefinitionDocument>();
        var issues = new ArrayList<StudioItemDefinitionReadIssue>();
        for (int index = 0; index < rawDefinitions.size(); index++) {
            Object candidate = rawDefinitions.get(index);
            if (!(candidate instanceof Map<?, ?> source)) {
                issues.add(new StudioItemDefinitionReadIssue(index, "definition must be a map"));
                continue;
            }
            try {
                var raw = new LinkedHashMap<String, Object>();
                for (Map.Entry<?, ?> entry : source.entrySet()) {
                    if (entry.getKey() == null) {
                        throw new IllegalArgumentException("definition contains a null key");
                    }
                    raw.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                documents.add(StudioItemDefinitionDocumentCodec.decode(raw));
            } catch (RuntimeException exception) {
                String message = exception.getMessage();
                issues.add(new StudioItemDefinitionReadIssue(
                        index,
                        message == null || message.isBlank() ? exception.getClass().getSimpleName() : message
                ));
            }
        }
        return new StudioItemDefinitionReadReport(documents, issues);
    }
}
