package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.model.RenderModelReference;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderInstanceRegistryTest {
    @Test
    void explicitProviderDoesNotFallThrough() {
        var registry = new RenderInstanceRegistry();
        var first = new FakeProvider("first", true);
        var second = new FakeProvider("second", true);
        registry.register(first);
        registry.register(second);

        var request = new RenderInstanceRequest(RenderModelReference.of("second", "crate"), new WorldPosition(null, 1, 2, 3), true);
        assertTrue(registry.create(request).isPresent());
        assertEquals(0, first.calls);
        assertEquals(1, second.calls);
    }

    @Test
    void unknownProviderDoesNotProbeRegisteredProviders() {
        var registry = new RenderInstanceRegistry();
        var first = new FakeProvider("first", true);
        registry.register(first);

        var request = new RenderInstanceRequest(RenderModelReference.of("missing", "crate"), new WorldPosition(null, 0, 0, 0), false);
        assertTrue(registry.create(request).isEmpty());
        assertEquals(0, first.calls);
    }

    private static final class FakeProvider implements RenderInstanceProvider {
        private final String id;
        private final boolean resolves;
        private int calls;
        private FakeProvider(String id, boolean resolves) { this.id = id; this.resolves = resolves; }
        @Override public String providerId() { return id; }
        @Override public RenderInstance createInstance(RenderInstanceRequest request) {
            calls++;
            if (!resolves) return null;
            return new RenderInstance() {
                @Override public void show(Player viewer) {}
                @Override public void hide(Player viewer) {}
                @Override public void close() {}
            };
        }
    }
}
