package net.momirealms.craftengine.core.render;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class AnimationRequestGuardTest {
    @Test void changingEquipmentRejectsAlreadyQueuedPlay() {
        var guard = new AnimationRequestGuard(); var id = UUID.randomUUID(); var queued = guard.capture(id);
        guard.invalidate(id); assertFalse(guard.valid(queued)); assertTrue(guard.valid(guard.capture(id)));
    }
    @Test void reconnectWithSameUuidCannotReviveOldRequest() {
        var guard = new AnimationRequestGuard(); var id = UUID.randomUUID(); var queued = guard.capture(id);
        guard.retire(id); var joined = guard.capture(id);
        assertFalse(guard.valid(queued)); assertTrue(guard.valid(joined));
    }
}
