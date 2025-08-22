package com.proxy.netty.codec.handler;

import com.proxy.listener.SetLogger;
import com.proxy.netty.NettyUtils;
import com.proxy.listener.MessageListener;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.FutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import com.proxy.utils.HostPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.Promise;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.util.function.Supplier;
import io.netty.handler.codec.http.HttpRequest;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Handle http connect tunnel proxy request
 */
public class HttpConnectProxyInitializer extends TunnelProxyHandler<HttpRequest> {
    
    private boolean removed;

    public HttpConnectProxyInitializer(MessageListener messageListener, ServerSSLContextManager sslContextManager,
                                       Supplier<ProxyHandler> proxyHandlerSupplier) {
        super(messageListener, sslContextManager, proxyHandlerSupplier);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) {
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

        HostPort address = HostPort.parse(request.uri());
		
        bootstrap.connect(address.host(), address.ensurePort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_GATEWAY));
                NettyUtils.closeOnFlush(ctx.channel());
            }
        });

        promise.addListener((FutureListener<Channel>) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_GATEWAY));
                NettyUtils.closeOnFlush(ctx.channel());
                return;
            }

            Channel outboundChannel = future.getNow();
            ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, OK));
            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                
                //FIXME: throw NoSuchElementException
                if (removed) {
                    
                    ctx.close();
                    return;
                }
                ctx.pipeline().remove(HttpConnectProxyInitializer.this);
                ctx.pipeline().remove(HttpServerCodec.class);
                initTcpProxyHandlers(ctx, address, outboundChannel);
            });
        });
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        removed = true;
        
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        
        NettyUtils.closeOnFlush(ctx.channel());
    }
}