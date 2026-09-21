package name.modid.optimization;

/**
 * Stable distance buckets for throttling non-critical simulation or animation
 * work. Critical callers can always bypass the policy.
 */
public final class DistanceUpdatePolicy {
	private final double near;
	private final double far;
	private final int farInterval;

	public DistanceUpdatePolicy(double near, double far, int farInterval) {
		if (near < 0 || far < near || farInterval < 1) {
			throw new IllegalArgumentException("invalid distance policy");
		}
		this.near = near * near;
		this.far = far * far;
		this.farInterval = farInterval;
	}

	public boolean shouldUpdate(double distanceSquared, long tick, boolean critical) {
		if (critical || distanceSquared <= near) {
			return true;
		}
		if (distanceSquared <= far) {
			return (tick & 1L) == 0L;
		}
		return Math.floorMod(tick, farInterval) == 0;
	}
}
