package producerconsumer.router;

/**
 * User: crodier
 */
public interface PacketRouter {
    void add(Packet p);

    Packet take(long timeout);

    Packet take();

    boolean isEmpty();
}
