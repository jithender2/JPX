package com.proxy.netty.codec.handler;

import com.proxy.netty.NettyUtils;
import io.netty.util.ReferenceCountUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import static com.proxy.netty.NettyUtils.causedByClientClose;

public class ReplayHandler extends ChannelInboundHandlerAdapter {

	private final Channel targetChannel;

	public ReplayHandler(Channel targetChannel) {
		this.targetChannel = targetChannel;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		

		ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
		if (targetChannel.isActive()) {
			targetChannel.writeAndFlush(msg);
		} else {

			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (targetChannel.isActive()) {
			NettyUtils.closeOnFlush(targetChannel);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
		if (causedByClientClose(e)) {

		} else {

		}
		NettyUtils.closeOnFlush(ctx.channel());
	}
}