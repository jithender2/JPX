package com.proxy.netty;
import io.netty.channel.ChannelFutureListener;
import java.nio.channels.ClosedChannelException;
import java.io.IOException;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class NettyUtils {

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel channel) {
        if (channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * If the exception is caused by unexpected client close.
     */
    public static boolean causedByClientClose(Throwable e) {
        if (e instanceof IOException) {
            String message = e.getMessage();
            if (message != null && message.contains("Connection reset by peer")) {
                return true;
            }
        }
        if (e instanceof ClosedChannelException) {
            // this should be avoid?
            return true;
        }
        return false;
    }
}