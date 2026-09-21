package name.modid.optimization;

/**
 * Tracks inactivity without sleeping or stopping the network event loop.
 * Light idle may reduce optional work; deep idle may skip it entirely.
 */
public final class IdleGate {
	public enum State { ACTIVE, LIGHT, DEEP }

	private final long lightAfterNanos;
	private final long deepAfterNanos;
	private volatile long lastActivity;

	public IdleGate(long lightAfterNanos, long deepAfterNanos) {
		if (lightAfterNanos < 0 || deepAfterNanos < lightAfterNanos) {
			throw new IllegalArgumentException("invalid idle thresholds");
		}
		this.lightAfterNanos = lightAfterNanos;
		this.deepAfterNanos = deepAfterNanos;
		lastActivity = System.nanoTime();
	}

	public void activity() {
		lastActivity = System.nanoTime();
	}

	public void activity(long nowNanos) {
		lastActivity = nowNanos;
	}

	public State state(long nowNanos) {
		long idle = Math.max(0, nowNanos - lastActivity);
		if (idle >= deepAfterNanos) return State.DEEP;
		if (idle >= lightAfterNanos) return State.LIGHT;
		return State.ACTIVE;
	}

	public boolean allowOptionalWork(long nowNanos) {
		return state(nowNanos) != State.DEEP;
	}

	public long lastActivityNanos() {
		return lastActivity;
	}
}
