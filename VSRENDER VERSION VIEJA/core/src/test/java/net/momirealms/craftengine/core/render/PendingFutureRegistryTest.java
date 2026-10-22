package net.momirealms.craftengine.core.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PendingFutureRegistryTest {
    @Test void completedFutureIsUnregistered() {
        var registry = new PendingFutureRegistry<>("closed");
        var future = registry.register();
        assertEquals(1, registry.size());
        assertTrue(future.complete("ok"));
        assertEquals("ok", future.join());
        assertEquals(0, registry.size());
    }

    @Test void closeCompletesAllOutstandingFutures() {
        var registry = new PendingFutureRegistry<>("closed");
        var first = registry.register();
        var second = registry.register();
        registry.close();
        assertEquals("closed", first.join());
        assertEquals("closed", second.join());
        assertEquals(0, registry.size());
        assertTrue(registry.isClosed());
    }

    @Test void closeIsIdempotentAndNewRegistrationsAreAlreadyClosed() {
        var registry = new PendingFutureRegistry<>("closed");
        registry.close();
        registry.close();
        var future = registry.register();
        assertTrue(future.isDone());
        assertEquals("closed", future.join());
        assertEquals(0, registry.size());
    }

    @Test void concurrentRegistrationAndCloseCannotLeakAnUnresolvedFuture() throws Exception {
        for (int attempt = 0; attempt < 250; attempt++) {
            var registry = new PendingFutureRegistry<>("closed");
            var start = new CountDownLatch(1);
            var futures = new ArrayList<java.util.concurrent.Future<?>>(2);
            var registered = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<String>>();
            try (var pool = Executors.newFixedThreadPool(2)) {
                futures.add(pool.submit(() -> {
                    await(start);
                    registered.set(registry.register());
                }));
                futures.add(pool.submit(() -> {
                    await(start);
                    registry.close();
                }));
                start.countDown();
                for (var task : futures) task.get(5, TimeUnit.SECONDS);
            }
            var future = registered.get();
            assertNotNull(future);
            assertTrue(future.isDone(), "attempt " + attempt + " leaked an unresolved future");
            assertEquals("closed", future.join());
            assertEquals(0, registry.size());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
    }
}
