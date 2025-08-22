package com.proxy.netty.codec.handler;

import com.proxy.data.Header;
import com.proxy.data.Http2ResponseHeaders;
import com.proxy.data.Http2RequestHeaders;
import com.proxy.data.Http2Message;
import com.proxy.listener.Http2InterceptorListener;
import com.proxy.listener.MessageListener;
import com.proxy.listener.SetLogger;
import com.proxy.netty.codec.frame.Http2PushPromiseEvent;
import com.proxy.netty.codec.frame.Http2Event;
import com.proxy.netty.codec.frame.Http2DataEvent;
import com.proxy.netty.codec.frame.Http2StreamEvent;
import com.proxy.store.Body;
import com.proxy.netty.codec.frame.IHttp2HeadersEvent;
import com.proxy.utils.HostPort;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Headers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.proxy.netty.NettyUtils.causedByClientClose;
import java.util.Set;
import static java.util.stream.Collectors.toList;

public class Http2Interceptor extends ChannelDuplexHandler {
	private Map<Integer, Http2Message> messageMap = new HashMap<>();
	private final HostPort address;
	private final MessageListener messageListener;
	// if is from clear text upgrade
	private final boolean clearText;
	// only for clear text
	private final String method;
	private final String path;
	int isUnWanted;

	public Http2Interceptor(HostPort address, MessageListener messageListener, boolean clearText) {
		this.address = address;
		this.messageListener = messageListener;
		this.clearText = clearText;
		this.method = "";
		this.path = "";
	}

	public Http2Interceptor(HostPort address, MessageListener messageListener, boolean clearText, String method,
			String path) {
		this.address = address;
		this.messageListener = messageListener;
		this.clearText = clearText;
		this.method = method;
		this.path = path;
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {

		if (!(msg instanceof Http2StreamEvent)) {
			ctx.write(msg, promise);
			return;
		}
		Http2StreamEvent event = (Http2StreamEvent) msg;
		int streamId = event.streamId();

		boolean isInterceptionOn = Http2InterceptorListener.getInstance().isInterceptionOn();

		if (msg instanceof IHttp2HeadersEvent) {
			IHttp2HeadersEvent headersEvent = (IHttp2HeadersEvent) msg;
			// create new Http2Message, set headers

			Http2RequestHeaders requestHeaders = onRequestHeaders(streamId, headersEvent.headers(),
					headersEvent.endOfStream(), ctx.channel().id());
			if (isUnwantedRequest(headersEvent.headers())) {
				isUnWanted = streamId;

			}

			if (headersEvent.endOfStream()) {
				if (isUnWanted == streamId) {
					ctx.write(msg, promise);
					isUnWanted = -1;
					return;
				}

				if (isInterceptionOn) {
					

					Http2InterceptorListener.getInstance().storePendingRequest(ctx, msg, promise, streamId);
					return;

				}
			}

		}

		if (msg instanceof Http2DataEvent) {
			if (isUnWanted == streamId) {
				ctx.write(msg, promise);
				isUnWanted = -1;
				return;
			}

			Http2DataEvent dataEvent = (Http2DataEvent) msg;

			Http2Message message = messageMap.get(streamId);
			if (message != null) {
				Body body = message.requestBody();

				body.append(dataEvent.data().nioBuffer());
				if (dataEvent.endOfStream()) {
					
					body.finish();
					if (isInterceptionOn) {
						Http2InterceptorListener.getInstance().storePendingRequest(ctx, message, promise, streamId);
						return;
					}

				}

			} else {

				throw new RuntimeException("message not found with stream id: " + streamId);
			}

		}
		
		ctx.write(msg, promise);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (!(msg instanceof Http2Event)) {
			ctx.fireChannelRead(msg);
			return;
		}
		if (!(msg instanceof Http2StreamEvent)) {
			ctx.fireChannelRead(msg);
			return;
		}
		Http2StreamEvent event = (Http2StreamEvent) msg;

		int streamId = event.streamId();

		if (msg instanceof Http2PushPromiseEvent) {
			Http2PushPromiseEvent pushPromiseEvent = (Http2PushPromiseEvent) event;
			onRequestHeaders(pushPromiseEvent.promisedStreamId(), pushPromiseEvent.headers(), true, ctx.channel().id());
		}

		if (msg instanceof IHttp2HeadersEvent) {
			IHttp2HeadersEvent headersEvent = (IHttp2HeadersEvent) event;
			Http2Message message = messageMap.get(streamId);
			if (message == null) {
				// h2c upgrade response
				if (clearText && streamId == 1) {
					Http2RequestHeaders fakeRequestHeaders = new Http2RequestHeaders(
							List.of(new Header("", "mock request for h2c upgrade. look back for upgrade request")),
							"http", method, path);
					Body body = fakeRequestHeaders.createBody();
					body.finish();
					message = new Http2Message(address, fakeRequestHeaders, body, streamId);
					messageListener.onMessage(message);
					messageMap.put(streamId, message);
				}
			}

			if (message != null) {
				Http2Headers nettyHeaders = headersEvent.headers();

				List<Header> headers = convertHeaders(nettyHeaders);
				Http2ResponseHeaders responseHeaders = new Http2ResponseHeaders(
						Integer.parseInt(nettyHeaders.status().toString()), headers);
				
				message.setResponseHeader(responseHeaders);

				message.setResponseBody(responseHeaders.createBody());
				if (headersEvent.endOfStream()) {
					message.responseBody().finish();
					messageMap.remove(streamId);
				}
				messageListener.onMessage(message);
			} else {
				//	logger.error("message for stream id {} not found", streamId);
			}
		}

		if (msg instanceof Http2DataEvent) {
			Http2DataEvent dataEvent = (Http2DataEvent) msg;
			Http2Message message = messageMap.get(streamId);
			if (message != null) {
				Body body = message.responseBody();
				body.append(dataEvent.data().nioBuffer());
				if (dataEvent.endOfStream()) {
					body.finish();
					messageMap.remove(streamId);
				}
			} else {

			}
		}

		ctx.fireChannelRead(msg);
	}

	private Http2RequestHeaders onRequestHeaders(int streamId, Http2Headers http2Headers, boolean endOfStream,
			ChannelId channelId) {

		List<Header> headers = convertHeaders(http2Headers);

		Http2RequestHeaders requestHeaders = new Http2RequestHeaders(headers, http2Headers.scheme().toString(),
				http2Headers.method().toString(), http2Headers.path().toString());

		Http2Message message = new Http2Message(address, requestHeaders, requestHeaders.createBody(), streamId);
		if (endOfStream) {
			Body body = message.requestBody();

			body.finish();
		}
		//messageListener.onMessage(message);
		messageMap.put(streamId, message);
		return requestHeaders;
	}

	private List<Header> convertHeaders(Http2Headers nettyHeaders) {
		ArrayList<Header> http2HeadersArrayList = new ArrayList<>();
		for (Map.Entry next : nettyHeaders) {
			if (next.getKey() != null) {
				http2HeadersArrayList.add(new Header(next.getKey().toString(), next.getValue().toString()));
			}
		}
		return http2HeadersArrayList;
	}

	private boolean isSystemRequest(Http2Headers headers) {
		String authority = headers.authority().toString();
		String path = headers.path().toString();

		// Detect common patterns for system requests
		return authority.contains("googleapis.com") || path.contains("/v1/pages") || path.contains("/stats")
				|| path.contains("/github/collect") | authority.contains("accounts.google.com")
				|| headers.contains("x-goog-api-key");
	}

	private boolean isUnwantedRequest(Http2Headers headers) {
		return isSystemRequest(headers) || isAdvertisementRequest(headers) || isAnalyticsOrTrackingRequest(headers);

	}

	private boolean isAnalyticsOrTrackingRequest(Http2Headers headers) {
		String authority = headers.authority().toString();
		String path = headers.path().toString();

		return authority.contains("analytics") || authority.contains("tracking") || headers.contains("x-tracking-id");
	}

	private boolean isAdvertisementRequest(Http2Headers headers) {
		String authority = headers.authority().toString();
		String path = headers.path().toString();

		return authority.contains("doubleclick.net") || authority.contains("googleads") || path.contains("ads?")
				|| path.contains("slotname");
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (causedByClientClose(cause)) {

		} else {

		}
		ctx.close();
	}
}