package com.proxy.netty.codec.handler;

import com.proxy.netty.NettyUtils;
import com.proxy.listener.MessageListener;
import com.proxy.utils.HostPort;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.channel.Channel;
import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.concurrent.Promise;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.v5.Socks5Message;
import static io.netty.handler.codec.socksx.v5.Socks5CommandStatus.FAILURE;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import java.util.function.Supplier;

public class Socks5ProxyHandler extends TunnelProxyHandler<Socks5Message> {
    
    public Socks5ProxyHandler(MessageListener messageListener, ServerSSLContextManager sslContextManager,
                             Supplier<ProxyHandler> proxyHandlerSupplier) {
        super(messageListener, sslContextManager, proxyHandlerSupplier);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5Message socksRequest) {
        if (socksRequest instanceof Socks5InitialRequest) {
            ctx.pipeline().addFirst("socks5-command-decoder", new Socks5CommandRequestDecoder());
            ctx.pipeline().remove("socks5-initial-decoder");
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
            return;
        }
        if (socksRequest instanceof Socks5PasswordAuthRequest) {
            ctx.pipeline().addFirst("socks5-command-decoder", new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
            return;
        }
        if (!(socksRequest instanceof Socks5CommandRequest)) {
              NettyUtils.closeOnFlush(ctx.channel());
            return;
        }
        Socks5CommandRequest command = (Socks5CommandRequest) socksRequest;
        if (command.type() != Socks5CommandType.CONNECT) {
            // only support connect command
             
            NettyUtils.closeOnFlush(ctx.channel());
            return;
        }
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

        bootstrap.connect(command.dstAddr(), command.dstPort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(FAILURE, command.dstAddrType()));
                NettyUtils.closeOnFlush(ctx.channel());
            }
        });

        promise.addListener((FutureListener<Channel>) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(FAILURE, command.dstAddrType()));
                NettyUtils.closeOnFlush(ctx.channel());
                return;
            }
            Channel outboundChannel = future.getNow();
            ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS,
                    command.dstAddrType(),
                    command.dstAddr(),
                    command.dstPort()));

            responseFuture.addListener((ChannelFutureListener) f -> {
                ctx.pipeline().remove("socks5-server-encoder");
                ctx.pipeline().remove("socks5-command-decoder");
                ctx.pipeline().remove(Socks5ProxyHandler.this);
                HostPort address = HostPort.of(command.dstAddr(), command.dstPort());
                initTcpProxyHandlers(ctx, address, outboundChannel);
            });
        });
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        
        NettyUtils.closeOnFlush(ctx.channel());
    }
}