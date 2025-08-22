package com.proxy.netty.codec.frame;

public abstract class Http2StreamEvent extends Http2Event {

    private final int streamId;

    public Http2StreamEvent(byte frameType, int streamId) {
        super(frameType);
        this.streamId = streamId;
    }

    public int streamId() {
        return streamId;
    }
}