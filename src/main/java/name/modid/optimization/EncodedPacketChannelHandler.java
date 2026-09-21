package name.modid.optimization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.io.ByteArrayOutputStream;

/**
 * Netty adapter for {@link EncodedPacketTransport}. It starts as a pure
 * pass-through and only consumes ByteBufs after an explicit two-sided
 * negotiation. Non-ByteBuf Minecraft packet objects are always passed on.
 */
public final class EncodedPacketChannelHandler extends ChannelDuplexHandler {
	public static final String NAME = "strontium_encoded_transport";
	private final EncodedPacketTransport transport;
	private final ByteArrayOutputStream inbound = new ByteArrayOutputStream();
	private volatile boolean localNegotiated;
	private volatile boolean peerNegotiated;

	public EncodedPacketChannelHandler(EncodedPacketTransport transport) {
		this.transport = transport;
	}

	public void negotiate(boolean local, boolean peer) {
		localNegotiated = local;
		peerNegotiated = peer;
		transport.setNegotiated(local && peer);
		if (!local || !peer) {
			inbound.reset();
		}
	}

	public boolean isNegotiated() {
		return localNegotiated && peerNegotiated && transport.isNegotiated();
	}

	public EncodedPacketTransport transport() {
		return transport;
	}

	/**
	 * Convenience hook for integrations that complete negotiation after the
	 * connection pipeline has been created.
	 */
	public static boolean negotiate(ChannelPipeline pipeline, boolean local, boolean peer) {
		if (pipeline.get(NAME) instanceof EncodedPacketChannelHandler handler) {
			handler.negotiate(local, peer);
			return true;
		}
		return false;
	}

	@Override
	public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
		if (!isNegotiated() || !(message instanceof ByteBuf buffer)) {
			super.channelRead(context, message);
			return;
		}
		try {
			byte[] bytes = new byte[buffer.readableBytes()];
			buffer.getBytes(buffer.readerIndex(), bytes);
			appendInbound(context, bytes);
		} finally {
			ReferenceCountUtil.release(message);
		}
	}

	@Override
	public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
		if (!isNegotiated() || !(message instanceof ByteBuf buffer)) {
			super.write(context, message, promise);
			return;
		}
		byte[] bytes = new byte[buffer.readableBytes()];
		buffer.getBytes(buffer.readerIndex(), bytes);
		byte[] encoded;
		try {
			encoded = transport.encode(bytes, false);
		} catch (IllegalArgumentException exception) {
			// A packet larger than the negotiated bound remains vanilla.
			super.write(context, message, promise);
			return;
		}
		ReferenceCountUtil.release(message);
		context.write(Unpooled.wrappedBuffer(encoded), promise);
	}

	private void appendInbound(ChannelHandlerContext context, byte[] bytes) {
		if (inbound.size() + bytes.length > transport.codec().maximumPayload()
				+ FramedPacketCodec.HEADER_SIZE * 2) {
			inbound.reset();
			context.fireChannelRead(Unpooled.wrappedBuffer(bytes));
			return;
		}
		inbound.writeBytes(bytes);
		while (inbound.size() > 0) {
			byte[] available = inbound.toByteArray();
			FramedPacketCodec.DecodeAttempt attempt = transport.decode(available, 0, available.length);
			if (attempt.consumed() == 0) {
				if (attempt.invalid()) {
					inbound.reset();
					context.fireChannelRead(Unpooled.wrappedBuffer(available));
				}
				break;
			}
			byte[] remainder = available;
			inbound.reset();
			if (attempt.consumed() < remainder.length) {
				inbound.write(remainder, attempt.consumed(), remainder.length - attempt.consumed());
			}
			if (attempt.payload() != null) {
				context.fireChannelRead(Unpooled.wrappedBuffer(attempt.payload()));
			}
			if (!attempt.invalid() && attempt.consumed() == 0) break;
		}
	}
}
