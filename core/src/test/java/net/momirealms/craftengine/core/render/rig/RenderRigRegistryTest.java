package net.momirealms.craftengine.core.render.rig;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RenderRigRegistryTest {
    @Test
    void resolvesExplicitAndLegacyProvidersDeterministically() {
        RenderRigRegistry registry = new RenderRigRegistry();
        registry.register(provider("first", "shared"));
        registry.register(provider("second", "shared"));

        assertEquals("first", registry.resolve(RenderRigReference.legacy("shared", RenderRigType.MODEL)).orElseThrow().providerId());
        assertEquals("second", registry.resolve(RenderRigReference.of("SECOND", "shared", RenderRigType.MODEL)).orElseThrow().providerId());
        assertTrue(registry.resolve(RenderRigReference.of("missing", "shared", RenderRigType.MODEL)).isEmpty());
    }

    @Test
    void validatesHierarchyAndAnimationMetadata() {
        RenderPart hand = new RenderPart("hand", "arm", List.of());
        RenderPart arm = new RenderPart("arm", "body", List.of("hand"));
        RenderPart body = new RenderPart("body", null, List.of("arm"));
        RenderRig rig = new RenderRig("native", "hero", RenderRigType.PLAYER, List.of(body, arm, hand), Set.of("idle", "wave"));

        assertEquals(List.of(body), rig.roots());
        assertEquals("arm", rig.part("hand").orElseThrow().parentId());
        assertTrue(rig.hasAnimation("wave"));
        assertFalse(rig.hasAnimation("missing"));

        assertThrows(IllegalArgumentException.class, () -> new RenderRig(
                "native", "bad", RenderRigType.MODEL,
                List.of(new RenderPart("a", "missing", List.of())), Set.of()
        ));
    }

    private static RenderRigProvider provider(String id, String rigId) {
        return new RenderRigProvider() {
            @Override public String providerId() { return id; }
            @Override public RenderRig resolveRig(String requested, RenderRigType type) {
                return rigId.equals(requested) ? new RenderRig(id, requested, type, List.of(), Set.of()) : null;
            }
        };
    }
}
