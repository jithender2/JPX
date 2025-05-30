package com.proxy.netty.codec.handler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import io.netty.util.concurrent.Promise;

public class ChannelActiveAwareHandler extends ChannelInboundHandlerAdapter {
	
	private final Promise<Channel> promise;
	
	public ChannelActiveAwareHandler(Promise<Channel> promise) {
		this.promise = promise;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.pipeline().remove(this);
		promise.setSuccess(ctx.channel());
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
		promise.setFailure(throwable);
	}
}