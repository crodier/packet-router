package producerconsumer.router;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static producerconsumer.router.JCToolsPacketRouter.QueueType.MPMC;
import static producerconsumer.router.JCToolsPacketRouter.QueueType.MPSC;
import static producerconsumer.router.JCToolsPacketRouter.QueueType.SPSC;

/**
 * The primary performance test harness by which the analysis is performed.
 *
 * Test different packet router implementations.
 *
 * JCTools aims to be the missing fast threaded queues library in Java.
 */
public class PerformanceTest {

    // Unit tests in PacketRouterTestCases
    public static void main(String[] args) throws InterruptedException {

        // TODO:  Shell script with G1 collector to reproduce
        // TODO:  Document to run and produce graphs
        // -XX:+UseG1GC
        // TODO:  Document the JVM used and With and Without G1GC and the CMS Also

        final int size = 1000*1000*3; // move to command line, or pick many sizes in a method

        runManyExperiments(size, false);
        runManyExperiments(size, true);
    }

    private static void runManyExperiments(int size, boolean isRandom) throws InterruptedException {

        long start = System.currentTimeMillis();
        if (isRandom)
            System.out.println("\n~~~~~~~~~~~~~  RANDOM:  Starting experiments (ETA ten min.) ~~~~~~~~~~~\n");
        else
            System.out.println("\n************   STATIC:  Starting experiments (ETA ten min.) ***********\n");

        ArrayList<Packet> cachedList;
        if (isRandom) {
            // Make a random distribution.  Should not matter, but easy to check, could find bugs or interesting results.
            cachedList = TestPacketFactory.makeRandomDistribution(size);
        }
        else
        {
            cachedList = TestPacketFactory.makeEqualDistribution(size);
        }

        int numRuns = 5; // should be 10-20 in final testing

        // single writer experiments, different readers
        runExperiment(1, 1, numRuns, cachedList);
        runExperiment(2, 1, numRuns, cachedList);
        runExperiment(3, 1, numRuns, cachedList);
        runExperiment(4, 1, numRuns, cachedList);
        runExperiment(5, 1, numRuns, cachedList);
        runExperiment(6, 1, numRuns, cachedList);

        // equal number of readers and writers
        runExperiment(2, 2, numRuns, cachedList);
        runExperiment(3, 3, numRuns, cachedList);
        runExperiment(4, 4, numRuns, cachedList);

        // single reader
        runExperiment(1, 2, numRuns, cachedList);
        runExperiment(1, 3, numRuns, cachedList);
        runExperiment(1, 4, numRuns, cachedList);
        runExperiment(1, 5, numRuns, cachedList);
        runExperiment(1, 6, numRuns, cachedList);

        long time = System.currentTimeMillis() - start;
        System.out.println("\nTotal experiments took (minutes): "+(time/1000/60));
    }


    private static void runExperiment(int threadRead, int threadWrite, int numRuns, ArrayList<Packet> cachedList) throws InterruptedException {

        System.out.println("\n============== STARTING TEST WITH THREAD COUNTS, write: "+threadWrite+", read: "+threadRead);

        long size = cachedList.size();
        // this is for printing the ordering of the results
        PriorityQueue<AvgTimeResult> results = new PriorityQueue<>();

        // incorrect is not allowed to compete, it doesn't handle prioritization, but is interesting as a benchmark
        // testManyTimes("INCORRECT baseline", numRuns, threadRead, threadWrite, cachedList, new IncorrectPacketRouter());

        if (threadRead == 1 && threadWrite == 1) {
            results.add(testManyTimes("Agrona SPSC w Busy", numRuns, threadRead, threadWrite, cachedList, new AgronaPacketRouter(false, AgronaPacketRouter.QueueType.SPSC)));
            results.add(testManyTimes("JCTools SPSC w Busy   ", numRuns, threadRead, threadWrite, cachedList, new JCToolsPacketRouter(false, SPSC)));
            results.add(testManyTimes("JCTools SPSC w Locking", numRuns, threadRead, threadWrite, cachedList, new JCToolsPacketRouter(true, SPSC)));
        }
        else if (threadRead == 1 && threadWrite > 1){
            results.add(testManyTimes("Agrona MPSC w Busy", numRuns, threadRead, threadWrite, cachedList, new AgronaPacketRouter(false, AgronaPacketRouter.QueueType.MPSC)));
            results.add(testManyTimes("JCTools MPSC w Busy   ", numRuns, threadRead, threadWrite, cachedList, new JCToolsPacketRouter(false, MPSC)));
            results.add(testManyTimes("JCTOOLS MPSC w Locking", numRuns, threadRead, threadWrite, cachedList, new JCToolsPacketRouter(true, MPSC)));
        }
        else
        // if (threadWrite == 1 && threadRead > 1) {
        {
            results.add(testManyTimes("Agrona MPSC w Busy", numRuns, threadRead, threadWrite, cachedList, new AgronaPacketRouter(false, AgronaPacketRouter.QueueType.MPMC)));
            results.add(testManyTimes("JCTools MPMC w Busy   ", numRuns, threadRead, threadWrite, cachedList, new JCToolsPacketRouter(false, MPMC)));
            results.add(testManyTimes("JCTools MPMC w Locking", numRuns, threadRead, threadWrite, cachedList, new JCToolsPacketRouter(true, MPMC)));
        }

        results.add(testManyTimes("Concurrent Busy", numRuns, threadRead, threadWrite, cachedList, new ConcurrentPacketRouter(false)));
        results.add(testManyTimes("Concurrent Locking", numRuns, threadRead, threadWrite, cachedList, new ConcurrentPacketRouter(true)));
        results.add(testManyTimes("Linked Busy", numRuns, threadRead, threadWrite, cachedList, new LinkedPacketRouter(false)));
        results.add(testManyTimes("Linked Locking", numRuns, threadRead, threadWrite, cachedList, new LinkedPacketRouter(true)));
        results.add(testManyTimes("Priority Queue Java", numRuns, threadRead, threadWrite, cachedList, new PriorityBlockingQueuePacketRouter()));

        AvgTimeResult result = results.poll();
        double ops = size / (result.avgTime / (1000* 1000 * 1000d));
        String opsPerSec = new DecimalFormat("#,##0.00").format(ops);
        System.out.println("\nWINNER: "+result + ", Ops/Sec = "+(opsPerSec));
    }


    private static class AvgTimeResult implements Comparable<AvgTimeResult> {
        private final long best;
        private final long worst;
        String name;
        long avgTime;

        public AvgTimeResult(String testName, long avg, long worst, long best) {
            this.name = testName;
            this.avgTime = avg;
            this.worst = worst;
            this.best = best;
        }

        @Override
        public String toString() {
            return name+", avg time (ms):  "+nanosToMs(avgTime);
        }

        @Override
        public int compareTo(AvgTimeResult o) {
            if (this.avgTime < o.avgTime) return -1;
            if (this.avgTime == o.avgTime) return 0;
            return 1;
        }
    }

    private static AvgTimeResult testManyTimes(String testName, int numRuns, int threadRead, int threadWrite,
                                      ArrayList<Packet> cachedList, PacketRouter pr) throws InterruptedException {

        while (testName.length() < 33)
            testName += " ";

        System.out.print(testName+", read, "+threadRead+", write, "+threadWrite+", ");

        long totalNanos = 0;
        long worst = 0;
        long best = Long.MAX_VALUE;

        System.out.print(", Runs: ");
        for (int i= 0; i<numRuns; i++) {
            long time = testPacketRouter(cachedList, pr, threadRead, threadWrite);
            if (time < best) best = time;
            if (time > worst) worst = time;
            totalNanos += time;
        }

        long avg = totalNanos/numRuns;
        System.out.println("\t,worst: \t"+nanosToMs(worst)+", \tbest: \t"+nanosToMs(best)+"\t, Avg. ms: "+ nanosToMs(avg));

        return new AvgTimeResult(testName, avg, worst, best);
    }

    private static String nanosToMs(long avgNanos) {
        double ms = avgNanos/(1000*1000.0d);
        return new DecimalFormat("###0.0").format(ms);
    }

    static class Writer implements Callable<Long> {

        final List<Packet> myAdds = new ArrayList<>();
        private final PacketRouter packetRouter;
        private final CountDownLatch latch;

        public Writer(PacketRouter packetRouter, CountDownLatch latch) {
            this.packetRouter = packetRouter;
            this.latch = latch;
        }

        @Override
        public Long call() throws Exception {
            long start = System.nanoTime();

            // move it to the Packet Router from the list
            myAdds.forEach(packetRouter::add);

            // let the system know we finished adding
            latch.countDown();

            long addsTook = (System.nanoTime() -start);
            // System.out.println("Adds took millis: "+ nanosToMs(addsTook));
            return addsTook;
        }
    }


    private static final class ReadResult {
        long takesTook;
        long avgLatency; // TODO:  Could do latency stats, when adding stats
    }

    static class Reader implements Callable<ReadResult> {

        private final long targetSize;
        private final AtomicInteger tookCount;
        private final PacketRouter pr;
        private final CountDownLatch latch;

        public Reader(PacketRouter pr, long targetSize, AtomicInteger tookCount, CountDownLatch latch) {
            this.targetSize = targetSize;
            this.tookCount = tookCount;
            this.pr = pr;
            this.latch = latch;
        }

        @Override
        public ReadResult call() throws Exception {
            long start = System.nanoTime();
            int localTook = 0;

            while (localTook < targetSize) {
                Packet packet = pr.take(10); // It helps throughput to wait, but hurts latency (less context switching)
                if (packet != null)
                    localTook = tookCount.incrementAndGet();
                else
                    localTook = tookCount.get();
            }

            latch.countDown();
            long takesTook = (System.nanoTime()-start);

            ReadResult result = new ReadResult();
            result.takesTook = takesTook;
            // result.avgLatency = 0;

            return result;
        }
    }

    private static long testPacketRouter(ArrayList<Packet> cachedPacketList, final PacketRouter pr, int threadRead, int threadWrite)
            throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(threadRead + threadWrite);

        // only use the total processors on the machine
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        Writer[] writers = new Writer[threadWrite];
        for (int i = 0; i<threadWrite; i++) {
            writers[i] = new Writer(pr, latch);
        }

        long size = cachedPacketList.size();
        for (int i=0; i<size; i++) {
            // ArrayList interface needed here on the get.
            writers[i % writers.length].myAdds.add(cachedPacketList.get(i));
        }

        AtomicInteger tookCount = new AtomicInteger(0);

        for (Writer writer : writers) {
            es.submit(writer);
        }

        for (int i=0; i<threadRead; i++) {
            es.submit(new Reader(pr, size, tookCount, latch));
        }

        // TODO:  to make histograms and a graph of avg. latency

        long fullStart = System.nanoTime();

        latch.await();

        final int length = cachedPacketList.size() ;

        if (pr.isEmpty() == false)
            throw new RuntimeException("Bug, did not drain the packet router.");

        long fullEnd = System.nanoTime();
        long tookNanos = fullEnd - fullStart;
        System.out.print("\t"+ nanosToMs((tookNanos)));
        es.shutdown();

        return tookNanos;
    }

}
