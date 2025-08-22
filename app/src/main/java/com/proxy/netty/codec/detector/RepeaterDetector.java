package com.proxy.netty.codec.detector;
import io.netty.buffer.ByteBuf;

public class RepeaterDetector extends ProtocolMatcher {
	@Override
	protected int match(ByteBuf buf) {
	    return 0;
	}
	
}