package com.proxy.netty.codec.handler;

import com.proxy.listener.HttpInterceptorListener;

import com.proxy.listener.MessageListener;
import com.proxy.listener.SetLogger;
import com.proxy.utils.BodyType;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import java.io.Reader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import com.proxy.data.Header;
import java.util.List;
import com.proxy.data.StatusLine;
import com.proxy.data.Http1ResponseHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpObject;
import com.proxy.data.RequestLine;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import com.proxy.store.Body;
import com.proxy.data.Http1RequestHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandlerContext;
import com.proxy.utils.HostPort;
import com.proxy.data.Http1Message;
import io.netty.channel.ChannelDuplexHandler;
import static com.proxy.netty.NettyUtils.causedByClientClose;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * `HttpInterceptor` is a Netty channel handler that intercepts HTTP/1.1
 * messages (requests and responses). It assembles complete HTTP messages,
 * notifies a listener, and interacts with `HttpInterceptorListener` for
 * request interception and modification.
 */
public class HttpInterceptor extends ChannelDuplexHandler {

	// the current http request/response
	private Http1Message httpMessage;

	private final boolean ssl;
	private final HostPort address;
	private final MessageListener messageListener;

	public HttpInterceptor(boolean ssl, HostPort address, MessageListener messageListener) {
		this.ssl = ssl;
		this.address = address;
		this.messageListener = messageListener;
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws InterruptedException {
		if (!(msg instanceof HttpObject)) {
			ctx.write(msg, promise);
			return;
		}

		if (msg instanceof HttpRequest) {
			HttpRequest request = (HttpRequest) msg;

			// Convert headers and initialize body
			Http1RequestHeaders requestHeader = convertHeader(request);

			Body body = requestHeader.createBody();
			httpMessage = new Http1Message(ssl ? "https" : "http", address, requestHeader, body);

			// Notify listener with the current message
			messageListener.onMessage(httpMessage);

			// Do not forward headers immediately if interception is on
			boolean isInterceptionOn = HttpInterceptorListener.getInstance().isInterceptionOn();
			if (isInterceptionOn) {
			
					HttpInterceptorListener.getInstance().storePendingRequest(ctx, httpMessage, promise);

				return;
			}
		}

		if (msg instanceof HttpContent) {
			Http1Message message = this.httpMessage;
			ByteBuf content = ((HttpContent) msg).content();
			// Append body chunks
			if (content.readableBytes() > 0) {

				message.requestBody().append(content.nioBuffer());
			}
			if (msg instanceof LastHttpContent) {
				message.requestBody().finish();

			}

		}

		ctx.write(msg, promise).addListener(f -> {
			if (!f.isSuccess()) {

			}
		});
	}

	private static Http1RequestHeaders convertHeader(HttpRequest request) {
		RequestLine requestLine = new RequestLine(request.method().name(), request.uri(),
				request.protocolVersion().text());
		List<Header> headers = new ArrayList<>();

		request.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));

		return new Http1RequestHeaders(requestLine, headers);

	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		if (!(msg instanceof HttpObject)) {

			ctx.fireChannelRead(msg);
			return;
		}

		Http1Message message = this.httpMessage;
		if (msg instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) msg;
			Http1ResponseHeaders responseHeader = convertHeader(response);

			message.responseHeader(responseHeader);
			Body body = responseHeader.createBody();
			message.responseBody(body);
		}

		if (msg instanceof HttpContent) {
			ByteBuf content = ((HttpContent) msg).content();
			if (content.readableBytes() > 0) {
				message.responseBody().append(content.nioBuffer());
			}
			if (msg instanceof LastHttpContent) {

				message.responseBody().finish();
				this.httpMessage = null;
			}
		}

		ctx.fireChannelRead(msg);
	}

	private static Http1ResponseHeaders convertHeader(HttpResponse response) {
		StatusLine statusLine = new StatusLine(response.protocolVersion().text(), response.status().code(),
				response.status().reasonPhrase());
		List<Header> headers = new ArrayList<>();

		response.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
		return new Http1ResponseHeaders(statusLine, headers);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (causedByClientClose(cause)) {

		} else {

		}
		ctx.close();
	}

}