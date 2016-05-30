package producerconsumer.router;

import java.util.Queue;

public interface  QueueFactory {
    public Queue<Packet> makeQueue();
}
