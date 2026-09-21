package name.modid.optimization;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Coalesces light positions until a region is queried. A caller must only
 * invoke drain when all neighboring chunks are loaded.
 */
public final class DeferredLightUpdates {
	private final ArrayDeque<BlockPos> pending = new ArrayDeque<>();
	private final Set<BlockPos> deduplication = new HashSet<>();

	public synchronized void enqueue(BlockPos position) {
		BlockPos immutable = position.immutable();
		if (deduplication.add(immutable)) {
			pending.addLast(immutable);
		}
	}

	public synchronized int size() {
		return pending.size();
	}

	public synchronized int drainIfNeighborsLoaded(boolean neighborsLoaded, Consumer<BlockPos> update) {
		if (!neighborsLoaded) {
			return 0;
		}
		int count = 0;
		while (!pending.isEmpty()) {
			BlockPos position = pending.removeFirst();
			deduplication.remove(position);
			update.accept(position);
			count++;
		}
		return count;
	}

	public synchronized void clear() {
		pending.clear();
		deduplication.clear();
	}
}
