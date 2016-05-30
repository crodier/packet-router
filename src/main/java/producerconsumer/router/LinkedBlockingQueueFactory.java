package producerconsumer.router;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test to see if LinkedBlocking queues (not wait-free Concurrent Queues), help in the case of high contention.
 * (but seems they do not.)
 */
public class LinkedBlockingQueueFactory implements QueueFactory {
    @Override
    public BlockingQueue<Packet> makeQueue() {
        return new LinkedBlockingQueue<>();
    }
}
