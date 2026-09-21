package name.modid.optimization;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Small, dependency-free compression adapter used by the encoded transport.
 * A Zstd implementation can be supplied by a future adapter without changing
 * the wire format. The JDK implementation is deliberately conservative.
 */
public final class PacketCompression {
	private PacketCompression() {
	}

	public static byte[] compress(byte[] input) {
		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
		try {
			deflater.setInput(input);
			deflater.finish();
			ByteArrayOutputStream output = new ByteArrayOutputStream(input.length);
			byte[] buffer = new byte[Math.min(8192, Math.max(256, input.length))];
			while (!deflater.finished()) {
				int count = deflater.deflate(buffer);
				if (count == 0 && deflater.needsInput()) {
					break;
				}
				output.write(buffer, 0, count);
			}
			return output.toByteArray();
		} finally {
			deflater.end();
		}
	}

	public static byte[] decompress(byte[] input, int expectedLength, int maximumLength) {
		if (expectedLength < 0 || expectedLength > maximumLength) {
			throw new IllegalArgumentException("invalid decompressed length");
		}
		Inflater inflater = new Inflater(false);
		try {
			inflater.setInput(input);
			ByteArrayOutputStream output = new ByteArrayOutputStream(expectedLength);
			byte[] buffer = new byte[Math.min(8192, Math.max(256, expectedLength))];
			while (!inflater.finished()) {
				int count;
				try {
					count = inflater.inflate(buffer);
				} catch (DataFormatException exception) {
					throw new IllegalArgumentException("invalid compressed packet", exception);
				}
				if (count > 0) {
					if (output.size() + count > maximumLength) {
						throw new IllegalArgumentException("decompressed packet exceeds limit");
					}
					output.write(buffer, 0, count);
				} else if (inflater.needsDictionary() || inflater.needsInput()) {
					throw new IllegalArgumentException("truncated compressed packet");
				}
			}
			byte[] result = output.toByteArray();
			if (result.length != expectedLength) {
				throw new IllegalArgumentException("decompressed length mismatch");
			}
			return result;
		} finally {
			inflater.end();
		}
	}
}
