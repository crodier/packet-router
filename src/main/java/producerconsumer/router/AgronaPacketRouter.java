package producerconsumer.router;


import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;
import producerconsumer.router.wait.BlockingWaitStrategy;
import producerconsumer.router.wait.BusyWaitStrategy;
import producerconsumer.router.wait.WaitStrategy;

import java.util.Queue;

/**
 * https://github.com/real-logic/Agrona
 *
 * Like JCTools, another crack at Leslie Lamport Queues
 *
 * From Martin Thomson, finance.
 */
public class AgronaPacketRouter extends QueuePacketRouter implements PacketRouter {

    // 16 million, as the ring buffer size
    // may be altered depending on messaging needs, and easily analyzed
    static int POWER_OF_TWO = 23;

    BusyWaitStrategy busyWaitStrategy = new BusyWaitStrategy(this);
    BlockingWaitStrategy blockingWaitStrategy = new BlockingWaitStrategy(this);

    boolean isBlocking = false;

    public AgronaPacketRouter(boolean blocking, QueueType queueType) {
        super(new JCToolsFactory(queueType));
        isBlocking = blocking;
    }

    @Override
    public WaitStrategy getWaitStrategy() {
        if (isBlocking)
            return blockingWaitStrategy;
        else
            return busyWaitStrategy;
    }

    /**
     * Only works for single producer, single consumer!
     */
    public static class JCToolsFactory implements QueueFactory {
        private final QueueType queue;

        public JCToolsFactory(QueueType qt) {
            this.queue = qt;
        }
        @Override
        public Queue<Packet> makeQueue() {
            if (queue == QueueType.SPSC)
                return new OneToOneConcurrentArrayQueue<>(1 << POWER_OF_TWO);
            else if (queue == QueueType.MPSC)
                return new ManyToOneConcurrentArrayQueue<>(1 << POWER_OF_TWO);
            else if (queue == QueueType.MPMC)
                return new ManyToManyConcurrentArrayQueue<>(1 << POWER_OF_TWO);
            else
                throw new RuntimeException("no queue #: "+queue);
        }
    }

}
