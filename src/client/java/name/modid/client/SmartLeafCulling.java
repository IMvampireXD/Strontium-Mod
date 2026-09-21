package name.modid.client;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Conservative face culling for dense leaf canopies. The vanilla leaf block
 * already exposes the distance from a trunk, which lets us avoid generating
 * faces hidden behind another nearby leaf without changing block lighting.
 */
public final class SmartLeafCulling {
	private static final String PROPERTY = "strontium.leafCullDistance";
	private static final int DEFAULT_DISTANCE = 3;

	private SmartLeafCulling() {
	}

	public static boolean shouldHideFace(BlockState state, BlockState adjacent, Direction direction) {
		if (!(state.getBlock() instanceof LeavesBlock)
				|| !(adjacent.getBlock() instanceof LeavesBlock)) {
			return false;
		}

		int distance = distance(adjacent);
		return distance <= threshold() && direction != Direction.DOWN;
	}

	public static boolean isEdgeLeaf(BlockState state, BlockState adjacent) {
		return state.getBlock() instanceof LeavesBlock
				&& adjacent.getBlock() instanceof LeavesBlock
				&& distance(adjacent) > threshold();
	}

	private static int distance(BlockState state) {
		return LeavesBlock.getOptionalDistanceAt(state).orElse(LeavesBlock.DECAY_DISTANCE);
	}

	private static int threshold() {
		int value = Integer.getInteger(PROPERTY, DEFAULT_DISTANCE);
		return Math.max(1, Math.min(LeavesBlock.DECAY_DISTANCE, value));
	}
}
