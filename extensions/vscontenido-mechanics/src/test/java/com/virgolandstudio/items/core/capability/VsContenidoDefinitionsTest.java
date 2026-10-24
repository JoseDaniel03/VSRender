package com.virgolandstudio.items.core.capability;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class VsContenidoDefinitionsTest {
    @Test void sameCapabilitiesCanBeUsedByItemBlockFurnitureAndEntity() {
        var definitions = new VsContenidoDefinitions();
        var capabilities = Map.of("fov", Map.of("aumento", 1.6), "controles", Map.of("activar", "mouse_right"));
        assertEquals(1, definitions.ingest("items", "demo",
                Map.of("demo:escopeta", Map.of("vscontenido", capabilities))));
        assertEquals(1, definitions.ingest("blocks", "demo",
                Map.of("demo:terminal", Map.of("vscontenido", capabilities))));
        assertEquals(1, definitions.ingest("furniture", "demo",
                Map.of("demo:telescopio", Map.of("vscontenido", capabilities))));
        assertEquals(1, definitions.ingest("entities", "demo",
                Map.of("demo:drone", Map.of("vscontenido", capabilities))));
        assertEquals(4, definitions.size());
        assertTrue(definitions.find(VsContenidoDefinitions.Kind.BLOCK, "demo:terminal")
                .orElseThrow().capabilities().has("controles"));
    }

    @Test void independentCapabilitiesAndFutureExtensionsAreAccepted() {
        var cfg = VsContenidoCapabilities.parse(Map.of(
                "fov", Map.of("aumento", 2.0),
                "cooldown", Map.of("ticks", 20),
                "vuelo", Map.of("activar", true, "limites", List.of(1, 2, 3))));
        assertTrue(cfg.has("vuelo"));
        assertFalse(cfg.isBuiltIn("vuelo"));
        assertTrue(cfg.isBuiltIn("fov"));
        assertThrows(UnsupportedOperationException.class, () -> cfg.settings("vuelo").put("activar", false));
    }

    @Test void badConfigurationsAreRejectedWithContext() {
        assertThrows(IllegalArgumentException.class,
                () -> VsContenidoCapabilities.parse(Map.of("fov", Map.of("aumento", 0.5))));
        assertThrows(IllegalArgumentException.class,
                () -> VsContenidoCapabilities.parse(Map.of("controles", Map.of("disparar", 12))));
        assertThrows(IllegalArgumentException.class,
                () -> VsContenidoCapabilities.parse(Map.of("recarga", "R")));
        assertThrows(IllegalArgumentException.class,
                () -> new VsContenidoDefinitions().ingest("items", "demo",
                        Map.of("demo:a", Map.of("vscontenido", Map.of("fov", Map.of("aumento", 99.0))))));
    }

    @Test void absentVsContenidoIsInertAndNoOtherConfigKeysAreTouched() {
        var r=new VsContenidoDefinitions();
        r.ingest("items", "demo", Map.of("demo:iron", Map.of("material", "iron_ingot")));
        assertEquals(0, r.size());
        assertNull(VsContenidoDefinitions.kindOf("recipes"));
        assertEquals(VsContenidoDefinitions.Kind.ENTITY, VsContenidoDefinitions.kindOf("entity"));
    }
}
