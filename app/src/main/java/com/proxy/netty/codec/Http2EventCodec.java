package com.proxy.netty.codec;

import com.proxy.netty.codec.frame.Http2HeadersEvent;
import com.proxy.netty.codec.frame.Http2UnknownEvent;
import com.proxy.netty.codec.frame.Http2WindowUpdateEvent;
import com.proxy.netty.codec.frame.Http2GoAwayEvent;
import com.proxy.netty.codec.frame.Http2SettingAckEvent;
import com.proxy.netty.codec.frame.Http2PingAckEvent;
import com.proxy.netty.codec.frame.Http2PushPromiseEvent;
import com.proxy.netty.codec.frame.Http2PingEvent;
import com.proxy.netty.codec.frame.Http2SettingEvent;
import com.proxy.netty.codec.frame.Http2RstEvent;
import com.proxy.netty.codec.frame.Http2PriorityEvent;
import com.proxy.netty.codec.frame.Http2PriorityHeadersEvent;
import com.proxy.netty.codec.frame.Http2DataEvent;
import com.proxy.netty.codec.frame.Http2Event;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DefaultHttp2HeadersDecoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.*;
import com.proxy.netty.codec.frame.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;
import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;
import static io.netty.handler.codec.http2.Http2CodecUtil.connectionPrefaceBuf;

/**
 * `Http2EventCodec` is a Netty channel handler that encodes and decodes
 * HTTP/2 frames and events. It handles the connection preface, frame reading,
 * frame writing, and conversion between Netty's HTTP/2 event model and the
 * underlying byte stream.
 */
public class Http2EventCodec extends ChannelDuplexHandler {

	private static final ByteBuf CONNECTION_PREFACE = unreleasableBuffer(connectionPrefaceBuf());
	private static final Cumulator prefaceCumulator = MERGE_CUMULATOR;
	private ByteBuf prefaceBuffer;
	private static final Cumulator frameDataCumulator = MERGE_CUMULATOR;
	private ByteBuf frameDataBuffer;

	private boolean expectPreface = true;

	private Http2FrameReader frameReader;
	private Http2FrameWriter frameWriter;

	/**
	 * Constructs a new `Http2EventCodec`. Initializes the frame reader and
	 * writer with appropriate configurations.
	 *
	 * @throws Http2Exception If an error occurs during initialization.
	 */
	public Http2EventCodec() throws Http2Exception {
		//TODO: fix HPACK - invalid max dynamic table size
		// we cannot get a HpackDecoder due to internal access constructor
		DefaultHttp2HeadersDecoder headersDecoder = new DefaultHttp2HeadersDecoder(true);
		headersDecoder.maxHeaderTableSize(40960);
		DefaultHttp2FrameReader frameReader = new DefaultHttp2FrameReader(headersDecoder);
		frameReader.maxFrameSize(0xffffff);
		this.frameReader = frameReader;
		DefaultHttp2FrameWriter frameWriter = new DefaultHttp2FrameWriter();
		//        frameWriter.maxFrameSize(0xffffff);
		this.frameWriter = frameWriter;
	}

	/**
	 * Writes an HTTP/2 event to the channel.  This method encodes the event
	 * into an HTTP/2 frame and writes it to the channel.
	 *
	 * @param ctx     The `ChannelHandlerContext`.
	 * @param msg     The HTTP/2 event to write.
	 * @param promise The `ChannelPromise` for the write operation.
	 */
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		if (!(msg instanceof Http2Event)) {
			// may be preface, or other message

			ctx.write(msg, promise);
			return;
		}

		if (msg instanceof Http2DataEvent) {
			Http2DataEvent event = (Http2DataEvent) msg;
			frameWriter.writeData(ctx, event.streamId(), event.data(), event.padding(), event.endOfStream(), promise);
		} else if (msg instanceof Http2HeadersEvent) {
			Http2HeadersEvent event = (Http2HeadersEvent) msg;

			frameWriter.writeHeaders(ctx, event.streamId(), event.headers(), event.padding(), event.endOfStream(),
					promise);
		} else if (msg instanceof Http2PriorityHeadersEvent) {
			Http2PriorityHeadersEvent event = (Http2PriorityHeadersEvent) msg;
			frameWriter.writeHeaders(ctx, event.streamId(), event.headers(), event.streamDependency(), event.weight(),
					event.exclusive(), event.padding(), event.endOfStream(), promise);
		} else if (msg instanceof Http2PriorityEvent) {
			Http2PriorityEvent event = (Http2PriorityEvent) msg;
			frameWriter.writePriority(ctx, event.streamId(), event.streamDependency(), event.weight(),
					event.exclusive(), promise);
		} else if (msg instanceof Http2RstEvent) {
			Http2RstEvent event = (Http2RstEvent) msg;
			frameWriter.writeRstStream(ctx, event.streamId(), event.errorCode(), promise);
		} else if (msg instanceof Http2SettingEvent) {
			Http2SettingEvent event = (Http2SettingEvent) msg;
			frameWriter.writeSettings(ctx, event.http2Settings(), promise);
		} else if (msg instanceof Http2SettingAckEvent) {
			frameWriter.writeSettingsAck(ctx, promise);
		} else if (msg instanceof Http2PingEvent) {
			Http2PingEvent event = (Http2PingEvent) msg;
			frameWriter.writePing(ctx, false, event.data(), promise);
		} else if (msg instanceof Http2PingAckEvent) {
			Http2PingAckEvent event = (Http2PingAckEvent) msg;
			frameWriter.writePing(ctx, true, event.data(), promise);
		} else if (msg instanceof Http2PushPromiseEvent) {
			Http2PushPromiseEvent event = (Http2PushPromiseEvent) msg;
			frameWriter.writePushPromise(ctx, event.streamId(), event.promisedStreamId(), event.headers(),
					event.padding(), promise);
		} else if (msg instanceof Http2GoAwayEvent) {
			Http2GoAwayEvent event = (Http2GoAwayEvent) msg;
			frameWriter.writeGoAway(ctx, event.lastStreamId(), event.errorCode(), event.debugData(), promise);
		} else if (msg instanceof Http2WindowUpdateEvent) {
			Http2WindowUpdateEvent event = (Http2WindowUpdateEvent) msg;
			frameWriter.writeWindowUpdate(ctx, event.streamId(), event.windowSizeIncrement(), promise);
		} else if (msg instanceof Http2UnknownEvent) {
			Http2UnknownEvent event = (Http2UnknownEvent) msg;
			frameWriter.writeFrame(ctx, event.frameType(), event.streamId(), event.flags(), event.payload(), promise);
		} else {
			// logger.error("unknown http2 event: {}", msg.getClass());
			ctx.close();
		}
	}

	/**
	 * Reads HTTP/2 frames from the channel. This method decodes the raw bytes
	 * into HTTP/2 frames and converts them into corresponding `Http2Event`
	 * objects, which are then passed up the pipeline.
	 *
	 * @param ctx The `ChannelHandlerContext`.
	 * @param msg The incoming message (should be a `ByteBuf`).
	 * @throws Http2Exception If an error occurs during frame reading or processing.
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Http2Exception {
		if (!(msg instanceof ByteBuf)) {
			ctx.fireChannelRead(msg);
			return;
		}
		ByteBuf in = (ByteBuf) msg;

		if (expectPreface) {
			if (ByteBufUtil.equals(CONNECTION_PREFACE, CONNECTION_PREFACE.readerIndex(), in, in.readerIndex(),
					Math.min(in.readableBytes(), CONNECTION_PREFACE.readableBytes()))) {
				if (prefaceBuffer != null) {
					in = prefaceCumulator.cumulate(ctx.alloc(), prefaceBuffer, in);
				}
				if (in.readableBytes() >= 24) {
					prefaceBuffer = null;
					ByteBuf prefaceBuf = in.retain().slice(in.readerIndex(), 24);
					in.skipBytes(24);

					ctx.fireChannelRead(prefaceBuf);
					expectPreface = false;
				} else {
					prefaceBuffer = in;
				}
			} else {
				expectPreface = false;
			}
		}

		if (frameDataBuffer != null) {
			in = frameDataCumulator.cumulate(ctx.alloc(), frameDataBuffer, in);
			frameDataBuffer = null;
		}

		frameReader.readFrame(ctx, in, new Http2FrameListener() {
			@Override
			public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
					boolean endOfStream) {
				int dataLen = data.readableBytes();
				Http2DataEvent http2Event = new Http2DataEvent(streamId, data.retain(), padding, endOfStream);
				onEventRead(ctx, http2Event);
				return dataLen + padding;
			}

			@Override
			public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
					boolean endOfStream) {
				Http2HeadersEvent http2Event = new Http2HeadersEvent(streamId, headers, padding, endOfStream);
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
					int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) {
				Http2PriorityHeadersEvent http2Event = new Http2PriorityHeadersEvent(streamId, headers, padding,
						endOfStream, streamDependency, weight, exclusive);
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight,
					boolean exclusive) {
				Http2PriorityEvent http2Event = new Http2PriorityEvent(streamId, streamDependency, weight, exclusive);
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
				Http2RstEvent http2Event = new Http2RstEvent(streamId, errorCode);
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onSettingsAckRead(ChannelHandlerContext ctx) {
				Http2SettingAckEvent http2Event = new Http2SettingAckEvent();
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
				Http2SettingEvent http2Event = new Http2SettingEvent(settings);
				onEventRead(ctx, http2Event);
			}

		/*	@Override
			public void onPingRead(ChannelHandlerContext ctx, long data) {
				Http2PingEvent http2Event = new Http2PingEvent(data);
				onEventRead(ctx, http2Event);
			}
			*/
			@Override
public void onPingRead(ChannelHandlerContext ctx, long data) {
	// Send ACK immediately
	//frameWriter.writePing(ctx, true, data, ctx.newPromise());
//	ctx.flush();

	// Still fire the event if needed
	Http2PingEvent http2Event = new Http2PingEvent(data);
	onEventRead(ctx, http2Event);
}

			@Override
			public void onPingAckRead(ChannelHandlerContext ctx, long data) {
				Http2PingAckEvent http2Event = new Http2PingAckEvent(data);
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
					Http2Headers headers, int padding) {
				Http2PushPromiseEvent http2Event = new Http2PushPromiseEvent(streamId, promisedStreamId, headers,
						padding);
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
				Http2GoAwayEvent http2Event = new Http2GoAwayEvent(lastStreamId, errorCode, debugData.retain());
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
				Http2WindowUpdateEvent http2Event = new Http2WindowUpdateEvent(streamId, windowSizeIncrement);
				onEventRead(ctx, http2Event);
			}

			@Override
			public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags,
					ByteBuf payload) {
				Http2UnknownEvent http2Event = new Http2UnknownEvent(frameType, streamId, flags, payload.retain());
				onEventRead(ctx, http2Event);
			}

			private void onEventRead(ChannelHandlerContext ctx, Http2Event http2Event) {

				ctx.fireChannelRead(http2Event);
				ctx.flush();
			}
		});

		if (in.readableBytes() > 0) {
			frameDataBuffer = in;
		} else {
			in.release();
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (prefaceBuffer != null) {
			prefaceBuffer.release();
			prefaceBuffer = null;
		}
		if (frameDataBuffer != null) {
			frameDataBuffer.release();
			frameDataBuffer = null;
		}
	}
}