package name.modid.optimization;

import java.util.Arrays;

/**
 * Collects immediate-mode vertices into one reusable primitive buffer. A
 * renderer adapter can consume the vertices in one draw call.
 */
public final class ImmediateBatch {
	private float[] vertices;
	private int size;

	public ImmediateBatch(int initialFloats) {
		if (initialFloats < 1) {
			throw new IllegalArgumentException("initialFloats must be positive");
		}
		vertices = new float[initialFloats];
	}

	public void vertex(float x, float y, float z, float u, float v, int color) {
		ensureCapacity(size + 6);
		vertices[size++] = x;
		vertices[size++] = y;
		vertices[size++] = z;
		vertices[size++] = u;
		vertices[size++] = v;
		vertices[size++] = Float.intBitsToFloat(color);
	}

	public int floatCount() {
		return size;
	}

	public float[] data() {
		return vertices;
	}

	public void clear() {
		size = 0;
	}

	private void ensureCapacity(int required) {
		if (required <= vertices.length) {
			return;
		}
		int capacity = vertices.length;
		while (capacity < required) {
			capacity = Math.max(capacity + 1, capacity * 2);
		}
		vertices = Arrays.copyOf(vertices, capacity);
	}
}
