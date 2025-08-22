package com.proxy.netty;

import com.proxy.listener.SetLogger;
import com.proxy.netty.codec.detector.HttpMatcher;
import com.proxy.netty.codec.detector.ProtocolDetector;

import com.proxy.setting.ProxySetting;
import com.proxy.setting.ServerSetting;
import com.proxy.listener.MessageListener;
import io.netty.handler.proxy.ProxyHandler;
import com.proxy.netty.codec.CloseTimeoutChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import com.proxy.netty.codec.detector.HttpProxyMatcher;
import com.proxy.netty.codec.detector.HttpConnectProxyMatcher;
import com.proxy.netty.codec.detector.Socks4ProxyMatcher;
import com.proxy.netty.codec.detector.Socks5ProxyMatcher;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.channel.ChannelInitializer;
import java.net.InetSocketAddress;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ChannelFuture;
import java.util.function.Supplier;
import com.proxy.netty.codec.handler.ServerSSLContextManager;

import static java.util.Objects.requireNonNull;

/**
 * `Server` class starts and manages the Netty-based proxy server.  It
 * configures the server bootstrap, sets up the channel pipeline with
 * appropriate handlers, and binds to the specified port.
 */
public class Server {

	private final ServerSetting setting;
	private final ServerSSLContextManager sslContextManager;
	private final MessageListener messageListener;
	private final Supplier<ProxyHandler> proxyHandlerSupplier;

	private ChannelFuture bindFuture;
	private EventLoopGroup master;
	private EventLoopGroup worker;

	/**
	 * Constructs a new `ProxyServer`.
	 *
	 * @param setting            The server settings.
	 * @param messageListener    The message listener.
	 * @param sslContextManager  The SSL context manager.
	 * @param proxyHandlerSupplier The proxy handler supplier.
	 */
	public Server(ServerSetting setting, ServerSSLContextManager sslContextManager, ProxySetting proxySetting,
			MessageListener messageListener) {
		this.setting = requireNonNull(setting);
		this.sslContextManager = requireNonNull(sslContextManager);
		this.messageListener = requireNonNull(messageListener);
		this.proxyHandlerSupplier = new ProxyHandlerSupplier(requireNonNull(proxySetting));
	}

	/**
	 * Starts the proxy server.  Configures the Netty `ServerBootstrap`, sets up
	 * the channel pipeline, and binds to the specified port.
	 *
	 * @throws Exception If an error occurs during server startup.
	 */
	public void start() throws Exception {
		int coreCount = Runtime.getRuntime().availableProcessors();
		int workerThreads = Math.max(1, coreCount / 2);

		ServerBootstrap bootstrap = new ServerBootstrap();
		master = new NioEventLoopGroup(1, new DefaultThreadFactory("netty-master"));
		worker = new NioEventLoopGroup(workerThreads, new DefaultThreadFactory("netty-worker"));
		bootstrap.group(master, worker).channel(NioServerSocketChannel.class)
				.localAddress(new InetSocketAddress(setting.port()))
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) {
						int timeoutSeconds = setting.timeout();
						IdleStateHandler idleStateHandler = new IdleStateHandler(timeoutSeconds, timeoutSeconds,
								timeoutSeconds, TimeUnit.SECONDS);
						ch.pipeline().addLast(idleStateHandler);
						ch.pipeline().addLast(new CloseTimeoutChannelHandler());

						ProtocolDetector protocolDetector = new ProtocolDetector(
								new Socks5ProxyMatcher(messageListener, sslContextManager, proxyHandlerSupplier),
								new Socks4ProxyMatcher(messageListener, sslContextManager, proxyHandlerSupplier),
								new HttpConnectProxyMatcher(messageListener, sslContextManager, proxyHandlerSupplier),
								new HttpProxyMatcher(messageListener, proxyHandlerSupplier),
								new HttpMatcher(sslContextManager));
						//ch.pipeline().addLast("context-manager", ContextManagerHandler.getInstance());

				
						ch.pipeline().addLast("protocol-detector", protocolDetector);

					}
				});

		bindFuture = bootstrap.bind().sync();

	}

	/**
	 * Stops the proxy server gracefully.  Shuts down the event loop groups.
	 * This method should be called when the server is no longer needed.
	 */
	public void stop() {
		try {
			bindFuture.channel().close().sync();
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
		try {
			master.shutdownGracefully(0, 0, TimeUnit.SECONDS).sync();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		try {
			worker.shutdownGracefully(0, 0, TimeUnit.SECONDS).sync();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

	}

}