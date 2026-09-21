package name.modid.optimization;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class DirtyRegionBatch {
	private final Set<BlockPos> positions = new HashSet<>();

	public synchronized void mark(BlockPos position) {
		positions.add(position.immutable());
	}

	public synchronized int drain(Consumer<BlockPos> consumer, int limit) {
		int count = 0;
		while (count < limit && !positions.isEmpty()) {
			BlockPos position = positions.iterator().next();
			positions.remove(position);
			consumer.accept(position);
			count++;
		}
		return count;
	}

	public synchronized int size() {
		return positions.size();
	}

	public synchronized void clear() {
		positions.clear();
	}
}
