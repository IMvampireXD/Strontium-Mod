package name.modid.optimization;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public final class PacketTrafficHandler extends ChannelDuplexHandler {
	private final PacketTrafficMetrics metrics;

	public PacketTrafficHandler(PacketTrafficMetrics metrics) {
		this.metrics = metrics;
	}

	@Override
	public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
		if (message instanceof ByteBuf buffer) {
			int bytes = buffer.readableBytes();
			metrics.recordInbound(message, bytes, bytes);
		}
		super.channelRead(context, message);
	}

	@Override
	public void write(ChannelHandlerContext context, Object message,
			io.netty.channel.ChannelPromise promise) throws Exception {
		if (message instanceof ByteBuf buffer) {
			int bytes = buffer.readableBytes();
			metrics.recordOutbound(message, bytes, bytes);
		}
		super.write(context, message, promise);
	}
}
