package com.proxy.listener;
import com.proxy.data.Message;
import io.netty.channel.ChannelId;

public interface MessageListener {
    void onMessage(Message message);
}