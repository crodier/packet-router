package producerconsumer.router.wait;

import producerconsumer.router.Packet;

/**
 * Allows for plugging in different Wait Strategies for testing
 */
public interface WaitStrategy {
    public Packet waitForNext(long timeout);

    void signal();
}
