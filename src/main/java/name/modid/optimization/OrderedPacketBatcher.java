package name.modid.optimization;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class OrderedPacketBatcher {
	private static final long FLUSH_NANOS = 20_000_000L;
	private final List<byte[]> packets = new ArrayList<>();
	private long openedAt = -1L;

	public synchronized void add(byte[] packet, boolean protocolBoundary, long nowNanos) {
		if (protocolBoundary && !packets.isEmpty()) {
			flush();
		}
		if (openedAt < 0) {
			openedAt = nowNanos;
		}
		packets.add(packet.clone());
		if (protocolBoundary || nowNanos - openedAt >= FLUSH_NANOS) {
			flush();
		}
	}

	public synchronized void add(byte[] packet, boolean protocolBoundary) {
		add(packet, protocolBoundary, System.nanoTime());
	}

	public synchronized boolean isEmpty() {
		return packets.isEmpty();
	}

	public synchronized byte[] flush() {
		if (packets.isEmpty()) {
			openedAt = -1L;
			return new byte[0];
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		for (byte[] packet : packets) {
			output.write((packet.length >>> 24) & 0xff);
			output.write((packet.length >>> 16) & 0xff);
			output.write((packet.length >>> 8) & 0xff);
			output.write(packet.length & 0xff);
			output.writeBytes(packet);
		}
		packets.clear();
		openedAt = -1L;
		return output.toByteArray();
	}
}
