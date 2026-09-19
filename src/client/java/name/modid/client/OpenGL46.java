package name.modid.client;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

public final class OpenGL46 {
	public static final String PROPERTY = "strontium.opengl46";

	private OpenGL46() {
	}

	public static boolean enabled() {
		return Boolean.parseBoolean(System.getProperty(PROPERTY, "true"));
	}

	public static void applyHints(Logger logger) {
		if (!enabled()) {
			return;
		}
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
		logger.info("Requesting an OpenGL 4.6 core context");
	}
}
