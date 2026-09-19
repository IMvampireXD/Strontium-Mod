package name.modid.optimization;

public final class BackgroundThrottle {
	private volatile boolean focused = true;
	private volatile boolean enabled;
	private volatile int backgroundFps = 15;

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setBackgroundFps(int fps) {
		if (fps < 1 || fps > 240) {
			throw new IllegalArgumentException("background FPS must be between 1 and 240");
		}
		backgroundFps = fps;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	public int targetFps(int foregroundFps) {
		return enabled && !focused ? Math.min(foregroundFps, backgroundFps) : foregroundFps;
	}

	public boolean shouldThrottleChunks() {
		return enabled && !focused;
	}
}
