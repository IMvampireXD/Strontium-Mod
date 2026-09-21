package name.modid.optimization;

/**
 * Optional native collision capability. The mod never requires a native
 * library: the Java broad phase remains the fallback on every platform.
 */
public final class NativeCollisionBackend {
	private final boolean available;

	public NativeCollisionBackend() {
		boolean enabled = Boolean.getBoolean("strontium.nativeCollision");
		boolean loaded = false;
		if (enabled) {
			try {
				System.loadLibrary("strontium_collision");
				loaded = true;
			} catch (LinkageError | SecurityException ignored) {
				loaded = false;
			}
		}
		available = loaded;
	}

	public boolean available() {
		return available;
	}
}
