package name.modid.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public final class StringDeduplicator {
	private final ConcurrentMap<String, String> values = new ConcurrentHashMap<>();
	private final LongAdder processed = new LongAdder();
	private final LongAdder deduplicated = new LongAdder();

	public String deduplicateLowercase(String value) {
		if (value == null || !value.equals(value.toLowerCase(java.util.Locale.ROOT))) {
			return value;
		}
		processed.increment();
		String canonical = values.putIfAbsent(value, value);
		if (canonical != null) {
			deduplicated.increment();
			return canonical;
		}
		return value;
	}

	public long processed() {
		return processed.sum();
	}

	public long unique() {
		return values.size();
	}

	public long deduplicated() {
		return deduplicated.sum();
	}

	public void clear() {
		values.clear();
		processed.reset();
		deduplicated.reset();
	}
}
