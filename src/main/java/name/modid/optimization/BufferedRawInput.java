package name.modid.optimization;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Batch-oriented input queue. Platform integrations enqueue decoded events
 * once, then the game thread drains the batch without JNI/reflection calls.
 */
public final class BufferedRawInput<T> {
	private final ConcurrentLinkedQueue<T> events = new ConcurrentLinkedQueue<>();

	public void offer(T event) {
		events.offer(event);
	}

	public int drainTo(Consumer<? super T> consumer) {
		int count = 0;
		T event;
		while ((event = events.poll()) != null) {
			consumer.accept(event);
			count++;
		}
		return count;
	}

	public int pending() {
		return events.size();
	}
}
