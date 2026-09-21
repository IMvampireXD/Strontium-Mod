package name.modid.optimization;

/**
 * Validates monotonic packet sequence numbers while allowing a small bounded
 * gap for a reconnect or a packet that was discarded by the transport.
 */
public final class SequenceEpochValidator {
	public enum Result {
		ACCEPTED,
		RESYNCED,
		REJECTED
	}

	private final long maximumResyncGap;
	private long epoch = -1;
	private long nextSequence;

	public SequenceEpochValidator(long maximumResyncGap) {
		if (maximumResyncGap < 0) {
			throw new IllegalArgumentException("maximumResyncGap must not be negative");
		}
		this.maximumResyncGap = maximumResyncGap;
	}

	public synchronized Result validate(long packetEpoch, long sequence) {
		if (packetEpoch < 0 || sequence < 0) {
			return Result.REJECTED;
		}
		if (epoch < 0) {
			epoch = packetEpoch;
			nextSequence = sequence + 1;
			return Result.ACCEPTED;
		}
		if (packetEpoch < epoch || packetEpoch > epoch + 1) {
			return Result.REJECTED;
		}
		if (packetEpoch > epoch) {
			epoch = packetEpoch;
			nextSequence = sequence + 1;
			return Result.RESYNCED;
		}
		if (sequence < nextSequence) {
			return Result.REJECTED;
		}
		long gap = sequence - nextSequence;
		if (gap > maximumResyncGap) {
			return Result.REJECTED;
		}
		nextSequence = sequence + 1;
		return gap == 0 ? Result.ACCEPTED : Result.RESYNCED;
	}

	public synchronized void reset(long newEpoch) {
		if (newEpoch < 0) {
			throw new IllegalArgumentException("epoch must not be negative");
		}
		epoch = newEpoch;
		nextSequence = 0;
	}

	public synchronized long epoch() {
		return epoch;
	}

	public synchronized long nextSequence() {
		return nextSequence;
	}
}
