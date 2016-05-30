package producerconsumer.router;

import producerconsumer.router.wait.WaitStrategy;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * Use a series of linked blocking queues to achieve priority.
 * Sweep across the queues in order to get the latest, in priority order.
 * We can use a sweep, due to the relaxed ordering requirements as far as
 * consistency of Adds while Reads are taking place (allowed to be inconsistent, hinted in the problem definition.)
 *
 * A WaitStrategy is used to test a Busy Wait for an estimated sleep time, vs. a notification on a lock.
 */
public abstract class QueuePacketRouter implements PacketRouter, GetLatestPacket {

    private Queue<Packet> mgmtLarge;
    private Queue<Packet> mgmt;
    private Queue<Packet> userLarge;
    private Queue<Packet> user;

    public QueuePacketRouter(QueueFactory queueFactory) {

        // allows for testing various queue types
        mgmtLarge = queueFactory.makeQueue();
        mgmt = queueFactory.makeQueue();
        userLarge = queueFactory.makeQueue();
        user = queueFactory.makeQueue();
    }

    @Override
    public void add(Packet p) {
        switch (p.getPacketType())
        {
            case Management:
                if (p.isLarge()) mgmtLarge.add(p); else mgmt.add(p);
                break;
            case User:
                if (p.isLarge()) userLarge.add(p); else user.add(p);
                break;
        }
        getWaitStrategy().signal();
    }

    public abstract WaitStrategy getWaitStrategy();

    @Override
    public Packet take(long timeout) {

        Packet p = getLatest();
        // no need to wait if we get one, or if timeout is zero don't wait
        if (p != null || timeout == 0) { return p; }

        // waitStrategy sweep a fixed number of times based on the estimate
        return getWaitStrategy().waitForNext(timeout);
    }

    public Packet take() {
        return take(0);
    }

    /**
     * In every queue implementation, you can't simply poll(timeout)
     * Because a packet may arrive for a lesser priority queue, while you are polling the higher priority queue.
     */
    public Packet getLatest() {
        Packet p = mgmtLarge.poll();
        if (p != null) return p;
        p = mgmt.poll();
        if (p != null) return p;
        p = userLarge.poll();
        if (p != null) return p;

        p = user.poll();

        return p;
    }

    /**
     * Warning, not O(1), O(N)
     * @return
     */
    @Override
    public boolean isEmpty() {
        if (mgmtLarge.isEmpty() == false) return false;
        if (mgmt.isEmpty() == false) return false;
        if (userLarge.isEmpty() == false) return false;
        if (user.isEmpty() == false) return false;

        return true;
    }
}
