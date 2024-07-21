/**
 * Copyright 2016-2018 Dell Inc. or its subsidiaries. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.Xdr;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.NotYetConnectedException;

import static com.emc.ecs.nfsclient.network.Connection.CONNECTION_OPTION;

/**
 * Each TCP connection has a corresponding ClientIOHandler instance.
 * ClientIOHandler includes the handler to receiving data and connection
 * establishment
 *
 * @author seibed
 */
public class ClientIOHandler extends SimpleChannelInboundHandler<Xdr> {

    /**
     * The usual logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ClientIOHandler.class);

    /**
     * The connection instance
     */
    private final Connection _connection;

    /**
     * The only constructor.
     *
     * @param bootstrap
     *            A Netty helper instance.
     */
    public ClientIOHandler(Bootstrap bootstrap) {
        _connection = (Connection) bootstrap.config().attrs().get(CONNECTION_OPTION);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Connected to: {}", ctx.channel().remoteAddress());
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        closeConnection("Channel disconnected", ctx);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeConnection("Channel closed", ctx);
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Xdr x) throws Exception {
        // remove the request from timeout manager map
        int xid = x.getXid();
        _connection.notifySender(xid, x);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // do not print exception if it is BindException.
        // we are trying to search available port below 1024. It is not good to
        // print a flood
        // of error logs during the searching.
        if (cause instanceof java.net.BindException) {
            return;
        }

        LOG.error("Exception on connection to " + ctx.channel().remoteAddress(), cause);

        // close the channel unless we are connecting and it is
        // NotYetConnectedException
        if (!((cause instanceof NotYetConnectedException)
                && _connection.getConnectionState().equals(Connection.State.CONNECTING))) {
            ctx.channel().close();
        }
        super.exceptionCaught(ctx, cause);
    }

    /**
     * Convenience method to standardize connection closing.We never try to
     * reconnect the tcp connections. the new connection will be launched when
     * new request is received. Reasons:
     * <ol>
     * <li>Portmap service will disconnect a tcp connection once it has been
     * idle for a few seconds.</li>
     * <li>Mounting service is listening to a temporary port, the port will
     * change after nfs server restart.</li>
     * <li>Even Nfs server may be listening to a temporary port.</li>
     * </ol>
     *
     * @param messageStart A string used to start the log message.
     * @param ctx
     */
    private void closeConnection(String messageStart, ChannelHandlerContext ctx) {
        LOG.warn(messageStart + ": {}", ctx.channel().remoteAddress());
        _connection.close();
    }

}
