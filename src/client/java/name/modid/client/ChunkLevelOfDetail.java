package name.modid.client;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

import java.util.Comparator;
import java.util.List;

/**
 * Distance policy for chunk work. It deliberately keeps the original mesh at
 * every distance: only work ordering changes, so distant terrain does not
 * acquire low-resolution seams or affect map/overlay consumers.
 */
public final class ChunkLevelOfDetail {
	private static final String PROPERTY = "strontium.lod";
	private static final double NEAR_DISTANCE = 64.0;
	private static final double MID_DISTANCE = 192.0;
	private static final double HYSTERESIS = 16.0;

	private ChunkLevelOfDetail() {
	}

	public static void orderVisible(List<SectionRenderDispatcher.RenderSection> sections,
			double cameraX, double cameraY, double cameraZ) {
		sections.sort(Comparator.<SectionRenderDispatcher.RenderSection>comparingInt(
						section -> level(section, cameraX, cameraY, cameraZ))
				.thenComparingDouble(section -> distanceSquared(section, cameraX, cameraY, cameraZ)));
	}

	public static int level(SectionRenderDispatcher.RenderSection section,
			double cameraX, double cameraY, double cameraZ) {
		if (!enabled()) {
			return 0;
		}
		double distance = Math.sqrt(distanceSquared(section, cameraX, cameraY, cameraZ));
		if (distance <= NEAR_DISTANCE - HYSTERESIS) {
			return 0;
		}
		if (distance <= MID_DISTANCE - HYSTERESIS) {
			return 1;
		}
		if (distance >= MID_DISTANCE + HYSTERESIS) {
			return 2;
		}
		return 1;
	}

	private static double distanceSquared(SectionRenderDispatcher.RenderSection section,
			double cameraX, double cameraY, double cameraZ) {
		return section.getBoundingBox().getCenter().distanceToSqr(cameraX, cameraY, cameraZ);
	}

	private static boolean enabled() {
		return Boolean.parseBoolean(System.getProperty(PROPERTY, "true"));
	}
}
