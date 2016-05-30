package producerconsumer.router;


import producerconsumer.router.wait.BlockingWaitStrategy;
import producerconsumer.router.wait.WaitStrategy;

public class LinkedLockPacketRouter extends QueuePacketRouter implements PacketRouter{

    WaitStrategy waitStrategy = new BlockingWaitStrategy(this);

    public LinkedLockPacketRouter() {
        super(new LinkedBlockingQueueFactory());
    }

    @Override
    public WaitStrategy getWaitStrategy() {
        // call the base class to try to get another latest
        return waitStrategy;
    }
}
