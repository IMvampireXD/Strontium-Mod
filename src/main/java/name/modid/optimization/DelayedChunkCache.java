package name.modid.optimization;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DelayedChunkCache {
	private final int maxEntries;
	private final long timeoutNanos;
	private final double maxDistanceSquared;
	private final Map<Long, Entry> entries;

	public DelayedChunkCache(int maxEntries, double distance, long timeoutMillis) {
		if (maxEntries < 1 || distance < 0 || timeoutMillis < 1) {
			throw new IllegalArgumentException("invalid delayed chunk cache settings");
		}
		this.maxEntries = maxEntries;
		this.maxDistanceSquared = distance * distance;
		this.timeoutNanos = timeoutMillis * 1_000_000L;
		this.entries = new LinkedHashMap<>(maxEntries, 0.75f, true);
	}

	public synchronized void retain(long chunkKey, double x, double z, long nowNanos, byte[] packet) {
		entries.put(chunkKey, new Entry(x, z, nowNanos, packet.clone()));
		while (entries.size() > maxEntries) {
			entries.remove(entries.keySet().iterator().next());
		}
	}

	public synchronized byte[] reuse(long chunkKey, double x, double z, long nowNanos) {
		Entry entry = entries.get(chunkKey);
		if (entry == null || nowNanos - entry.createdNanos() > timeoutNanos
				|| squaredDistance(entry.x(), entry.z(), x, z) > maxDistanceSquared) {
			if (entry != null) {
				entries.remove(chunkKey);
			}
			return null;
		}
		return entry.packet().clone();
	}

	public synchronized void clear() {
		entries.clear();
	}

	private static double squaredDistance(double x1, double z1, double x2, double z2) {
		double dx = x1 - x2;
		double dz = z1 - z2;
		return dx * dx + dz * dz;
	}

	private record Entry(double x, double z, long createdNanos, byte[] packet) {
	}
}
