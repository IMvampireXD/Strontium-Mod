package name.modid.optimization;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Interns immutable vertex keys. The bounded cache prevents resource reloads
 * or procedural geometry from retaining an unbounded number of vertices.
 */
public final class VertexDeduplicator {
	private final BoundedCache<VertexKey, VertexKey> cache;

	public VertexDeduplicator(int capacity) {
		cache = new BoundedCache<>(capacity);
	}

	public VertexKey canonical(float[] position, float[] normal, float[] uv, int color, int light) {
		VertexKey key = new VertexKey(position, normal, uv, color, light);
		return cache.getOrCompute(key, ignored -> key);
	}

	public void clear() {
		cache.clear();
	}

	public record VertexKey(float[] position, float[] normal, float[] uv, int color, int light) {
		public VertexKey {
			position = position.clone();
			normal = normal.clone();
			uv = uv.clone();
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof VertexKey vertex
					&& color == vertex.color
					&& light == vertex.light
					&& Arrays.equals(position, vertex.position)
					&& Arrays.equals(normal, vertex.normal)
					&& Arrays.equals(uv, vertex.uv);
		}

		@Override
		public int hashCode() {
			int result = Arrays.hashCode(position);
			result = 31 * result + Arrays.hashCode(normal);
			result = 31 * result + Arrays.hashCode(uv);
			result = 31 * result + color;
			return 31 * result + light;
		}
	}
}
