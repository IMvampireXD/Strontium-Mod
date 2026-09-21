package name.modid.optimization;

public final class ParticleBudget {
	private final int minimum;
	private final int maximum;
	private int budget;
	private int remaining;

	public ParticleBudget(int minimum, int maximum) {
		if (minimum < 1 || maximum < minimum) {
			throw new IllegalArgumentException("invalid particle budget");
		}
		this.minimum = minimum;
		this.maximum = maximum;
		budget = maximum;
		remaining = maximum;
	}

	public synchronized void beginFrame(double frameMillis) {
		if (frameMillis > 40.0) {
			budget = Math.max(minimum, budget - 1);
		} else if (frameMillis < 20.0) {
			budget = Math.min(maximum, budget + 1);
		}
		remaining = budget;
	}

	public synchronized boolean allow(double distanceSquared, double frameMillis) {
		if (distanceSquared > 128.0 * 128.0) {
			return false;
		}
		return remaining > 0 && remaining-- > 0;
	}

	public synchronized int budget() {
		return budget;
	}
}
