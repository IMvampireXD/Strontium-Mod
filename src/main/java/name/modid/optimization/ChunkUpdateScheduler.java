package name.modid.optimization;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Bounded worker queue for CPU-side chunk preparation. OpenGL work must remain on
 * the render thread; callers should only submit pure data preparation here.
 */
public final class ChunkUpdateScheduler implements AutoCloseable {
	private final ThreadPoolExecutor executor;

	public ChunkUpdateScheduler() {
		int workers = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
		executor = new ThreadPoolExecutor(
				workers,
				workers,
				30L,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(Math.max(32, workers * 8)),
				runnable -> {
					Thread thread = new Thread(runnable, "Strontium-ChunkWorker");
					thread.setDaemon(true);
					return thread;
				},
				new ThreadPoolExecutor.CallerRunsPolicy()
		);
		executor.allowCoreThreadTimeOut(true);
	}

	public <T> CompletableFuture<T> submit(Supplier<T> work) {
		CompletableFuture<T> result = new CompletableFuture<>();
		executor.execute(() -> {
			try {
				result.complete(work.get());
			} catch (Throwable throwable) {
				result.completeExceptionally(throwable);
			}
		});
		return result;
	}

	public int queuedTasks() {
		return executor.getQueue().size();
	}

	@Override
	public void close() {
		executor.shutdown();
	}
}
