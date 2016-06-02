package producerconsumer.router;

import junit.framework.TestCase;
import org.jctools.queues.SpscArrayQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Test cases that must hold, priority is a simple test case, for every implementation
 */
public class TestPacketRouter {

    AtomicLong packetNo = new AtomicLong();

    public class Drink {
        public int olives;

        @Override
        public String toString() {
            return ""+olives;
        }
    }

    @Test
    public void testStringBuffer() {
        int chris = 1;
//        synchronized (chris) {
//
//        }
        // above doesn't compile, must synchronize on an object
        StringBuffer sb = new StringBuffer("abcdef");
        StringBuffer sb2 = sb.insert(3, "c");
        Assert.assertEquals(sb, sb2);

        Drink[] drinks = new Drink[2];
        // no effect, the array is passed by reference
        stirDrinks(drinks[0]);
        System.out.println("0:"+drinks[0]+", 1: "+drinks[1]);
    }

    private static abstract class Chris {
        final void go() {
            // works
        }
    }

    public void stirDrinks(Drink drink) {
        if (drink == null)
            drink = new Drink();
        drink.olives++;

    }

    public void testPacketOrderingBackwards(PacketRouter packetRouter) {

        for (int i=0; i<10; i++) {
            packetRouter.add(new Packet().setLarge(false).setPacketType(Packet.PacketType.User).setPacketNo(packetNo.getAndIncrement()));
        }

        for (int i=0; i<10; i++) {
            packetRouter.add(new Packet().setLarge(true).setPacketType(Packet.PacketType.User).setPacketNo(packetNo.getAndIncrement()));
        }

        for (int i=0; i<10; i++) {
            packetRouter.add(new Packet().setLarge(false).setPacketType(Packet.PacketType.Management).setPacketNo(packetNo.getAndIncrement()));
        }

        for (int i=0; i<10; i++) {
            packetRouter.add(new Packet().setLarge(true).setPacketType(Packet.PacketType.Management).setPacketNo(packetNo.getAndIncrement()));
        }

        verifyOrder(packetRouter);
    }

    public void testPacketOrderingMixed(PacketRouter packetRouter) {

        for (int i=0; i<10; i++) {
            packetRouter.add(new Packet().setLarge(false).setPacketType(Packet.PacketType.User));
            packetRouter.add(new Packet().setLarge(true).setPacketType(Packet.PacketType.User));
            packetRouter.add(new Packet().setLarge(false).setPacketType(Packet.PacketType.Management));
            packetRouter.add(new Packet().setLarge(true).setPacketType(Packet.PacketType.Management));
        }

        verifyOrder(packetRouter);
    }

    // then went on in any order, but always need to come off in this order.
    private void verifyOrder(PacketRouter packetRouter) {
        testPackets(packetRouter, Packet.PacketType.Management, true);
        testPackets(packetRouter, Packet.PacketType.Management, false);
        testPackets(packetRouter, Packet.PacketType.User, true);
        testPackets(packetRouter, Packet.PacketType.User, false);
    }

    private void testPackets(PacketRouter packetRouter, Packet.PacketType expectedType, boolean isLarge) {
        Long lastPacketNo = null;
        for (int i=0; i<10; i++) {
            Packet p = packetRouter.take();
            if (lastPacketNo == null)
                lastPacketNo = p.packetNo;
            else {
                Assert.assertTrue("packets not in order: "+p.packetNo+", last: "+lastPacketNo, p.packetNo > lastPacketNo);
                lastPacketNo = p.packetNo;
            }
            Assert.assertEquals(expectedType, p.getPacketType());
            Assert.assertEquals(isLarge, p.isLarge());
        }
    }

    public void testPacketOrdering(PacketRouter router) {
        testPacketOrderingBackwards(router);
        testPacketOrderingMixed(router);
    }

    @Test
    public void priorityBlockingQueueTests() {
        testPacketOrdering(new PriorityBlockingQueuePacketRouter());
    }

    @Test
    public void concurrentQueueBusyWaitTests() {
      testPacketOrdering(new ConcurrentPacketRouter(false));
    }

    @Test
    public void concurrentQueueBlockingWaitTests() {
        testPacketOrdering(new ConcurrentPacketRouter(true));
    }

    @Test
    public void linkedBlockingQueueBusyWaitTests() {
        testPacketOrdering(new LinkedPacketRouter(true));
    }


    @Test
    public void threadedTestRandomDist() {
        threadedTest(TestPacketFactory.makeRandomDistribution(1000*1000));
    }

    @Test
    public void threadedTestStaticDist() {
        threadedTest(TestPacketFactory.makeEqualDistribution(1000*1000));
    }

    public void threadedTest(ArrayList<Packet> cachedPacketList) {

        ExecutorService es = Executors.newCachedThreadPool();

        TestWriter[] writers = new TestWriter[4];

        CountDownLatch latch = new CountDownLatch(4);

        PacketRouter pr = new JCToolsPacketRouter(false, JCToolsPacketRouter.QueueType.MPMC);

        for (int i = 0; i<4; i++) {
            writers[i] = new TestWriter(pr, latch);
        }

        long size = cachedPacketList.size();
        for (int i=0; i<size; i++) {
            // ArrayList interface needed here on the get.
            writers[i % writers.length].myAdds.add(cachedPacketList.get(i));
        }

        AtomicInteger tookCount = new AtomicInteger(0);

        for (TestWriter writer : writers) {
            es.submit(writer);
        }

        for (int i=0; i<4; i++) {
            es.submit(new TestReader(pr, size, tookCount, latch));
        }
    }

    static class TestWriter implements Callable<Long> {

        final List<Packet> myAdds = new ArrayList<>();
        private final PacketRouter packetRouter;
        private final CountDownLatch latch;

        public TestWriter(PacketRouter packetRouter, CountDownLatch latch) {
            this.packetRouter = packetRouter;
            this.latch = latch;
        }

        @Override
        public Long call() throws Exception {
            long start = System.nanoTime();

            // move it to the Packet Router from the list
            long packetNo = 0;
            for (Packet myAdd : myAdds) {
                myAdd.packetNo = packetNo++;
            }

            myAdds.forEach(packetRouter::add);

            // let the system know we finished adding
            latch.countDown();

            long addsTook = (System.nanoTime() -start);
            // System.out.println("Adds took millis: "+ nanosToMs(addsTook));
            return addsTook;
        }
    }


    static class TestReader implements Callable  {

        // verify the packets come back in order, per priority
        Map<PacketTypeKey, Long> map = new HashMap<>();

        private final long targetSize;
        private final AtomicInteger tookCount;
        private final PacketRouter pr;
        private final CountDownLatch latch;

        public TestReader(PacketRouter pr, long targetSize, AtomicInteger tookCount, CountDownLatch latch) {
            this.targetSize = targetSize;
            this.tookCount = tookCount;
            this.pr = pr;
            this.latch = latch;
        }

        @Override
        public Integer call() throws Exception {

            int localTook = 0;

            while (localTook < targetSize) {
                Packet packet = pr.take(10); // It helps throughput to wait, but hurts latency (less context switching)
                if (packet != null) {
                    PacketTypeKey newPtk = new PacketTypeKey(packet.isLarge, packet.getPacketType());
                    Long last = map.get(newPtk);
                    if (last == null)
                        map.put(newPtk, packet.packetNo);
                    else {
                        Assert.assertTrue("Packets out of order", packet.packetNo > last);
                        map.replace(newPtk, packet.packetNo);
                    }

                    localTook = tookCount.incrementAndGet();
                }
                else {
                    localTook = tookCount.get();
                }
            }

            latch.countDown();
            return 0;
        }
    }

}
