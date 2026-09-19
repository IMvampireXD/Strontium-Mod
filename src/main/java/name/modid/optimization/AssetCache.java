package name.modid.optimization;

import java.util.Arrays;

/**
 * Bounded cache for decoded asset bytes. It avoids repeated filesystem reads
 * while keeping memory usage predictable.
 */
public final class AssetCache {
	private final BoundedCache<String, byte[]> entries;

	public AssetCache(int capacity) {
		entries = new BoundedCache<>(capacity);
	}

	public byte[] getOrLoad(String key, java.util.function.Supplier<byte[]> loader) {
		return entries.getOrCompute(key, ignored -> {
			byte[] bytes = loader.get();
			return Arrays.copyOf(bytes, bytes.length);
		});
	}

	public void clear() {
		entries.clear();
	}
}
