package net.momirealms.craftengine.core.render;

import net.momirealms.craftengine.core.render.animation.RenderAnimationSession;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AnimationSessionBookTest {
    static class Session implements RenderAnimationSession {
        int closes; Runnable finish; boolean accepts = true; boolean fail;
        public boolean start(Runnable done) { finish = done; if (fail) throw new IllegalStateException("failed"); return accepts; }
        public void close() { closes++; }
    }
    @Test void replacementClosesOldAndLateCompletionCannotCloseNew() {
        var book = new AnimationSessionBook(); var id = UUID.randomUUID();
        var old = new Session(); var next = new Session();
        assertTrue(book.start(id, old)); assertTrue(book.start(id, next));
        assertEquals(1, old.closes); old.finish.run(); assertEquals(0, next.closes);
        next.finish.run(); assertEquals(1, next.closes); assertEquals(0, book.size());
    }
    @Test void rejectedAnimationIsClosedAndNotRecorded() {
        var book = new AnimationSessionBook(); var s = new Session(); s.accepts = false;
        assertFalse(book.start(UUID.randomUUID(), s)); assertEquals(1, s.closes); assertEquals(0, book.size());
    }
    @Test void exceptionDuringStartCleansUp() {
        var book = new AnimationSessionBook(); var s = new Session(); s.fail = true;
        assertThrows(IllegalStateException.class, () -> book.start(UUID.randomUUID(), s));
        assertEquals(1, s.closes); assertEquals(0, book.size());
    }
    @Test void stopAndCompletionAreIdempotent() {
        var book = new AnimationSessionBook(); var id = UUID.randomUUID(); var s = new Session();
        book.start(id, s); assertTrue(book.stop(id)); assertFalse(book.stop(id)); s.finish.run();
        assertEquals(1, s.closes);
    }
    @Test void synchronousCompletionLeavesNoSession() {
        var book = new AnimationSessionBook(); var s = new Session() {
            public boolean start(Runnable done) { done.run(); return true; }
        };
        book.start(UUID.randomUUID(), s); assertEquals(0, book.size()); assertEquals(1, s.closes);
    }
    @Test void shutdownClosesAllOwnedSessions() {
        var book = new AnimationSessionBook(); var a = new Session(); var b = new Session();
        book.start(UUID.randomUUID(), a); book.start(UUID.randomUUID(), b); book.close(); book.close();
        assertEquals(1, a.closes); assertEquals(1, b.closes); assertEquals(0, book.size());
    }
}
