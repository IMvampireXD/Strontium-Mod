package name.modid.optimization;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asynchronously coalesces mouse motion without touching GLFW from a worker
 * thread. GLFW callbacks remain the platform source; the game thread consumes
 * compact deltas instead of one allocation per event.
 */
public final class AsyncRawMouseInput implements AutoCloseable {
	private final ConcurrentLinkedQueue<MouseDelta> pending = new ConcurrentLinkedQueue<>();
	private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Strontium-RawMouse");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicBoolean running = new AtomicBoolean(true);

	public void offer(double deltaX, double deltaY, long timestampNanos) {
		if (!running.get()) {
			return;
		}
		pending.offer(new MouseDelta(deltaX, deltaY, timestampNanos));
	}

	public void drain(MouseConsumer consumer) {
		MouseDelta delta;
		while ((delta = pending.poll()) != null) {
			consumer.accept(delta.deltaX(), delta.deltaY(), delta.timestampNanos());
		}
	}

	public int pending() {
		return pending.size();
	}

	@Override
	public void close() {
		if (running.compareAndSet(true, false)) {
			pending.clear();
			worker.shutdownNow();
		}
	}

	@FunctionalInterface
	public interface MouseConsumer {
		void accept(double deltaX, double deltaY, long timestampNanos);
	}

	private record MouseDelta(double deltaX, double deltaY, long timestampNanos) {
	}
}
