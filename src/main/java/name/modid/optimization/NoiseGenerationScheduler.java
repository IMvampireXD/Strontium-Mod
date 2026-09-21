package name.modid.optimization;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Parallel scheduler for deterministic, side-effect-free noise columns. World
 * mutation and chunk publication must remain on the owning server thread.
 */
public final class NoiseGenerationScheduler {
	private final ChunkUpdateScheduler workers;

	public NoiseGenerationScheduler(ChunkUpdateScheduler workers) {
		this.workers = workers;
	}

	public <T> CompletableFuture<T> submit(Supplier<T> noiseCalculation) {
		return workers.submit(noiseCalculation);
	}

	public int queuedTasks() {
		return workers.queuedTasks();
	}
}
