package producerconsumer.router;


import producerconsumer.router.wait.BlockingWaitStrategy;
import producerconsumer.router.wait.BusyWaitStrategy;
import producerconsumer.router.wait.WaitStrategy;

public class LinkedPacketRouter extends QueuePacketRouter implements PacketRouter{

    BusyWaitStrategy busy = new BusyWaitStrategy(this);
    BlockingWaitStrategy blocking = new BlockingWaitStrategy(this);

    boolean isBlocking = false;

    public LinkedPacketRouter(boolean isBlocking) {
        super(new LinkedBlockingQueueFactory());
        this.isBlocking = isBlocking;
    }

    @Override
    public WaitStrategy getWaitStrategy() {
        // call the base class to try to get another latest
        if (isBlocking)
            return blocking;
        else
            return busy;
    }
}
