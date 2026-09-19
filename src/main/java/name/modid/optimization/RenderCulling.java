package name.modid.optimization;

public final class RenderCulling {
	private RenderCulling() {
	}

	public static boolean isChunkWithinDistance(int chunkX, int chunkZ, int cameraChunkX, int cameraChunkZ, int viewDistance) {
		long dx = (long) chunkX - cameraChunkX;
		long dz = (long) chunkZ - cameraChunkZ;
		long distance = (long) viewDistance + 1;
		return dx * dx + dz * dz <= distance * distance;
	}

	public static boolean isSphereVisible(double x, double y, double z, double cameraX, double cameraY,
			double cameraZ, double radius, double maxDistance) {
		double dx = x - cameraX;
		double dy = y - cameraY;
		double dz = z - cameraZ;
		double limit = maxDistance + radius;
		return dx * dx + dy * dy + dz * dz <= limit * limit;
	}
}
