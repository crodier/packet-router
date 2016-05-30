package producerconsumer.router;


// import org.jctools.queues.FFBuffer;
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
public class FFBufferPacketRouter extends QueuePacketRouter implements PacketRouter {

    BusyWaitStrategy busyWaitStrategy = new BusyWaitStrategy(this);
    BlockingWaitStrategy blockingWaitStrategy = new BlockingWaitStrategy(this);

    boolean isBlocking = false;

    public FFBufferPacketRouter(boolean blocking, QueueType queueType) {
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
//            if (queue == QueueType.SPSC)
//                return new FFBuffer<>(1 << POWER_OF_TWO);
//            else if (queue == QueueType.MPSC)
//                return new FFBuffer<>(1 << POWER_OF_TWO);
//            else if (queue == QueueType.MPMC)
//                return new FFBuffer<>(1 << POWER_OF_TWO);
//            else
                throw new RuntimeException("no queue #: "+queue);
        }
    }

}
