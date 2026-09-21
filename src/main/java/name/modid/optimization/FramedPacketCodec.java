package name.modid.optimization;

import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Versioned frame codec. Frames are self-delimiting and carry both a length
 * bound and a checksum so malformed or corrupted input can be discarded
 * without taking down a connection.
 */
public final class FramedPacketCodec {
	public static final int MAGIC = 0x5354;
	public static final int VERSION = 1;
	public static final int HEADER_SIZE = 28;
	public static final int FLAG_COMPRESSED = 1;
	public static final int FLAG_PROTOCOL_BOUNDARY = 2;

	private final int maximumPayload;
	private final SequenceEpochValidator sequences;

	public FramedPacketCodec(int maximumPayload, long maximumResyncGap) {
		if (maximumPayload < 1) {
			throw new IllegalArgumentException("maximumPayload must be positive");
		}
		this.maximumPayload = maximumPayload;
		this.sequences = new SequenceEpochValidator(maximumResyncGap);
	}

	public byte[] encode(long epoch, long sequence, byte[] raw, boolean protocolBoundary) {
		if (epoch < 0 || epoch > 0xffff_ffffL || sequence < 0
				|| raw == null || raw.length > maximumPayload) {
			throw new IllegalArgumentException("payload exceeds transport limit");
		}
		byte[] compressed = PacketCompression.compress(raw);
		boolean useCompression = compressed.length < raw.length;
		byte[] payload = useCompression ? compressed : raw;
		int flags = (useCompression ? FLAG_COMPRESSED : 0)
				| (protocolBoundary ? FLAG_PROTOCOL_BOUNDARY : 0);
		byte[] frame = new byte[HEADER_SIZE + payload.length];
		writeShort(frame, 0, MAGIC);
		frame[2] = VERSION;
		frame[3] = (byte) flags;
		writeInt(frame, 4, (int) epoch);
		writeLong(frame, 8, sequence);
		writeInt(frame, 16, raw.length);
		writeInt(frame, 20, payload.length);
		writeInt(frame, 24, checksum(raw));
		System.arraycopy(payload, 0, frame, HEADER_SIZE, payload.length);
		return frame;
	}

	/**
	 * Decodes one frame from a byte array. Incomplete input returns an attempt
	 * with consumed=0; invalid leading bytes are skipped up to the next magic.
	 */
	public synchronized DecodeAttempt decodeNext(byte[] input, int offset, int length) {
		if (input == null || offset < 0 || length < 0 || offset > input.length - length) {
			throw new IllegalArgumentException("invalid input range");
		}
		int end = offset + length;
		int start = findMagic(input, offset, end);
		if (start < 0) {
			return new DecodeAttempt(0, null, false, false);
		}
		if (end - start < HEADER_SIZE) {
			return new DecodeAttempt(start - offset, null, false, false);
		}
		if ((input[start + 2] & 0xff) != VERSION) {
			return new DecodeAttempt(start - offset + 2, null, false, true);
		}
		int flags = input[start + 3] & 0xff;
		if ((flags & ~(FLAG_COMPRESSED | FLAG_PROTOCOL_BOUNDARY)) != 0) {
			return new DecodeAttempt(start - offset + 2, null, false, true);
		}
		long epoch = Integer.toUnsignedLong(readInt(input, start + 4));
		long sequence = readLong(input, start + 8);
		int rawLength = readInt(input, start + 16);
		int payloadLength = readInt(input, start + 20);
		if (rawLength < 0 || rawLength > maximumPayload || payloadLength < 0
				|| payloadLength > maximumPayload || payloadLength > end - start - HEADER_SIZE) {
			return new DecodeAttempt(start - offset + 2, null, false, true);
		}
		int frameLength = HEADER_SIZE + payloadLength;
		if (end - start < frameLength) {
			return new DecodeAttempt(start - offset, null, false, false);
		}
		byte[] encodedPayload = Arrays.copyOfRange(input, start + HEADER_SIZE, start + frameLength);
		byte[] raw;
		try {
			raw = (flags & FLAG_COMPRESSED) != 0
					? PacketCompression.decompress(encodedPayload, rawLength, maximumPayload)
					: encodedPayload;
		} catch (RuntimeException exception) {
			return new DecodeAttempt(start - offset + frameLength, null, false, true);
		}
		if (raw.length != rawLength || checksum(raw) != readInt(input, start + 24)
				|| sequences.validate(epoch, sequence) == SequenceEpochValidator.Result.REJECTED) {
			return new DecodeAttempt(start - offset + frameLength, null, false, true);
		}
		return new DecodeAttempt(start - offset + frameLength, raw,
				(flags & FLAG_PROTOCOL_BOUNDARY) != 0, false);
	}

	public int maximumPayload() {
		return maximumPayload;
	}

	private static int findMagic(byte[] input, int offset, int end) {
		for (int index = offset; index + 1 < end; index++) {
			if ((input[index] & 0xff) == (MAGIC >>> 8) && (input[index + 1] & 0xff) == (MAGIC & 0xff)) {
				return index;
			}
		}
		return -1;
	}

	private static int checksum(byte[] bytes) {
		CRC32 crc = new CRC32();
		crc.update(bytes);
		return (int) crc.getValue();
	}

	private static void writeShort(byte[] target, int offset, int value) {
		target[offset] = (byte) (value >>> 8);
		target[offset + 1] = (byte) value;
	}

	private static void writeInt(byte[] target, int offset, int value) {
		target[offset] = (byte) (value >>> 24);
		target[offset + 1] = (byte) (value >>> 16);
		target[offset + 2] = (byte) (value >>> 8);
		target[offset + 3] = (byte) value;
	}

	private static void writeLong(byte[] target, int offset, long value) {
		for (int index = 7; index >= 0; index--) {
			target[offset + 7 - index] = (byte) (value >>> (index * 8));
		}
	}

	private static int readInt(byte[] source, int offset) {
		return ((source[offset] & 0xff) << 24) | ((source[offset + 1] & 0xff) << 16)
				| ((source[offset + 2] & 0xff) << 8) | (source[offset + 3] & 0xff);
	}

	private static long readLong(byte[] source, int offset) {
		long value = 0;
		for (int index = 0; index < 8; index++) {
			value = (value << 8) | (source[offset + index] & 0xffL);
		}
		return value;
	}

	public record DecodeAttempt(int consumed, byte[] payload, boolean protocolBoundary, boolean invalid) {
		public DecodeAttempt {
			if (consumed < 0) {
				throw new IllegalArgumentException("consumed must not be negative");
			}
			if (payload != null) {
				payload = payload.clone();
			}
		}

		@Override
		public byte[] payload() {
			return payload == null ? null : payload.clone();
		}
	}
}
