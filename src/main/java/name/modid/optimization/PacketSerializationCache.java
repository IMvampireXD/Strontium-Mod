package name.modid.optimization;

import java.util.Arrays;

/**
 * Reusable packet-sized byte storage for callers that serialize immutable
 * packet fields repeatedly. The returned copy is isolated from the cache.
 */
public final class PacketSerializationCache {
	private byte[] storage = new byte[256];

	public byte[] copyOf(byte[] encoded) {
		if (encoded.length > storage.length) {
			storage = new byte[Math.max(encoded.length, storage.length * 2)];
		}
		System.arraycopy(encoded, 0, storage, 0, encoded.length);
		return Arrays.copyOf(storage, encoded.length);
	}

	public void clear() {
		storage = new byte[256];
	}
}
