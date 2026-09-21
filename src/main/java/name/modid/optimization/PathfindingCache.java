package name.modid.optimization;

import java.util.function.Supplier;

public final class PathfindingCache<K, V> {
	private final BoundedCache<K, V> paths;

	public PathfindingCache(int capacity) {
		paths = new BoundedCache<>(capacity);
	}

	public V getOrCompute(K key, Supplier<V> calculation) {
		return paths.getOrCompute(key, ignored -> calculation.get());
	}

	public void invalidate() {
		paths.clear();
	}
}
