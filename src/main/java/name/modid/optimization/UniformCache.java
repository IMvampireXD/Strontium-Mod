package name.modid.optimization;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Avoids redundant uniform uploads while keeping the cache scoped to one
 * render context. Call invalidate after a context/resource reload.
 */
public final class UniformCache {
	private final Map<String, Object> values = new HashMap<>();

	public boolean set(String name, Object value) {
		Objects.requireNonNull(name, "name");
		if (Objects.equals(values.put(name, value), value)) {
			return false;
		}
		return true;
	}

	public boolean setInt(String name, int value) {
		return set(name, Integer.valueOf(value));
	}

	public boolean setFloat(String name, float value) {
		return set(name, Float.valueOf(value));
	}

	public boolean setMatrix(String name, float[] matrix) {
		return set(name, new MatrixValue(matrix));
	}

	public void invalidate() {
		values.clear();
	}

	private record MatrixValue(float[] values) {
		private MatrixValue {
			values = values.clone();
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof MatrixValue matrix
					&& java.util.Arrays.equals(values, matrix.values);
		}

		@Override
		public int hashCode() {
			return java.util.Arrays.hashCode(values);
		}
	}
}
