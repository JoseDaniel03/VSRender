package net.momirealms.craftengine.core.render.profile;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RenderProfileTest {

    @Test
    void normalizesUnknownIdentityAndWhitespace() {
        RenderProfile profile = new RenderProfile(RenderProfile.UNKNOWN_ID, "Jose", "texture");
        assertNull(profile.id());
        assertEquals("Jose", profile.name());
        assertEquals("texture", profile.rawTextures());
        assertTrue(profile.hasEmbeddedTextures());
        assertTrue(profile.hasLookupIdentity());
    }

    @Test
    void rejectsEmptyProfile() {
        assertThrows(IllegalArgumentException.class, () -> new RenderProfile(null, "", null));
    }

    @Test
    void acceptsUuidOnlyProfile() {
        UUID id = UUID.randomUUID();
        RenderProfile profile = new RenderProfile(id, null, null);
        assertEquals(id, profile.id());
        assertTrue(profile.hasLookupIdentity());
        assertFalse(profile.hasEmbeddedTextures());
    }
}
