package name.modid.optimization;

import java.util.ArrayDeque;
import java.util.function.IntFunction;

/**
 * Bounded pool for integer-backed VBO/EBO/FBO handles. Deletion is owned by the
 * handle factory and can therefore be performed on the render thread.
 */
public final class ResourcePool {
	private final ArrayDeque<Integer> free = new ArrayDeque<>();
	private final int capacity;
	private final IntFunction<Integer> create;
	private int created;

	public ResourcePool(int capacity, IntFunction<Integer> create) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.capacity = capacity;
		this.create = create;
	}

	public int acquire() {
		Integer handle = free.pollFirst();
		if (handle != null) {
			return handle;
		}
		if (created >= capacity) {
			throw new IllegalStateException("render resource pool exhausted");
		}
		return create.apply(created++);
	}

	public void release(int handle) {
		if (free.size() < capacity) {
			free.offerFirst(handle);
		}
	}

	public int available() {
		return free.size();
	}
}
