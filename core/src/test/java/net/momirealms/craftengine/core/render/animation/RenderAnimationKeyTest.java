package net.momirealms.craftengine.core.render.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderAnimationKeyTest {
    @Test
    void preservesProviderIndependentRigAndAnimationIdentity() {
        var key = new RenderAnimationKey("player_arms", "reload");
        assertEquals("player_arms", key.rigId());
        assertEquals("reload", key.animationId());
    }

    @Test
    void rejectsBlankResourceNames() {
        assertThrows(IllegalArgumentException.class, () -> new RenderAnimationKey("", "reload"));
        assertThrows(IllegalArgumentException.class, () -> new RenderAnimationKey("player_arms", " "));
    }
}
