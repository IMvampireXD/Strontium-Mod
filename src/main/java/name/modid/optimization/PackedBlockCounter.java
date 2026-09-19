package name.modid.optimization;

import java.util.Arrays;

/**
 * Delays block-count writes and stores the result in a compact primitive array.
 * The caller supplies a stable palette index for each block.
 */
public final class PackedBlockCounter {
	private final int[] counts;
	private boolean dirty;

	public PackedBlockCounter(int paletteSize) {
		if (paletteSize < 1) {
			throw new IllegalArgumentException("paletteSize must be positive");
		}
		counts = new int[paletteSize];
	}

	public void add(int paletteIndex) {
		if (paletteIndex < 0 || paletteIndex >= counts.length) {
			throw new IndexOutOfBoundsException("palette index: " + paletteIndex);
		}
		counts[paletteIndex]++;
		dirty = true;
	}

	public int count(int paletteIndex) {
		return counts[paletteIndex];
	}

	public boolean isDirty() {
		return dirty;
	}

	public void clear() {
		Arrays.fill(counts, 0);
		dirty = false;
	}

	public int[] snapshot() {
		dirty = false;
		return counts.clone();
	}
}
