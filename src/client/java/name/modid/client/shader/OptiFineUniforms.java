package name.modid.client.shader;

import org.lwjgl.opengl.GL20;

public final class OptiFineUniforms {
	private OptiFineUniforms() {
	}

	public static void upload(int program, int width, int height, int frameCounter) {
		setInt(program, "frameCounter", frameCounter);
		setFloat(program, "viewWidth", width);
		setFloat(program, "viewHeight", height);
		setFloat(program, "aspectRatio", (float) width / height);
		setFloat(program, "frameTimeCounter", frameCounter / 60.0f);
	}

	private static void setInt(int program, String name, int value) {
		int location = GL20.glGetUniformLocation(program, name);
		if (location >= 0) GL20.glUniform1i(location, value);
	}

	private static void setFloat(int program, String name, float value) {
		int location = GL20.glGetUniformLocation(program, name);
		if (location >= 0) GL20.glUniform1f(location, value);
	}
}
