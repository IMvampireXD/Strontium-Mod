package name.modid.optimization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Negotiated packet indexes. The wire prefix is one version byte plus a
 * two-byte channel index, so unknown indexes can safely fall back to vanilla.
 */
public final class CompactPacketRegistry {
	private final Map<String, Integer> indexes = new ConcurrentHashMap<>();
	private final Map<Integer, String> names = new ConcurrentHashMap<>();

	public boolean register(String channel, int index) {
		if (index < 0 || index >= 4096 || channel == null) {
			return false;
		}
		if (indexes.putIfAbsent(channel, index) != null) {
			return false;
		}
		names.putIfAbsent(index, channel);
		return true;
	}

	public byte[] header(String channel) {
		Integer index = indexes.get(channel);
		if (index == null) {
			return null;
		}
		return new byte[] { 1, (byte) (index >>> 8), (byte) index.intValue() };
	}

	public String channel(byte[] header) {
		if (header == null || header.length != 3 || header[0] != 1) {
			return null;
		}
		return names.get(((header[1] & 0xff) << 8) | (header[2] & 0xff));
	}
}
