package com.proxy.netty.codec.frame;

public abstract class Http2Event {
    // see Http2FrameTypes
    private final byte frameType;

    protected Http2Event(byte frameType) {
        this.frameType = frameType;
    }

    public byte frameType() {
        return frameType;
    }
}