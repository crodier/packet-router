package producerconsumer.router;

public class Packet implements Comparable<Packet> {

    public enum PacketType { Management, User};

    PacketType packetType;
    boolean isLarge;

    Long packetNo; // priority queue needs to stamp this

    @Override
    public String toString() {
        return new StringBuilder().append(", type: ").append(packetType).append(", size: ").append(isLarge ? "large" : "small").toString();
    }

    @Override
    public int compareTo(Packet o) {
        if (this == o) return 0; // minor speed optimization for equal comparisons

        if (false == this.packetType.equals(o.packetType))
            return this.packetType.compareTo(o.packetType);

        if (this.isLarge() != o.isLarge()) {
            if (this.isLarge()) return -1;
            return 1;
        }

        if (packetNo != null && o.packetNo != null)
            return packetNo.compareTo(o.packetNo);

        return 0;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public Packet setPacketType(PacketType packetType) {
        this.packetType = packetType;
        return this;
    }

    public boolean isLarge() {
        return isLarge;
    }

    public Packet setLarge(boolean large) {
        isLarge = large;
        return this;
    }

    public Packet setPacketNo(Long packetNo) {
        this.packetNo = packetNo;
        return this;
    }
}
