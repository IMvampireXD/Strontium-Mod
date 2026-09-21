package name.modid.optimization;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Bounded, priority-ordered delivery queue. Packet encoding happens once and
 * the resulting immutable bytes can be sent to many players.
 */
public final class ChunkDeliveryScheduler {
	private final PriorityQueue<Delivery> queue = new PriorityQueue<>(
			Comparator.comparingInt(Delivery::priority).thenComparingLong(Delivery::sequence));
	private final int capacity;
	private long sequence;

	public ChunkDeliveryScheduler(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.capacity = capacity;
	}

	public synchronized boolean offer(long chunkKey, byte[] packet, int priority) {
		if (queue.size() >= capacity) {
			Delivery farthest = queue.stream().max(Comparator.comparingInt(Delivery::priority)).orElse(null);
			if (farthest == null || farthest.priority() <= priority) {
				return false;
			}
			queue.remove(farthest);
		}
		queue.offer(new Delivery(chunkKey, packet.clone(), priority, sequence++));
		return true;
	}

	public synchronized Delivery poll() {
		return queue.poll();
	}

	public synchronized int size() {
		return queue.size();
	}

	public record Delivery(long chunkKey, byte[] packet, int priority, long sequence) {
		public Delivery {
			packet = packet.clone();
		}

		@Override
		public byte[] packet() {
			return packet.clone();
		}
	}
}
