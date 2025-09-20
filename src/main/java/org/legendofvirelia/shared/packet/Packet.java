package org.legendofvirelia.shared.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Packet {
    void write(DataOutputStream out) throws IOException;
    void read(DataInputStream in) throws IOException;
    void handle(PacketHandler handler); // dispatch
}
