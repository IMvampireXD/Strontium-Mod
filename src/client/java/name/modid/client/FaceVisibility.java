package name.modid.client;

import net.minecraft.core.Direction;

public final class FaceVisibility {
	private FaceVisibility() {
	}

	public static boolean facesCamera(Direction face, double normalX, double normalY, double normalZ,
			double cameraX, double cameraY, double cameraZ,
			double faceX, double faceY, double faceZ) {
		double viewX = cameraX - faceX;
		double viewY = cameraY - faceY;
		double viewZ = cameraZ - faceZ;
		double dot = normalX * viewX + normalY * viewY + normalZ * viewZ;
		return dot >= -0.001;
	}

	public static boolean facesCamera(Direction face, double cameraX, double cameraY, double cameraZ,
			double faceX, double faceY, double faceZ) {
		return facesCamera(face, face.getStepX(), face.getStepY(), face.getStepZ(),
				cameraX, cameraY, cameraZ, faceX, faceY, faceZ);
	}
}
