package producerconsumer.router;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PriorityBlockingQueuePacketRouter implements PacketRouter {

    // probably should code it both ways, with above, and a priority queue with CAS, see which is faster
    PriorityBlockingQueue<Packet> pq = new PriorityBlockingQueue<>();

    AtomicLong packetCounter = new AtomicLong(0);

    public Packet take() {
        try {
            return pq.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Packet take(long timeoutMs) {
        try {
            return pq.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(Packet p) {
        // PriorityQueue is not stable, sorting wise
        p.packetNo = packetCounter.incrementAndGet();
        pq.add(p);
    }


    // for the test harness
    @Override
    public boolean isEmpty() {
        return pq.isEmpty();
    }
}
