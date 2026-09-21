package name.modid.optimization;

import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

public final class UniformLocationCache {
	private final Map<Key, Integer> locations = new HashMap<>();

	public synchronized int get(int program, String name) {
		return locations.computeIfAbsent(new Key(program, name),
				key -> GL20.glGetUniformLocation(key.program(), key.name()));
	}

	public synchronized void invalidateProgram(int program) {
		locations.keySet().removeIf(key -> key.program() == program);
	}

	public synchronized void clear() {
		locations.clear();
	}

	private record Key(int program, String name) {
	}
}
