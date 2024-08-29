package bgu.spl.net.impl.packets;

public enum PacketType {
    RRQ((short) 1),
    WRQ((short) 2),
    DATA((short) 3),
    ACK((short) 4),
    ERROR((short) 5),
    DIRQ((short) 6),
    LOGRQ((short) 7),
    DELRQ((short) 8),
    BCAST((short) 9),
    DISC((short) 10);

    private final short value;

    PacketType(short value) {
        this.value = value;
    }

    public static PacketType valueOf(short value) {
        for (PacketType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PacketType value: " + value);
    }
}