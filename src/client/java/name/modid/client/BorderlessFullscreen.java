package name.modid.client;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public final class BorderlessFullscreen {
	private static boolean active;
	private static int x;
	private static int y;
	private static int width;
	private static int height;

	private BorderlessFullscreen() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void toggle(Window window) {
		if (active) {
			disable(window);
		} else {
			enable(window);
		}
	}

	public static void enable(Window window) {
		if (active) {
			return;
		}
		long handle = window.handle();
		int[] currentX = new int[1];
		int[] currentY = new int[1];
		int[] currentWidth = new int[1];
		int[] currentHeight = new int[1];
		GLFW.glfwGetWindowPos(handle, currentX, currentY);
		GLFW.glfwGetWindowSize(handle, currentWidth, currentHeight);
		x = currentX[0];
		y = currentY[0];
		width = currentWidth[0];
		height = currentHeight[0];
		long monitor = GLFW.glfwGetPrimaryMonitor();
		GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);
		if (monitor == 0L || videoMode == null) {
			return;
		}
		int screenWidth = videoMode.width();
		int screenHeight = videoMode.height();
		GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
		GLFW.glfwSetWindowMonitor(handle, monitor, 0, 0, screenWidth, screenHeight, videoMode.refreshRate());
		window.setWidth(screenWidth);
		window.setHeight(screenHeight);
		active = true;
	}

	public static void disable(Window window) {
		if (!active) {
			return;
		}
		long handle = window.handle();
		GLFW.glfwSetWindowMonitor(handle, 0L, x, y, Math.max(1, width), Math.max(1, height),
				GLFW.GLFW_DONT_CARE);
		GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
		window.setWidth(Math.max(1, width));
		window.setHeight(Math.max(1, height));
		active = false;
	}
}
