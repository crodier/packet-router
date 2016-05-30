package producerconsumer.router;

/**
 * Test, to lookup packet types in hash maps
 */
public class PacketTypeKey {
    public Packet.PacketType packetType;
    public boolean isLarge;

    public PacketTypeKey(boolean isLarge, Packet.PacketType packetType) {
        this.isLarge = isLarge;
        this.packetType = packetType;
    }

    @Override
    public int hashCode() {
        if (isLarge)
            return 17 * packetType.hashCode();
        return packetType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        PacketTypeKey other = (PacketTypeKey) obj;
        return other.packetType == this.packetType && other.isLarge == this.isLarge;
    }
}
