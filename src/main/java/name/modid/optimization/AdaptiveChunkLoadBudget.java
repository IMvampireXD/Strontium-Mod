package name.modid.optimization;

public final class AdaptiveChunkLoadBudget {
	private final int minimum;
	private final int maximum;
	private final double targetFrameMillis;
	private int budget;

	public AdaptiveChunkLoadBudget(int minimum, int maximum, double targetFrameMillis) {
		if (minimum < 1 || maximum < minimum || targetFrameMillis <= 0) {
			throw new IllegalArgumentException("invalid chunk budget");
		}
		this.minimum = minimum;
		this.maximum = maximum;
		this.targetFrameMillis = targetFrameMillis;
		budget = minimum;
	}

	public synchronized int update(double frameMillis) {
		if (frameMillis > targetFrameMillis * 1.15) {
			budget = Math.max(minimum, budget - 1);
		} else if (frameMillis < targetFrameMillis * 0.75) {
			budget = Math.min(maximum, budget + 1);
		}
		return budget;
	}

	public synchronized int budget() {
		return budget;
	}
}
