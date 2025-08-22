package com.proxy.netty.codec.frame;
import io.netty.handler.codec.http2.Http2FrameTypes;

public class Http2SettingAckEvent extends Http2Event {

    public Http2SettingAckEvent() {
        super(Http2FrameTypes.SETTINGS);
    }
}