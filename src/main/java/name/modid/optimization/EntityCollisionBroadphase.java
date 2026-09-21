package name.modid.optimization;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Broad-phase candidate index for dense entity regions. Vanilla still owns
 * canCollideWith and narrow-phase resolution; this only avoids comparing
 * entities whose grid cells cannot overlap.
 */
public final class EntityCollisionBroadphase {
	private static final double CELL_SIZE = 2.0;
	private static final int DEFAULT_THRESHOLD = 32;
	private final int threshold;
	private final Map<Cell, List<Entity>> cells = new HashMap<>();

	public EntityCollisionBroadphase() {
		threshold = Math.max(2, Integer.getInteger("strontium.collisionThreshold", DEFAULT_THRESHOLD));
	}

	public void rebuild(Iterable<? extends Entity> entities) {
		cells.clear();
		for (Entity entity : entities) {
			AABB box = entity.getBoundingBox();
			int minX = cell(box.minX);
			int maxX = cell(box.maxX);
			int minZ = cell(box.minZ);
			int maxZ = cell(box.maxZ);
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					cells.computeIfAbsent(new Cell(x, z), ignored -> new ArrayList<>()).add(entity);
				}
			}
		}
	}

	public List<Entity> candidates(Entity entity) {
		AABB box = entity.getBoundingBox();
		List<Entity> result = new ArrayList<>();
		for (int x = cell(box.minX) - 1; x <= cell(box.maxX) + 1; x++) {
			for (int z = cell(box.minZ) - 1; z <= cell(box.maxZ) + 1; z++) {
				List<Entity> bucket = cells.get(new Cell(x, z));
				if (bucket != null) {
					for (Entity candidate : bucket) {
						if (candidate != entity && candidate.getBoundingBox().intersects(box)
								&& !result.contains(candidate)) {
							result.add(candidate);
						}
					}
				}
			}
		}
		return result;
	}

	public boolean shouldAccelerate(int entityCount) {
		return entityCount >= threshold;
	}

	public int threshold() {
		return threshold;
	}

	private static int cell(double coordinate) {
		return (int) Math.floor(coordinate / CELL_SIZE);
	}

	private record Cell(int x, int z) {
	}
}
