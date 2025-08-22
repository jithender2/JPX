package com.proxy.netty.codec.handler;

import com.proxy.listener.MessageListener;
import com.proxy.store.Body;
import com.proxy.utils.BodyType;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;

import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandlerContext;
import java.util.Objects;
import java.util.Optional;
import com.proxy.data.WebSocketMessage;
import io.netty.channel.ChannelDuplexHandler;
import static com.proxy.netty.NettyUtils.causedByClientClose;

public class WebSocketInterceptor extends ChannelDuplexHandler {
	private WebSocketMessage requestMessage;
	private WebSocketMessage responseMessage;

	private final String host;
	private final String url;
	private final MessageListener messageListener;

	public WebSocketInterceptor(String host, String url, MessageListener messageListener) {
		this.host = host;
		this.url = url;
		this.messageListener = Objects.requireNonNull(messageListener);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		filterWebSocketFrame(ctx, msg, false);
		ctx.fireChannelRead(msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		filterWebSocketFrame(ctx, msg, true);
		ctx.write(msg, promise);
	}

	private void filterWebSocketFrame(ChannelHandlerContext ctx, Object msg, boolean request) {
		if (msg instanceof EmptyByteBuf) {
			((EmptyByteBuf) msg).release();
			return;
		}
		if (!(msg instanceof WebSocketFrame)) {
			//	logger.debug("not web-socket frame type: {}", msg.getClass().getName());
		}

		if (msg instanceof BinaryWebSocketFrame) {
			BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;

			newWebSocketMessage(ctx, frame, WebSocketMessage.TYPE_BINARY, request);
		} else if (msg instanceof TextWebSocketFrame) {
			TextWebSocketFrame frame = (TextWebSocketFrame) msg;

			newWebSocketMessage(ctx, frame, WebSocketMessage.TYPE_TEXT, request);
		} else if (msg instanceof PingWebSocketFrame) {
			//	logger.debug("PingWebSocketFrame received");
		} else if (msg instanceof PongWebSocketFrame) {
			//logger.debug("PongWebSocketFrame received");
		} else if (msg instanceof CloseWebSocketFrame) {
			CloseWebSocketFrame frame = (CloseWebSocketFrame) msg;
			//logger.debug("CloseWebSocketFrame received, status:{}, reason:{}", frame.statusCode(), frame.reasonText());
		} else if (msg instanceof ContinuationWebSocketFrame) {
			WebSocketMessage message;
			if (request) {
				message = requestMessage;
			} else {
				message = responseMessage;
			}
			if (message == null) {
				//logger.error("ContinuationWebSocketFrame without first frame");
			} else {
				ContinuationWebSocketFrame frame = (ContinuationWebSocketFrame) msg;
				ByteBuf content = frame.content();
				message.body().append(content.nioBuffer());
				if (frame.isFinalFragment()) {
					message.body().finish();
					if (request) {
						requestMessage = message;
					} else {
						responseMessage = message;
					}
				}
			}

		} else {

		}
	}

	private void newWebSocketMessage(ChannelHandlerContext ctx, WebSocketFrame frame, int type, boolean request) {
		BodyType bodyType = type == WebSocketMessage.TYPE_TEXT ? BodyType.text : BodyType.binary;
		Body body = new Body(bodyType, Optional.empty(), "");
		ByteBuf content = frame.content();
		body.append(content.nioBuffer());
		WebSocketMessage message = new WebSocketMessage(host, url, type, request, body);
		messageListener.onMessage(message);
		if (frame.isFinalFragment()) {
			body.finish();
		} else {
			if (request) {
				responseMessage = message;
			} else {
				responseMessage = message;
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (causedByClientClose(cause)) {
			//logger.warn("client closed connection: {}", cause.getMessage());
		} else {
			//	logger.error("websocket error", cause);
		}
		ctx.close();
	}
}