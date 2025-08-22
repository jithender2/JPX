package com.proxy.netty.codec.detector;
import io.netty.channel.ChannelPipeline;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.function.Consumer;

public class AnyMatcher extends ProtocolMatcher {
    private Consumer<ChannelPipeline> consumer;

    public AnyMatcher(Consumer<ChannelPipeline> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public int match(ByteBuf buf) {
        return MATCH;
    }

    @Override
    protected void handlePipeline(ChannelPipeline pipeline) {
        consumer.accept(pipeline);
    }
}