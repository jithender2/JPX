package com.proxy.netty.codec.frame;
import io.netty.handler.codec.http2.Http2FrameTypes;

public class Http2PingAckEvent extends Http2Event {
	private final long data;
	
	public Http2PingAckEvent(long data) {
		super(Http2FrameTypes.PING);
		this.data = data;
	}
	
	public long data() {
		return data;
	}
}