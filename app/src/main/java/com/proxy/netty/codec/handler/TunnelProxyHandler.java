package com.proxy.netty.codec.handler ;
import com.proxy.utils.HostPort;
import com.proxy.netty.codec.handler.ChannelActiveAwareHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.ChannelInitializer;
import com.proxy.netty.NettySettings;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import com.proxy.netty.codec.handler.ServerSSLContextManager;
import java.util.function.Supplier;
import com.proxy.listener.MessageListener;
import io.netty.util.concurrent.Promise;
import io.netty.channel.ChannelHandlerContext;
import com.proxy.netty.codec.handler.SSLDetector;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.channel.ChannelOption;
import io.netty.bootstrap.Bootstrap;
/**
 * for socks/http connect tunnel
 */
public abstract class TunnelProxyHandler<T> extends SimpleChannelInboundHandler<T> {
	private final MessageListener messageListener;
	private final ServerSSLContextManager sslContextManager;
	private final Supplier<ProxyHandler> proxyHandlerSupplier;

	public TunnelProxyHandler(MessageListener messageListener, ServerSSLContextManager sslContextManager,
			Supplier<ProxyHandler> proxyHandlerSupplier) {
		this.messageListener = messageListener;
		this.sslContextManager = sslContextManager;
		this.proxyHandlerSupplier = proxyHandlerSupplier;
	}

	protected Bootstrap initBootStrap(Promise<Channel> promise, EventLoopGroup eventLoopGroup) {
		return new Bootstrap().group(eventLoopGroup).channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, NettySettings.CONNECT_TIMEOUT)
				.option(ChannelOption.SO_KEEPALIVE, true).handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) {
						ProxyHandler proxyHandler = proxyHandlerSupplier.get();
						if (proxyHandler != null) {
							ch.pipeline().addLast(proxyHandler);
						}
						ch.pipeline().addLast(new ChannelActiveAwareHandler(promise));
					}
				});
	}

	protected void initTcpProxyHandlers(ChannelHandlerContext ctx, HostPort address, Channel outChannel) {
		ctx.pipeline().addLast(new SSLDetector(address, messageListener, outChannel, sslContextManager));
	}

}