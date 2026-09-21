package name.modid.optimization;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Low-overhead packet class counters for diagnostics. Class names are retained
 * as strings so this remains useful across Minecraft mapping changes.
 */
public final class PacketClassDiagnostics {
	private final Map<String, Counter> counters = new LinkedHashMap<>();

	public synchronized void record(Object packet, int transmittedBytes, int rawBytes) {
		String name = packet == null ? "null" : packet.getClass().getName();
		Counter counter = counters.computeIfAbsent(name, ignored -> new Counter());
		counter.packets++;
		counter.transmitted += Math.max(0, transmittedBytes);
		counter.raw += Math.max(0, rawBytes);
	}

	public synchronized Map<String, Statistics> snapshot() {
		Map<String, Statistics> result = new LinkedHashMap<>();
		counters.forEach((name, value) -> result.put(name,
				new Statistics(value.packets, value.transmitted, value.raw)));
		return Collections.unmodifiableMap(result);
	}

	public synchronized void clear() {
		counters.clear();
	}

	public record Statistics(long packets, long transmittedBytes, long rawBytes) { }

	private static final class Counter {
		private long packets;
		private long transmitted;
		private long raw;
	}
}
