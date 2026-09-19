package name.modid.optimization;

/**
 * Tracks the last bound render objects so redundant GL state changes can be
 * skipped by the render-thread adapter.
 */
public final class RenderResourceState {
	private int vertexArray;
	private int vertexBuffer;
	private int elementBuffer;
	private int framebuffer;
	private int texture;

	public boolean bindVertexArray(int id) {
		if (vertexArray == id) {
			return false;
		}
		vertexArray = id;
		return true;
	}

	public boolean bindVertexBuffer(int id) {
		if (vertexBuffer == id) {
			return false;
		}
		vertexBuffer = id;
		return true;
	}

	public boolean bindElementBuffer(int id) {
		if (elementBuffer == id) {
			return false;
		}
		elementBuffer = id;
		return true;
	}

	public boolean bindFramebuffer(int id) {
		if (framebuffer == id) {
			return false;
		}
		framebuffer = id;
		return true;
	}

	public boolean bindTexture(int id) {
		if (texture == id) {
			return false;
		}
		texture = id;
		return true;
	}

	public void invalidate() {
		vertexArray = 0;
		vertexBuffer = 0;
		elementBuffer = 0;
		framebuffer = 0;
		texture = 0;
	}
}
