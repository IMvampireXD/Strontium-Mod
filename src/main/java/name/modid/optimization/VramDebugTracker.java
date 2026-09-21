package name.modid.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public final class VramDebugTracker {
	private final ConcurrentMap<Long, Allocation> allocations = new ConcurrentHashMap<>();
	private final AtomicLong nextOffset = new AtomicLong();

	public long allocate(long size, String kind) {
		if (size < 0) {
			throw new IllegalArgumentException("size must not be negative");
		}
		long offset = nextOffset.getAndAdd(size);
		allocations.put(offset, new Allocation(size, kind));
		return offset;
	}

	public void release(long offset) {
		allocations.remove(offset);
	}

	public long allocatedBytes() {
		return allocations.values().stream().mapToLong(Allocation::size).sum();
	}

	public int allocationCount() {
		return allocations.size();
	}

	public String summary() {
		return allocatedBytes() / (1024 * 1024) + " MiB / " + allocationCount() + " GPU allocations";
	}

	private record Allocation(long size, String kind) {
	}
}
