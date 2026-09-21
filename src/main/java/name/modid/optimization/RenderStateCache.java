package name.modid.optimization;

import java.util.function.Supplier;

public final class RenderStateCache<K, V> {
	private final BoundedCache<K, V> values;

	public RenderStateCache(int capacity) {
		values = new BoundedCache<>(capacity);
	}

	public V getOrCompute(K key, Supplier<V> supplier) {
		return values.getOrCompute(key, ignored -> supplier.get());
	}

	public void invalidate() {
		values.clear();
	}
}
