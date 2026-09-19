package name.modid.optimization;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Separates event polling/data preparation from render submission. The render
 * callback remains serialized on a dedicated thread and never touches GL from
 * worker code.
 */
public final class RenderThreadCoordinator implements AutoCloseable {
	private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Strontium-Render");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicBoolean running = new AtomicBoolean(true);

	public CompletableFuture<Void> submit(Runnable renderWork) {
		if (!running.get()) {
			throw new IllegalStateException("render coordinator is closed");
		}
		return CompletableFuture.runAsync(renderWork, renderExecutor);
	}

	@Override
	public void close() {
		if (running.compareAndSet(true, false)) {
			renderExecutor.shutdown();
		}
	}
}
