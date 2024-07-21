package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.Xdr;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static com.emc.ecs.nfsclient.network.RecordMarkingUtil.removeRecordMarking;

public class RPCRecordDecoder extends SimpleChannelInboundHandler<byte[]> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] bytes) throws Exception {
        final Xdr xdr = removeRecordMarking(bytes);

        super.channelRead(ctx, xdr);
    }
}
