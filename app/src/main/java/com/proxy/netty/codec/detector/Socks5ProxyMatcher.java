package com.proxy.netty.codec.detector;
import com.proxy.netty.codec.handler.ServerSSLContextManager;
import com.proxy.listener.MessageListener;
import com.proxy.netty.codec.handler.Socks5ProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import java.util.function.Supplier;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
;
import io.netty.channel.ChannelPipeline;

import io.netty.buffer.ByteBuf;

public class Socks5ProxyMatcher extends ProtocolMatcher {
    
    private final MessageListener messageListener;
    private final ServerSSLContextManager sslContextManager;
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public Socks5ProxyMatcher(MessageListener messageListener, ServerSSLContextManager sslContextManager,
                              Supplier<ProxyHandler> proxyHandlerSupplier) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
        this.proxyHandlerSupplier = proxyHandlerSupplier;
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            return PENDING;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        if (first == 5) {
            return MATCH;
        }
        return MISMATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        pipeline.addLast("socks5-server-encoder", Socks5ServerEncoder.DEFAULT);
        pipeline.addLast("socks5-initial-decoder", new Socks5InitialRequestDecoder());
        pipeline.addLast(new Socks5ProxyHandler(messageListener, sslContextManager, proxyHandlerSupplier));
    }
}