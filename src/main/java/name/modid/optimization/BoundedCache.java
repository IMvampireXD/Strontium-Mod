package name.modid.optimization;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Small allocation-conscious LRU cache for hot, repeatable lookups.
 */
public final class BoundedCache<K, V> {
	private final Map<K, V> values;

	public BoundedCache(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.values = new LinkedHashMap<>(capacity, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > capacity;
			}
		};
	}

	public synchronized V getOrCompute(K key, Function<? super K, ? extends V> factory) {
		V value = values.get(key);
		if (value != null) {
			return value;
		}
		value = factory.apply(key);
		values.put(key, value);
		return value;
	}

	public synchronized void clear() {
		values.clear();
	}

	public synchronized int size() {
		return values.size();
	}
}
