package name.modid.client;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.entity.Entity;

public final class AggressiveCulling {
	private static final String PROPERTY = "strontium.cullingDistance";

	private AggressiveCulling() {
	}

	public static double maxDistance() {
		return Math.max(16.0, Double.parseDouble(System.getProperty(PROPERTY, "512")));
	}

	public static boolean keep(SectionRenderDispatcher.RenderSection section, double cameraX, double cameraY,
			double cameraZ) {
		double distance = section.getBoundingBox().getCenter().distanceToSqr(cameraX, cameraY, cameraZ);
		double limit = maxDistance() + 32.0;
		return distance <= limit * limit;
	}

	public static boolean keepEntity(Entity entity, double cameraX, double cameraY, double cameraZ) {
		if (entity.isAlwaysTicking() || entity.isSpectator()) {
			return true;
		}
		double dx = entity.getX() - cameraX;
		double dy = entity.getY() - cameraY;
		double dz = entity.getZ() - cameraZ;
		double limit = maxDistance();
		return dx * dx + dy * dy + dz * dz <= limit * limit;
	}
}
