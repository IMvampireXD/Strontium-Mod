package name.modid.optimization;

import java.nio.ByteBuffer;

public final class StrontiumOptimizations implements AutoCloseable {
	public static final int CHUNK_BUFFER_SIZE = 256 * 1024;
	private final ByteBufferPool bufferPool = new ByteBufferPool(CHUNK_BUFFER_SIZE, 64);
	private final ChunkUpdateScheduler chunkScheduler = new ChunkUpdateScheduler();
	private final BoundedCache<Long, Boolean> visibilityCache = new BoundedCache<>(4096);
	private final TextureAnimationCache textureAnimations = new TextureAnimationCache(1024);
	private final BackgroundThrottle backgroundThrottle = new BackgroundThrottle();

	public ByteBuffer acquireChunkBuffer() {
		return bufferPool.acquire();
	}

	public void releaseChunkBuffer(ByteBuffer buffer) {
		bufferPool.release(buffer);
	}

	public ChunkUpdateScheduler chunks() {
		return chunkScheduler;
	}

	public TextureAnimationCache textureAnimations() {
		return textureAnimations;
	}

	public BackgroundThrottle backgroundThrottle() {
		return backgroundThrottle;
	}

	public boolean cachedVisibility(long key, java.util.function.BooleanSupplier calculation) {
		return visibilityCache.getOrCompute(key, ignored -> calculation.getAsBoolean());
	}

	public void clearFrameCaches() {
		visibilityCache.clear();
	}

	@Override
	public void close() {
		chunkScheduler.close();
		bufferPool.clear();
		visibilityCache.clear();
		textureAnimations.clear();
	}
}
