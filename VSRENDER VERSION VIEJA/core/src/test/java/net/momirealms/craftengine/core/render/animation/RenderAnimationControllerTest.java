package net.momirealms.craftengine.core.render.animation;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RenderAnimationControllerTest {
    @Test
    void supportsOnlyMatchingRigAndKnownAnimation() {
        RenderAnimationController controller = new RenderAnimationController() {
            @Override public String rigId() { return "machine"; }
            @Override public Set<String> animations() { return Set.of("open", "close"); }
            @Override public Optional<RenderAnimationSession> open(RenderAnimationKey key, RenderAnimationMode mode) { return Optional.empty(); }
            @Override public void close() {}
        };

        assertTrue(controller.supports(new RenderAnimationKey("machine", "open")));
        assertFalse(controller.supports(new RenderAnimationKey("other", "open")));
        assertFalse(controller.supports(new RenderAnimationKey("machine", "missing")));
    }
}
