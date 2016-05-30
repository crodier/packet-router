package producerconsumer.router;

import producerconsumer.router.wait.BlockingWaitStrategy;
import producerconsumer.router.wait.BusyWaitStrategy;
import producerconsumer.router.wait.WaitStrategy;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentPacketRouter extends QueuePacketRouter implements PacketRouter {

    BusyWaitStrategy busyWaitStrategy = new BusyWaitStrategy(this);
    BlockingWaitStrategy blockingWaitStrategy = new BlockingWaitStrategy(this);

    boolean isBlocking = false;

    public ConcurrentPacketRouter(boolean isBlocking) {
        super(new ConcurrentLinkedQueueFactory());
        this.isBlocking = isBlocking;
    }

    @Override
    public WaitStrategy getWaitStrategy() {

        if (isBlocking)
            return blockingWaitStrategy;
        else
            return busyWaitStrategy;
    }


    /**
     * Concurrent linked queue are "wait-free", which works well except under high contention.
     */
    public static class ConcurrentLinkedQueueFactory implements QueueFactory {
        @Override
        public Queue<Packet> makeQueue() {
            return new ConcurrentLinkedQueue<>();
        }
    }

}
