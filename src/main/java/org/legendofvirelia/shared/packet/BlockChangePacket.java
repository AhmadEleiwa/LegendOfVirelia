package org.legendofvirelia.shared.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BlockChangePacket implements Packet {
    public int x,y,z,blockId;
    /* similar to above */

     public BlockChangePacket() {}
    public BlockChangePacket(int x,int y,int z,int blockId) {
        this.x=x;this.y=y;this.z=z;this.blockId=blockId;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        out.writeInt(blockId);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        x=in.readInt();
        y=in.readInt();
        z=in.readInt();
        blockId=in.readInt();
    }

    @Override
    public void handle(PacketHandler handler) {
        handler.handle(this);
    }
}
