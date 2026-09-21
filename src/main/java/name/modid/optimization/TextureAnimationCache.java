package name.modid.optimization;

import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

/**
 * Keeps immutable animation frames available for GPU-side copy adapters,
 * avoiding repeated CPU-side conversion and upload preparation.
 */
public final class TextureAnimationCache {
	private final BoundedCache<String, byte[]> frames;
	private final LongAdder gpuCopies = new LongAdder();

	public TextureAnimationCache(int capacity) {
		frames = new BoundedCache<>(capacity);
	}

	public byte[] getOrStore(String key, byte[] pixels) {
		return frames.getOrCompute(key, ignored -> Arrays.copyOf(pixels, pixels.length));
	}

	public void recordGpuCopy() {
		gpuCopies.increment();
	}

	public long gpuCopies() {
		return gpuCopies.sum();
	}

	public void clear() {
		frames.clear();
		gpuCopies.reset();
	}
}
