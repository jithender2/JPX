package com.proxy.netty.codec;

import io.netty.handler.timeout.IdleStateEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelDuplexHandler;

/**
 * `CloseTimeoutChannelHandler` is a Netty channel handler that closes the
 * connection when an idle timeout event is triggered.  It extends
 * `ChannelDuplexHandler` to handle both inbound (channelRead) and outbound
 * (write) events, although it currently only uses the
 * `userEventTriggered` method.
 */
public class CloseTimeoutChannelHandler extends ChannelDuplexHandler {

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;

			ctx.channel().close();
		}
	}
}