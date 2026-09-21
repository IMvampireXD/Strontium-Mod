package name.modid.optimization;

import java.nio.ByteBuffer;

/**
 * Reusable direct staging storage for texture uploads. The caller owns the GL
 * upload and can bind this buffer to a PBO on the render thread.
 */
public final class FastTextureUpload {
	private ByteBuffer staging;

	public FastTextureUpload(int initialCapacity) {
		if (initialCapacity < 1) {
			throw new IllegalArgumentException("initialCapacity must be positive");
		}
		staging = ByteBuffer.allocateDirect(initialCapacity);
	}

	public ByteBuffer prepare(byte[] pixels) {
		ensureCapacity(pixels.length);
		staging.clear();
		staging.put(pixels).flip();
		return staging;
	}

	public ByteBuffer prepare(ByteBuffer pixels) {
		ensureCapacity(pixels.remaining());
		staging.clear();
		staging.put(pixels.duplicate()).flip();
		return staging;
	}

	public void clear() {
		staging = ByteBuffer.allocateDirect(1);
	}

	private void ensureCapacity(int required) {
		if (required <= staging.capacity()) {
			return;
		}
		int capacity = staging.capacity();
		while (capacity < required) {
			capacity = Math.max(required, capacity * 2);
		}
		staging = ByteBuffer.allocateDirect(capacity);
	}
}
