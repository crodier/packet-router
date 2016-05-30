package producerconsumer.router.wait;

import producerconsumer.router.GetLatestPacket;
import producerconsumer.router.Packet;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingWaitStrategy implements  WaitStrategy {

    private final GetLatestPacket fetcher;

    public ReentrantLock lock = new ReentrantLock();
    public Condition waitCond = lock.newCondition();

    public BlockingWaitStrategy(GetLatestPacket fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public Packet waitForNext(long timeout) {
        try {
            boolean locked = lock.tryLock(timeout, TimeUnit.NANOSECONDS);

            if (locked) {
                waitCond.await(timeout, TimeUnit.NANOSECONDS);
                return fetcher.getLatest();
            }
            return null;
        } catch (InterruptedException e) {
            // expected
            return fetcher.getLatest(); // could be a spurious wakeup, permitted depending on platform.
        }
        finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }

    @Override
    public void signal() {
        // wake up the waiter
        boolean locked = lock.tryLock();
        try {
            if (locked)
                waitCond.signal();
        }
        finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }
}
