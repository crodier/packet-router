package producerconsumer.router.wait;

import producerconsumer.router.GetLatestPacket;
import producerconsumer.router.Packet;

/**
 * spin (busy) wait the reader for approx. timeoutMs.
 * Assumes  not using more physical cores than on the machine, minimizes latency.
 */
public class BusyWaitStrategy implements WaitStrategy {

    // callback to the PacketRouter to get the next element
    private GetLatestPacket fetcher;

    // in the constructor, bootstrap for the machine you are on.
    // to avoid blocking wait
    private long nanosPerLoop = 0;

    public BusyWaitStrategy(GetLatestPacket fetcher) {

        this.fetcher = fetcher;

        // do a million empty gets, to estimate the number of empty waits per millisecond
        int count = 1000*1000;
        long now = System.nanoTime();

        for (int i=0; i<count; i++) {
            fetcher.getLatest();
        }

        long end = System.nanoTime();
        long took  = end-now;
        // System.out.println("Bootstrap a million (empty) took ns: "+took);

        nanosPerLoop = took / count;
        // System.out.println("Nanos per loop: "+ nanosPerLoop);
    }

    private long estimateLoopTime(long timeout) {
        // how many loops
        double val = timeout / nanosPerLoop;
        return Math.max(1, (int) Math.ceil(val));
    }

    @Override
    public Packet waitForNext(long timeout) {
        long estimateTime = estimateLoopTime(timeout);
        for (long timer = 0; timer < estimateTime; timer++) {
            Packet p = fetcher.getLatest();
            if (p != null) return p;
        }
        return null;
    }

    @Override
    public void signal() {
        // NOOP there it is
    }

}
