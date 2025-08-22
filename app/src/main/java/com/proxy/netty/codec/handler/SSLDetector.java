package com.proxy.netty.codec.handler;

import com.proxy.listener.MessageListener;
import com.proxy.listener.SetLogger;
import com.proxy.netty.NettyUtils;
import com.proxy.netty.codec.Http2EventCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.channel.ChannelHandlerContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.channel.Channel;
import com.proxy.utils.HostPort;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInboundHandlerAdapter;
import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;
import java.util.Queue;
import static com.proxy.netty.NettyUtils.causedByClientClose;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_2;
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;

public class SSLDetector extends ChannelInboundHandlerAdapter {

	private final Cumulator cumulator = MERGE_CUMULATOR;
	private ByteBuf buf;
	private boolean isSSL;
	private Queue<ByteBuf> queue;
	private boolean removed;

	private final HostPort address;
	private final MessageListener messageListener;
	private final Channel outboundChannel;
	private final ServerSSLContextManager sslContextManager;

	public SSLDetector(HostPort address, MessageListener messageListener, Channel outboundChannel,
			ServerSSLContextManager sslContextManager) {
		this.address = address;
		this.messageListener = messageListener;
		this.outboundChannel = outboundChannel;
		this.sslContextManager = sslContextManager;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (!(msg instanceof ByteBuf)) {

			NettyUtils.closeOnFlush(ctx.channel());
			return;
		}

		ByteBuf in = (ByteBuf) msg;
		if (isSSL) {
			queue.add(in);
			return;
		}

		if (buf == null) {
			buf = in;
		} else {
			buf = cumulator.cumulate(ctx.alloc(), buf, in);
		}

		if (buf.readableBytes() < 3) {
			return;
		}
		byte first = buf.getByte(buf.readerIndex());
		byte second = buf.getByte(buf.readerIndex() + 1);
		byte third = buf.getByte(buf.readerIndex() + 2);
		if (!(first == 22 && second <= 3 && third <= 3)) {
			// not ssl
			setHttpInterceptor(ctx, false);
			ctx.pipeline().remove(this);
			return;
		}

		isSSL = true;
		queue = new ArrayDeque<>(2);

		SslContext sslContext = ClientSSLContextManager.getInstance().get();
		SslHandler sslHandler = sslContext.newHandler(ctx.alloc(), address.host(), address.ensurePort());
		outboundChannel.pipeline().addLast("ssl-handler", sslHandler);

		outboundChannel.pipeline().addLast(new ApplicationProtocolNegotiationHandler(HTTP_1_1) {
			@Override
			protected void configurePipeline(ChannelHandlerContext c, String protocol) {
				// logger.debug("alpn with target server {}: {}", address.host(), protocol);
				boolean useH2 = protocol.equalsIgnoreCase(HTTP_2);
				if (address.host().contains("web-security-academy.net")) {
					useH2 = false;
				}
				
				SslContext serverContext = sslContextManager.createSSlContext(address.host(), useH2);
				SslHandler serverSSLHandler = serverContext.newHandler(ctx.alloc());
				ctx.pipeline().addLast("ssl-handler", serverSSLHandler);
				ctx.pipeline().addLast(new ApplicationProtocolNegotiationHandler(HTTP_1_1) {
					@Override
					protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Http2Exception {
						//logger.debug("alpn with client {}: {}", address.host(), protocol);
						if (protocol.equalsIgnoreCase(HTTP_2)) {
							setHttp2Interceptor(ctx);
						} else {
							setHttpInterceptor(ctx, true);
						}
					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
						if (causedByClientClose(cause)) {
							//logger.warn("client closed connection: {}", cause.getMessage());
						} else {
							//logger.error("application protocol negotiation error", cause);
						}
						ctx.close();
					}
				});
				if (!removed) {
					ctx.pipeline().remove(SSLDetector.this);
					removed = true;
				}
			}
		});
	}

	private void setHttp2Interceptor(ChannelHandlerContext ctx) throws Http2Exception {
		ctx.pipeline().addLast("http2-frame-codec", new Http2EventCodec());
		ctx.pipeline().addLast("replay-handler", new ReplayHandler(outboundChannel));
		outboundChannel.pipeline().addLast("http2-frame-codec", new Http2EventCodec());
		
		Http2Interceptor interceptor = new Http2Interceptor(address, messageListener, false);
		outboundChannel.pipeline().addLast("http2-interceptor", interceptor);
		outboundChannel.pipeline().addLast("replay-handler", new ReplayHandler(ctx.channel()));
	}

	private void setHttpInterceptor(ChannelHandlerContext ctx, boolean ssl) {

		ctx.pipeline().addLast("http-codec", new HttpServerCodec());
		ctx.pipeline().addLast(new HttpServerExpectContinueHandler());

		ctx.pipeline().addLast("replay-handler", new ReplayHandler(outboundChannel));
		outboundChannel.pipeline().addLast("http-codec", new HttpClientCodec());

		HttpUpgradeHandler httpUpgradeHandler = new HttpUpgradeHandler(ssl, address, messageListener, ctx.pipeline());
		outboundChannel.pipeline().addLast("http-upgrade-handler", httpUpgradeHandler);
		HttpInterceptor httpInterceptor = new HttpInterceptor(ssl, address, messageListener);
		outboundChannel.pipeline().addLast("http-interceptor", httpInterceptor);
		outboundChannel.pipeline().addLast("replay-handler", new ReplayHandler(ctx.channel()));

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (buf != null) {
			buf.release();
			buf = null;
		}

		NettyUtils.closeOnFlush(ctx.channel());
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		removed = true;
		if (buf != null) {
			ctx.fireChannelRead(buf);
		}
		if (queue != null) {
			ByteBuf b;
			while ((b = queue.poll()) != null) {
				ctx.fireChannelRead(b);
			}
		}
		ctx.flush();
		queue = null;
		buf = null;
	}
}