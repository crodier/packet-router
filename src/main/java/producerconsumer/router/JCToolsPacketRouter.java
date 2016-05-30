package producerconsumer.router;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpscArrayQueue;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import producerconsumer.router.wait.BlockingWaitStrategy;
import producerconsumer.router.wait.BusyWaitStrategy;
import producerconsumer.router.wait.WaitStrategy;

import java.util.Queue;

import static producerconsumer.router.JCToolsPacketRouter.QueueType.MPMC;
import static producerconsumer.router.JCToolsPacketRouter.QueueType.MPSC;
import static producerconsumer.router.JCToolsPacketRouter.QueueType.SPSC;

/**
 * http://jctools.github.io/JCTools/
 *
 * Leslie Lamport Queues (also known for Paxos fame..)
 */
public class JCToolsPacketRouter extends QueuePacketRouter implements PacketRouter {

    public enum QueueType {
        SPSC,
        MPSC,
        MPMC
    }

    BusyWaitStrategy busyWaitStrategy = new BusyWaitStrategy(this);
    BlockingWaitStrategy blockingWaitStrategy = new BlockingWaitStrategy(this);

    boolean isBlocking = false;

    public JCToolsPacketRouter(boolean blocking, QueueType queueType) {
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

        int POWER_OF_TWO = 22; // 8 million

        public JCToolsFactory(QueueType qt) {
            this.queue = qt;
        }
        @Override
        public Queue<Packet> makeQueue() {
            if (queue == SPSC)
                return new SpscArrayQueue<>(1 << POWER_OF_TWO);
            else if (queue == MPSC)
                return new MpscArrayQueue<>(1 << POWER_OF_TWO);
            else if (queue == MPMC)
                return new MpmcArrayQueue<>(1 << POWER_OF_TWO);
            else
                throw new RuntimeException("no queue #: "+queue);
        }
    }

}
