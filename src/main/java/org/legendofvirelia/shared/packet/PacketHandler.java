package org.legendofvirelia.shared.packet;

public interface PacketHandler {
    void handle(PlaceBlockPacket pkt);
    void handle(BlockChangePacket pkt);
    // add more types later
}
