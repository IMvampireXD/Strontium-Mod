package name.modid.client;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

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
}
