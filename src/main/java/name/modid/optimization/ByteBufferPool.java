package name.modid.optimization;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Reuses direct buffers used while packing chunk and UI data.
 */
public final class ByteBufferPool {
	private final ConcurrentLinkedQueue<ByteBuffer> available = new ConcurrentLinkedQueue<>();
	private final int bufferSize;
	private final int maximumBuffers;

	public ByteBufferPool(int bufferSize, int maximumBuffers) {
		if (bufferSize <= 0 || maximumBuffers <= 0) {
			throw new IllegalArgumentException("pool dimensions must be positive");
		}
		this.bufferSize = bufferSize;
		this.maximumBuffers = maximumBuffers;
	}

	public ByteBuffer acquire() {
		ByteBuffer buffer = available.poll();
		return buffer == null ? ByteBuffer.allocateDirect(bufferSize) : buffer;
	}

	public void release(ByteBuffer buffer) {
		if (buffer.capacity() != bufferSize || available.size() >= maximumBuffers) {
			return;
		}
		buffer.clear();
		available.offer(buffer);
	}

	public void clear() {
		available.clear();
	}
}
