package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.Xdr;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static io.netty.buffer.Unpooled.wrappedBuffer;

/**
 * Encodes the RPC records for sending requests to the server
 */
public class RPCRecordEncoder extends MessageToMessageEncoder<Xdr> {

    private static final Logger LOG = LoggerFactory.getLogger(RPCRecordEncoder.class);

    /**
     * RFC suggest setting the record size to MTU (Ethernet: MTU=1500 - 40(ip
     * and tcp header). But, When we send multiple fragments continuously, some
     * NFS server kill the connection and the report the error below:
     * "RPC: multiple fragments per record not supported" To bypass this
     * limitation, set a big MTU_SIZE number now.
     */
    public static final int MTU_SIZE = 1024 * 1024;

    /**
     * Special constant used for the last fragment size. This is a number bigger
     * than the largest unsigned int, so it cannot be a real fragment size..
     */
    private static final int LAST_FRAG = 0x80000000;

    @Override
    protected void encode(ChannelHandlerContext ctx, Xdr rpcRequest, List<Object> out) throws Exception {
        // XDR header buffer
        List<ByteBuffer> buffers = new LinkedList<>();
        buffers.add(ByteBuffer.wrap(rpcRequest.getBuffer(), 0, rpcRequest.getOffset()));

        // payload buffer
        if (rpcRequest.getPayloads() != null) {
            buffers.addAll(rpcRequest.getPayloads());
        }

        List<ByteBuffer> outBuffers = new ArrayList<>();

        int bytesToWrite = 0;
        int remainingBuffers = buffers.size();
        boolean isLast = false;

        for (ByteBuffer buffer : buffers) {

            if (bytesToWrite + buffer.remaining() > MTU_SIZE) {
                if (outBuffers.isEmpty()) {
                    LOG.error("too big single byte buffer {}", buffer.remaining());
                    throw new IllegalArgumentException(
                            String.format("too big single byte buffer %d", buffer.remaining()));
                } else {

                    sendBuffers(out, bytesToWrite, outBuffers, isLast);

                    bytesToWrite = 0;
                    outBuffers.clear();
                }
            }

            outBuffers.add(buffer);
            bytesToWrite += buffer.remaining();
            remainingBuffers -= 1;
            isLast = (remainingBuffers == 0);
        }

        // send out remaining buffers
        if (!outBuffers.isEmpty()) {
            sendBuffers(out, bytesToWrite, outBuffers, true);
        }
    }

    private static void sendBuffers(List<Object> out, int bytesToWrite, List<ByteBuffer> outBuffers, boolean isLast) {
        ByteBuffer recSizeBuf = ByteBuffer.allocate(4);

        if (isLast) {
            recSizeBuf.putInt(LAST_FRAG | bytesToWrite);
        } else {
            recSizeBuf.putInt(bytesToWrite);
        }
        recSizeBuf.rewind();
        outBuffers.add(0, recSizeBuf);

        ByteBuffer[] outArray = outBuffers.toArray(new ByteBuffer[0]);
        ByteBuf channelBuffer = wrappedBuffer(outArray);
        out.add(channelBuffer);
    }

}
