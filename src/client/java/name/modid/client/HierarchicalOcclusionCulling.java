package name.modid.client;

import java.util.Arrays;

/**
 * Small CPU-side hierarchical-Z representation used as the safe data path for
 * render-thread occlusion tests. Producers publish conservative screen-space
 * depth samples; tests walk from coarse to fine levels and never reject a
 * region until a complete pyramid tile is available.
 */
public final class HierarchicalOcclusionCulling {
	private final float[][] levels;
	private final int[] widths;
	private final int[] heights;
	private boolean hasSamples;

	public HierarchicalOcclusionCulling(int width, int height) {
		if (width < 1 || height < 1) {
			throw new IllegalArgumentException("HZB dimensions must be positive");
		}
		int count = 1;
		int levelWidth = width;
		int levelHeight = height;
		while (levelWidth > 1 || levelHeight > 1) {
			levelWidth = Math.max(1, (levelWidth + 1) / 2);
			levelHeight = Math.max(1, (levelHeight + 1) / 2);
			count++;
		}
		levels = new float[count][];
		widths = new int[count];
		heights = new int[count];
		levelWidth = width;
		levelHeight = height;
		for (int i = 0; i < count; i++) {
			widths[i] = levelWidth;
			heights[i] = levelHeight;
			levels[i] = new float[levelWidth * levelHeight];
			levelWidth = Math.max(1, (levelWidth + 1) / 2);
			levelHeight = Math.max(1, (levelHeight + 1) / 2);
		}
		clear();
	}

	public void clear() {
		for (float[] level : levels) {
			Arrays.fill(level, 1.0f);
		}
		hasSamples = false;
	}

	public void publishDepth(int x, int y, float depth) {
		if (x < 0 || y < 0 || x >= widths[0] || y >= heights[0]) {
			return;
		}
		levels[0][y * widths[0] + x] = Math.min(levels[0][y * widths[0] + x], clamp(depth));
		hasSamples = true;
	}

	public void build() {
		for (int level = 1; level < levels.length; level++) {
			int parentWidth = widths[level];
			int childWidth = widths[level - 1];
			int childHeight = heights[level - 1];
			for (int y = 0; y < heights[level]; y++) {
				for (int x = 0; x < parentWidth; x++) {
					float minimum = 1.0f;
					for (int dy = 0; dy < 2; dy++) {
						for (int dx = 0; dx < 2; dx++) {
							int childX = x * 2 + dx;
							int childY = y * 2 + dy;
							if (childX < childWidth && childY < childHeight) {
								minimum = Math.min(minimum, levels[level - 1][childY * childWidth + childX]);
							}
						}
					}
					levels[level][y * parentWidth + x] = minimum;
				}
			}
		}
	}

	public boolean isOccluded(int minX, int minY, int maxX, int maxY, float nearestDepth) {
		if (!hasSamples || minX > maxX || minY > maxY) {
			return false;
		}
		int level = 0;
		while (level + 1 < levels.length
				&& (maxX - minX + 1 > 2 || maxY - minY + 1 > 2)) {
			minX >>= 1;
			minY >>= 1;
			maxX >>= 1;
			maxY >>= 1;
			level++;
		}
		for (int y = Math.max(0, minY); y <= Math.min(heights[level] - 1, maxY); y++) {
			for (int x = Math.max(0, minX); x <= Math.min(widths[level] - 1, maxX); x++) {
				if (levels[level][y * widths[level] + x] >= nearestDepth) {
					return false;
				}
			}
		}
		return true;
	}

	private static float clamp(float depth) {
		return Math.max(0.0f, Math.min(1.0f, depth));
	}
}
