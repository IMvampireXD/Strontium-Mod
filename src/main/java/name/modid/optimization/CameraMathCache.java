package name.modid.optimization;

/**
 * Per-frame trigonometric cache. Camera callers must call beginFrame once
 * before reading values.
 */
public final class CameraMathCache {
	private long frame = Long.MIN_VALUE;
	private float yaw;
	private float pitch;
	private float sinYaw;
	private float cosYaw;
	private float sinPitch;
	private float cosPitch;

	public void beginFrame(long frame, float yaw, float pitch) {
		if (this.frame == frame && this.yaw == yaw && this.pitch == pitch) {
			return;
		}
		this.frame = frame;
		this.yaw = yaw;
		this.pitch = pitch;
		double yawRadians = Math.toRadians(-yaw - 180.0);
		double pitchRadians = Math.toRadians(-pitch);
		sinYaw = (float) Math.sin(yawRadians);
		cosYaw = (float) Math.cos(yawRadians);
		sinPitch = (float) Math.sin(pitchRadians);
		cosPitch = (float) Math.cos(pitchRadians);
	}

	public float sinYaw() { return sinYaw; }
	public float cosYaw() { return cosYaw; }
	public float sinPitch() { return sinPitch; }
	public float cosPitch() { return cosPitch; }
}
