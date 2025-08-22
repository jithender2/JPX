package com.proxy.netty.codec.detector;
import com.proxy.listener.MessageListener;
import com.proxy.listener.SetLogger;
import com.proxy.netty.codec.handler.HttpConnectProxyInitializer;
import com.proxy.netty.codec.handler.ServerSSLContextManager;

import com.proxy.netty.codec.detector.ProtocolMatcher;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.channel.ChannelPipeline;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class HttpConnectProxyMatcher extends ProtocolMatcher {

	private final MessageListener messageListener;
	private final ServerSSLContextManager sslContextManager;
	private final Supplier<ProxyHandler> proxyHandlerSupplier;

	public HttpConnectProxyMatcher(MessageListener messageListener, ServerSSLContextManager sslContextManager,
			Supplier<ProxyHandler> proxyHandlerSupplier) {
		this.messageListener = messageListener;
		this.sslContextManager = sslContextManager;
		this.proxyHandlerSupplier = proxyHandlerSupplier;
	}

	@Override
	public int match(ByteBuf buf) {
		
		if (buf.readableBytes() < 8) {
			return PENDING;
		}

		String method = buf.toString(0, 8, US_ASCII);
		if (!"CONNECT ".equalsIgnoreCase(method)) {
			return MISMATCH;
		}

		return MATCH;
	}

	@Override
	public void handlePipeline(ChannelPipeline pipeline) {
		pipeline.addLast(new HttpServerCodec());
		pipeline.addLast(new HttpConnectProxyInitializer(messageListener, sslContextManager, proxyHandlerSupplier));
	}
}