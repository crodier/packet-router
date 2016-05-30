package producerconsumer.router;

import java.util.concurrent.*;

/**
 * Only for testing!!!!
 * This packet Router is Incorrect.
 * But it gives us a baseline for how much overhead we add by the priority of certain packets first,
 * as this is close to optimal.
 * It also shows us what a good theoretical lower bound is for performance.
 */
public class IncorrectPacketRouter implements PacketRouter {

    // BlockingQueue<Packet> onlyOneQueue = new ArrayBlockingQueue<Packet>(1000*1000*10);
    BlockingQueue<Packet> onlyOneQueue = new LinkedBlockingQueue<>();

    @Override
    public void add(Packet p) {
        onlyOneQueue.add(p);
    }

    @Override
    public Packet take(long timeoutMs) {
        try {
            return onlyOneQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
        return null;
    }

    @Override
    public Packet take() {
        return take(0);
    }

    @Override
    public boolean isEmpty() {
        return onlyOneQueue.isEmpty();
    }

}
