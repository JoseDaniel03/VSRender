package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DeferredRenderInstanceTest {

    @Test
    void replaysViewerIntentWhenDelegateArrives() {
        DeferredRenderInstance deferred = new DeferredRenderInstance();
        Player first = viewer("first");
        Player second = viewer("second");
        RecordingInstance delegate = new RecordingInstance();

        deferred.show(first);
        deferred.show(second);
        deferred.hide(first);

        assertEquals(1, deferred.desiredViewerCount());
        assertTrue(deferred.bind(delegate));
        assertEquals(1, delegate.shows.get());
        assertEquals(1, delegate.showsFor(second));
        assertEquals(0, delegate.showsFor(first));

        deferred.hide(second);
        assertEquals(1, delegate.hides.get());
    }

    @Test
    void closeBeforeBindClosesLateDelegateWithoutReplay() {
        DeferredRenderInstance deferred = new DeferredRenderInstance();
        Player viewer = viewer("viewer");
        RecordingInstance delegate = new RecordingInstance();
        deferred.show(viewer);

        deferred.close();

        assertFalse(deferred.bind(delegate));
        assertTrue(delegate.closed.get());
        assertEquals(0, delegate.shows.get());
        assertTrue(deferred.closed());
    }

    @Test
    void bindCloseRaceNeverCallsDelegateAfterClose() throws Exception {
        for (int iteration = 0; iteration < 1_000; iteration++) {
            DeferredRenderInstance deferred = new DeferredRenderInstance();
            Player viewer = viewer("viewer-" + iteration);
            RecordingInstance delegate = new RecordingInstance();
            deferred.show(viewer);

            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread binder = new Thread(() -> runGuarded(start, failure, () -> deferred.bind(delegate)));
            Thread closer = new Thread(() -> runGuarded(start, failure, deferred::close));
            binder.start();
            closer.start();
            start.countDown();
            binder.join();
            closer.join();

            assertNull(failure.get(), "iteration " + iteration);
            assertTrue(deferred.closed());
            assertTrue(delegate.closed.get());
            assertFalse(delegate.callbackAfterClose.get(), "iteration " + iteration);
        }
    }

    private static void runGuarded(CountDownLatch start, AtomicReference<Throwable> failure, Runnable action) {
        try {
            start.await();
            action.run();
        } catch (Throwable throwable) {
            failure.compareAndSet(null, throwable);
        }
    }

    private static Player viewer(String name) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> name;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        throw new AssertionError(type);
    }

    private static final class RecordingInstance implements RenderInstance {
        private final AtomicInteger shows = new AtomicInteger();
        private final AtomicInteger hides = new AtomicInteger();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean callbackAfterClose = new AtomicBoolean();
        private final java.util.concurrent.ConcurrentHashMap<Player, AtomicInteger> showsByViewer = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public void show(Player viewer) {
            if (closed.get()) callbackAfterClose.set(true);
            shows.incrementAndGet();
            showsByViewer.computeIfAbsent(viewer, ignored -> new AtomicInteger()).incrementAndGet();
        }

        @Override
        public void hide(Player viewer) {
            if (closed.get()) callbackAfterClose.set(true);
            hides.incrementAndGet();
        }

        @Override
        public void close() {
            closed.set(true);
        }

        private int showsFor(Player viewer) {
            AtomicInteger count = showsByViewer.get(viewer);
            return count == null ? 0 : count.get();
        }
    }
}
