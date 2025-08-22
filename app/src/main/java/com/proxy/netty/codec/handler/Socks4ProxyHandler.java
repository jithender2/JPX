package com.proxy.netty.codec.handler;


import com.proxy.netty.NettyUtils;
import com.proxy.listener.MessageListener;
import io.netty.channel.ChannelFuture;
import com.proxy.utils.HostPort;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.util.concurrent.FutureListener;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.REJECTED_OR_FAILED;
import io.netty.handler.codec.socksx.v4.Socks4Message;
import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.SUCCESS;
import io.netty.handler.proxy.ProxyHandler;
import java.util.function.Supplier;

public class Socks4ProxyHandler extends TunnelProxyHandler<Socks4Message> {

	public Socks4ProxyHandler(MessageListener messageListener, ServerSSLContextManager sslContextManager,
			Supplier<ProxyHandler> proxyHandlerSupplier) {
		super(messageListener, sslContextManager, proxyHandlerSupplier);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Socks4Message socksRequest) {
		Socks4CommandRequest command = (Socks4CommandRequest) socksRequest;
		if (command.type() != Socks4CommandType.CONNECT) {
			NettyUtils.closeOnFlush(ctx.channel());

			return;
		}
		Promise<Channel> promise = ctx.executor().newPromise();
		Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

		bootstrap.connect(command.dstAddr(), command.dstPort()).addListener((ChannelFutureListener) future -> {
			if (future.isSuccess()) {
				ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(REJECTED_OR_FAILED));
				NettyUtils.closeOnFlush(ctx.channel());
			}
		});

		promise.addListener((FutureListener<Channel>) future -> {
			Channel outboundChannel = future.getNow();
			if (!future.isSuccess()) {
				ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(REJECTED_OR_FAILED));
				NettyUtils.closeOnFlush(ctx.channel());
				return;
			}
			ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(SUCCESS));

			responseFuture.addListener((ChannelFutureListener) channelFuture -> {
				ctx.pipeline().remove(Socks4ProxyHandler.this);
				ctx.pipeline().remove(Socks4ServerEncoder.class);
				ctx.pipeline().remove(Socks4ServerDecoder.class);
				HostPort address = HostPort.of(command.dstAddr(), command.dstPort());
				initTcpProxyHandlers(ctx, address, outboundChannel);
			});
		});
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {

		NettyUtils.closeOnFlush(ctx.channel());
	}

}