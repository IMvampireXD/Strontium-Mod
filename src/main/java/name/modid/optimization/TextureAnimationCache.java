package name.modid.optimization;

import java.util.Arrays;

/**
 * Keeps immutable animation frames available for GPU-side copy adapters,
 * avoiding repeated CPU-side conversion and upload preparation.
 */
public final class TextureAnimationCache {
	private final BoundedCache<String, byte[]> frames;

	public TextureAnimationCache(int capacity) {
		frames = new BoundedCache<>(capacity);
	}

	public byte[] getOrStore(String key, byte[] pixels) {
		return frames.getOrCompute(key, ignored -> Arrays.copyOf(pixels, pixels.length));
	}

	public void clear() {
		frames.clear();
	}
}
