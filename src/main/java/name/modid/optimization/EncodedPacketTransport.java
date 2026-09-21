package name.modid.optimization;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stateful opt-in transport facade. It does not alter packets until
 * {@link #setNegotiated(boolean)} is called, which makes it safe to install on
 * vanilla connections.
 */
public final class EncodedPacketTransport {
	private final FramedPacketCodec codec;
	private final long epoch;
	private final AtomicLong sequence = new AtomicLong();
	private volatile boolean negotiated;

	public EncodedPacketTransport() {
		this(2 * 1024 * 1024, 32);
	}

	public EncodedPacketTransport(int maximumPayload, long maximumResyncGap) {
		codec = new FramedPacketCodec(maximumPayload, maximumResyncGap);
		epoch = Integer.toUnsignedLong((int) System.nanoTime());
	}

	public void setNegotiated(boolean negotiated) {
		this.negotiated = negotiated;
	}

	public boolean isNegotiated() {
		return negotiated;
	}

	public byte[] encode(byte[] payload, boolean protocolBoundary) {
		if (!negotiated) {
			return payload.clone();
		}
		return codec.encode(epoch, sequence.getAndIncrement(), payload, protocolBoundary);
	}

	public FramedPacketCodec.DecodeAttempt decode(byte[] bytes, int offset, int length) {
		if (!negotiated) {
			return new FramedPacketCodec.DecodeAttempt(length, Arrays.copyOfRange(bytes, offset, offset + length),
					false, false);
		}
		return codec.decodeNext(bytes, offset, length);
	}

	public FramedPacketCodec codec() {
		return codec;
	}
}
