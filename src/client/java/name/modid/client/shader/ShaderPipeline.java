package name.modid.client.shader;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Describes the ordered OptiFine/Iris-compatible stages for the active pack.
 * GPU compilation and framebuffer ownership are deliberately kept in the
 * render backend adapter, since those resources must be created on the render
 * thread.
 */
public final class ShaderPipeline {
	private static final List<String> ORDER = List.of(
			"setup", "begin", "shadow", "shadowcomp", "prepare",
			"gbuffers", "deferred", "composite", "final"
	);
	private final AtomicReference<ShaderPack> activePack = new AtomicReference<>();

	public void activate(ShaderPack pack) {
		activePack.set(pack);
	}

	public ShaderPack activePack() {
		return activePack.get();
	}

	public List<ShaderPass> orderedPasses() {
		ShaderPack pack = activePack.get();
		if (pack == null) {
			return List.of();
		}
		return pack.passes().stream()
				.sorted((left, right) -> Integer.compare(orderIndex(left.name()), orderIndex(right.name())))
				.toList();
	}

	private static int orderIndex(String name) {
		for (int index = 0; index < ORDER.size(); index++) {
			String prefix = ORDER.get(index);
			if (name.equals(prefix) || name.startsWith(prefix)) {
				return index;
			}
		}
		return ORDER.size();
	}
}
