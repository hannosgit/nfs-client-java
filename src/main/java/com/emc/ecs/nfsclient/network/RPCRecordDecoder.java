package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.Xdr;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static com.emc.ecs.nfsclient.network.RecordMarkingUtil.removeRecordMarking;

/**
 * Decodes {@link ByteBuf} to {@link Xdr}
 */
public class RPCRecordDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        final Xdr xdr = removeRecordMarking(bytes);
        out.add(xdr);
    }

}
