package net.momirealms.craftengine.core.block.multiblock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MultiblockRemovalReservationsTest {
    @Test void sameAnchorCannotBeReusedBeforeAllPiecesFinish() {
        var registry=new MultiblockRemovalReservations();var ticket=registry.begin("world:anchor",3);
        assertNotNull(ticket);assertTrue(registry.isReserved("world:anchor"));assertNull(registry.begin("world:anchor",3));
        ticket.completePart(0);ticket.completePart(1);assertTrue(registry.isReserved("world:anchor"));
        ticket.completePart(2);assertFalse(registry.isReserved("world:anchor"));assertNotNull(registry.begin("world:anchor",3));
    }
    @Test void duplicateCallbackCannotReleaseAnotherPendingPiece() {
        var registry=new MultiblockRemovalReservations();var ticket=registry.begin("anchor",2);assertNotNull(ticket);
        ticket.completePart(0);ticket.completePart(0);assertTrue(registry.isReserved("anchor"));ticket.completePart(1);assertFalse(registry.isReserved("anchor"));
    }
    @Test void oldCallbackCannotReleaseTheNextReservation() {
        var registry=new MultiblockRemovalReservations();var old=registry.begin("anchor",1);assertNotNull(old);old.completePart(0);
        var next=registry.begin("anchor",1);assertNotNull(next);old.completePart(0);assertTrue(registry.isReserved("anchor"));next.completePart(0);assertFalse(registry.isReserved("anchor"));
    }
    @Test void neighboringStatuesUseIndependentReservations() {
        var registry=new MultiblockRemovalReservations();var a=registry.begin("a",1);var b=registry.begin("b",1);assertNotNull(a);assertNotNull(b);
        a.completePart(0);assertFalse(registry.isReserved("a"));assertTrue(registry.isReserved("b"));
    }
    @Test void concurrentRemovalRequestsReserveAnAnchorOnce() throws Exception {
        var registry=new MultiblockRemovalReservations();
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var start=new java.util.concurrent.CountDownLatch(1);var winners=new java.util.concurrent.atomic.AtomicInteger();
            var jobs=new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for(int i=0;i<16;i++)jobs.add(pool.submit(()->{try{start.await();if(registry.begin("anchor",1)!=null)winners.incrementAndGet();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}}));
            start.countDown();for(var job:jobs)job.get();assertEquals(1,winners.get());
        }
    }
}
