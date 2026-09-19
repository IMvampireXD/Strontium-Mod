package name.modid.client.shader;

import name.modid.Strontium;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

/**
 * Render-thread-owned compatibility backend for fullscreen OptiFine-style
 * passes. Vanilla remains responsible for geometry and the backend only takes
 * over after vanilla has produced the frame.
 */
public final class ShaderBackend implements AutoCloseable {
	private static final String FULLSCREEN_VERTEX = """
			#version 330 core
			out vec2 texCoord;
			void main() {
				vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
				texCoord = p;
				gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
			}
			""";
	private static final String BUILTIN_LIGHTING_FRAGMENT = """
			#version 330 core
			in vec2 texCoord;
			uniform sampler2D colortex0;
			out vec4 strontiumColor;
			void main() {
				vec4 color = texture(colortex0, texCoord);
				float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
				vec3 lifted = mix(color.rgb, vec3(luminance), 0.08);
				strontiumColor = vec4(lifted * 1.06 + vec3(0.008), color.a);
			}
			""";
	private final List<CompiledPass> passes = new ArrayList<>();
	private int framebuffer;
	private int colorTexture;
	private int pingTexture;
	private int depthTexture;
	private int shadowFramebuffer;
	private int shadowTexture;
	private int vertexArray;
	private int width;
	private int height;
	private int frameCounter;
	private boolean enabled;
	private String failure;

	public synchronized boolean load(ShaderPack pack, int newWidth, int newHeight) {
		close();
		try {
			resize(newWidth, newHeight);
			boolean presentablePass = false;
			for (ShaderPass pass : pack.passes()) {
				if (isFullscreenPass(pass.name())) {
					passes.add(compile(pass));
					presentablePass |= isPresentablePass(pass.name());
				}
			}
			if (passes.isEmpty()) {
				throw new IllegalArgumentException("No deferred, composite, or final pass was found");
			}
			if (!presentablePass) {
				throw new IllegalArgumentException(
						"Pack contains deferred lighting stages but no composite/final presentation stage");
			}
			vertexArray = GL30.glGenVertexArrays();
			enabled = true;
			failure = null;
			Strontium.LOGGER.info("Shader backend enabled for {}", pack.name());
			return true;
		} catch (RuntimeException exception) {
			failure = exception.getMessage();
			Strontium.LOGGER.error("Shader backend disabled; vanilla rendering will be used", exception);
			close();
			return false;
		}
	}

	public synchronized boolean enableBuiltinLighting(int newWidth, int newHeight) {
		close();
		try {
			resize(newWidth, newHeight);
			passes.add(compileBuiltin());
			vertexArray = GL30.glGenVertexArrays();
			enabled = true;
			failure = null;
			Strontium.LOGGER.info("Built-in Strontium lighting enabled");
			return true;
		} catch (RuntimeException exception) {
			failure = exception.getMessage();
			Strontium.LOGGER.error("Built-in lighting could not be enabled", exception);
			close();
			return false;
		}
	}

	public synchronized boolean isEnabled() {
		return enabled;
	}

	public synchronized String failure() {
		return failure;
	}

	public synchronized void beginVanillaFrame(int newWidth, int newHeight) {
		if (!enabled) {
			return;
		}
		resize(newWidth, newHeight);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
				GL11.GL_TEXTURE_2D, colorTexture, 0);
		GL30.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
		GL11.glViewport(0, 0, width, height);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
	}

	public synchronized void applyAfterVanilla(int newWidth, int newHeight) {
		if (!enabled) {
			return;
		}
		try {
			clearGlErrors();
			resize(newWidth, newHeight);
			GL30.glBindVertexArray(vertexArray);
			GL11.glViewport(0, 0, width, height);
			for (CompiledPass pass : passes) {
				GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
				GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
						GL11.GL_TEXTURE_2D, pingTexture, 0);
				GL20.glUseProgram(pass.program());
				bindUniforms(pass.program());
				GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
				int old = colorTexture;
				colorTexture = pingTexture;
				pingTexture = old;
			}
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			present();
			GL20.glUseProgram(0);
			GL30.glBindVertexArray(0);
			int error = GL11.glGetError();
			if (error != GL11.GL_NO_ERROR) {
				throw new IllegalStateException("OpenGL error during shader post-processing: 0x"
						+ Integer.toHexString(error));
			}

			frameCounter++;
		} catch (RuntimeException exception) {
			failure = exception.getMessage();
			Strontium.LOGGER.error("Shader post-processing failed; disabling backend", exception);
			close();
		}
	}

	private static void clearGlErrors() {
		while (GL11.glGetError() != GL11.GL_NO_ERROR) {
			// Discard errors left by vanilla before entering the optional backend.
		}
	}

	private void present() {
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
		GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
				GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	private void bindUniforms(int program) {
		bindTexture(program, "colortex0", 0, colorTexture);
		bindTexture(program, "colortex1", 1, colorTexture);
		bindTexture(program, "colortex4", 4, colorTexture);
		bindTexture(program, "depthtex0", 1, depthTexture);
		bindTexture(program, "shadowtex0", 2, shadowTexture);
		uniformInt(program, "frameCounter", frameCounter);
		uniformFloat(program, "viewWidth", width);
		uniformFloat(program, "viewHeight", height);
		uniformFloat(program, "aspectRatio", (float) width / height);
		uniformFloat(program, "frameTimeCounter", frameCounter / 60.0f);
		uniformFloat(program, "texelSizeX", 1.0f / width);
		uniformFloat(program, "texelSizeY", 1.0f / height);
		uniformVec2(program, "texelSize", 1.0f / width, 1.0f / height);
		uniformFloat(program, "rainStrength", 0.0f);
		uniformFloat(program, "sunIntensity", 1.0f);
		uniformFloat(program, "moonIntensity", 0.0f);
		uniformFloat(program, "skyIntensity", 1.0f);
		uniformFloat(program, "skyIntensityNight", 0.0f);
		uniformFloat(program, "sunElevation", 1.0f);
		uniformVec3(program, "sunColor", 1.0f, 0.95f, 0.8f);
		uniformVec3(program, "nsunColor", 1.0f, 0.95f, 0.8f);
	}

	private static void bindTexture(int program, String name, int unit, int texture) {
		int location = GL20.glGetUniformLocation(program, name);
		if (location >= 0) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
			GL20.glUniform1i(location, unit);
		}
	}

	private static void uniformInt(int program, String name, int value) {
		int location = GL20.glGetUniformLocation(program, name);
		if (location >= 0) {
			GL20.glUniform1i(location, value);
		}
	}

	private static void uniformFloat(int program, String name, float value) {
		int location = GL20.glGetUniformLocation(program, name);
		if (location >= 0) {
			GL20.glUniform1f(location, value);
		}
	}

	private static void uniformVec2(int program, String name, float x, float y) {
		int location = GL20.glGetUniformLocation(program, name);
		if (location >= 0) {
			GL20.glUniform2f(location, x, y);
		}
	}

	private static void uniformVec3(int program, String name, float x, float y, float z) {
		int location = GL20.glGetUniformLocation(program, name);
		if (location >= 0) {
			GL20.glUniform3f(location, x, y, z);
		}
	}

	private void resize(int newWidth, int newHeight) {
		if (newWidth < 1 || newHeight < 1
				|| (newWidth == width && newHeight == height && framebuffer != 0)) {
			return;
		}
		deleteTargets();
		width = newWidth;
		height = newHeight;
		framebuffer = GL30.glGenFramebuffers();
		colorTexture = createColorTexture();
		pingTexture = createColorTexture();
		depthTexture = createDepthTexture(width, height);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT,
				GL11.GL_TEXTURE_2D, depthTexture, 0);
		checkFramebuffer("post-process");

		shadowFramebuffer = GL30.glGenFramebuffers();
		shadowTexture = createShadowTexture(2048);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, shadowFramebuffer);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
				GL11.GL_TEXTURE_2D, shadowTexture, 0);
		GL30.glDrawBuffer(GL11.GL_NONE);
		GL30.glReadBuffer(GL11.GL_NONE);
		checkFramebuffer("shadow");
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	private int createColorTexture() {
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
				GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		return texture;
	}

	private static int createDepthTexture(int width, int height) {
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0,
				GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, 0L);
		return texture;
	}

	private static int createShadowTexture(int size) {
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, size, size, 0,
				GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0L);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		return texture;
	}

	private static void checkFramebuffer(String name) {
		if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException(name + " framebuffer is incomplete");
		}
	}

	private CompiledPass compile(ShaderPass pass) {
		String fragment = pass.fragmentSource();
		if (fragment == null) {
			throw new IllegalArgumentException("Pass " + pass.name() + " has no fragment shader");
		}
		int vertex = compileStage(GL20.GL_VERTEX_SHADER,
				normalizeVertex(pass.vertexSource()), pass.name());
		int fragmentShader = compileStage(GL20.GL_FRAGMENT_SHADER,
				normalizeVersion(fragment), pass.name());
		int program = GL20.glCreateProgram();
		GL20.glAttachShader(program, vertex);
		GL20.glAttachShader(program, fragmentShader);
		GL20.glLinkProgram(program);
		GL20.glDeleteShader(vertex);
		GL20.glDeleteShader(fragmentShader);
		if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
			String log = GL20.glGetProgramInfoLog(program);
			GL20.glDeleteProgram(program);
			throw new IllegalArgumentException("Pass " + pass.name() + " link failed: " + log);
		}
		return new CompiledPass(program);
	}

	private CompiledPass compileBuiltin() {
		int vertex = compileStage(GL20.GL_VERTEX_SHADER, FULLSCREEN_VERTEX, "strontium-lighting");
		int fragment = compileStage(GL20.GL_FRAGMENT_SHADER, BUILTIN_LIGHTING_FRAGMENT, "strontium-lighting");
		int program = GL20.glCreateProgram();
		GL20.glAttachShader(program, vertex);
		GL20.glAttachShader(program, fragment);
		GL20.glLinkProgram(program);
		GL20.glDeleteShader(vertex);
		GL20.glDeleteShader(fragment);
		if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
			String log = GL20.glGetProgramInfoLog(program);
			GL20.glDeleteProgram(program);
			throw new IllegalArgumentException("built-in lighting link failed: " + log);
		}
		return new CompiledPass(program);
	}

	private static String normalizeVersion(String source) {
		int version = source.indexOf("#version");
		if (version >= 0) {
			int end = source.indexOf('\n', version);
			String body = end < 0 ? "" : source.substring(end + 1);
			return "#version 330 core\nout vec4 strontiumColor;\n"
					+ injectedSampler("colortex0", body)
					+ injectedSampler("depthtex0", body)
					+ injectedSampler("shadowtex0", body)
					+ normalizeLegacyOutputs(body);
		}
		return "#version 330 core\nout vec4 strontiumColor;\n"
				+ injectedSampler("colortex0", source)
				+ injectedSampler("depthtex0", source)
				+ injectedSampler("shadowtex0", source)
				+ normalizeLegacyOutputs(source);
	}

	private static String injectedSampler(String name, String source) {
		return source.matches("(?s).*\\buniform\\s+(?:sampler\\w*)\\s+" + name + "\\s*;.*")
				? ""
				: "uniform sampler2D " + name + ";\n";
	}

	private static String normalizeLegacyOutputs(String source) {
		return source.replaceAll("(?m)^\\s*#extension[^\\n]*\\n?", "")
				.replace("flat varying", "flat in")
				.replace("varying", "in")
				.replace("texture2D", "texture")
				.replace("texelFetch2D", "texelFetch")
				.replace("shadow2D", "texture")
				.replace("gl_FragColor", "strontiumColor")
				.replace("gl_FragData[0]", "strontiumColor");
	}

	private static String normalizeVertex(String source) {
		if (source == null || source.isBlank()) {
			return FULLSCREEN_VERTEX;
		}
		int version = source.indexOf("#version");
		String body = source;
		if (version >= 0) {
			int end = source.indexOf('\n', version);
			body = end < 0 ? "" : source.substring(end + 1);
		}
		body = body.replaceAll("(?m)^\\s*#extension[^\\n]*\\n?", "")
				.replace("texture2D", "texture")
				.replace("texelFetch2D", "texelFetch")
				.replace("shadow2D", "texture")
				.replace("flat varying", "flat out")
				.replace("varying", "out")
				.replace("attribute", "in")
				.replace("ftransform()", "strontiumTransform()")
				.replaceAll("\\bgl_MultiTexCoord0\\b", "strontiumTexCoord0()")
				.replaceAll("\\bgl_MultiTexCoord1\\b", "strontiumTexCoord1()")
				.replaceAll("\\bgl_Vertex\\b", "strontiumVertex()")
				.replaceAll("\\bgl_Normal\\b", "strontiumNormal()")
				.replaceAll("\\bgl_Color\\b", "strontiumColorInput()")
				.replaceAll("\\bgl_ModelViewMatrix\\b", "strontiumModelViewMatrix")
				.replaceAll("\\bgl_ProjectionMatrix\\b", "strontiumProjectionMatrix")
				.replaceAll("\\bgl_NormalMatrix\\b", "strontiumNormalMatrix");
		return """
				#version 330 core
				vec4 strontiumVertex() {
					vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
					return vec4(p * 2.0 - 1.0, 0.0, 1.0);
				}
				vec4 strontiumTransform() { return strontiumVertex(); }
				vec4 strontiumTexCoord0() {
					vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
					return vec4(p, 0.0, 1.0);
				}
				vec4 strontiumTexCoord1() { return strontiumTexCoord0(); }
				vec3 strontiumNormal() { return vec3(0.0, 1.0, 0.0); }
				vec4 strontiumColorInput() { return vec4(1.0); }
				uniform mat4 strontiumModelViewMatrix;
				uniform mat4 strontiumProjectionMatrix;
				uniform mat3 strontiumNormalMatrix;
				""" + body;
	}

	private static int compileStage(int type, String source, String name) {
		int shader = GL20.glCreateShader(type);
		GL20.glShaderSource(shader, source);
		GL20.glCompileShader(shader);
		if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			String log = GL20.glGetShaderInfoLog(shader);
			GL20.glDeleteShader(shader);
			throw new IllegalArgumentException("Pass " + name + " compilation failed: " + log);
		}
		return shader;
	}

	private static boolean isFullscreenPass(String name) {
		return name.equals("deferred") || name.startsWith("deferred")
				|| name.equals("composite") || name.startsWith("composite")
				|| name.equals("final");
	}

	private static boolean isPresentablePass(String name) {
		return name.equals("final") || name.equals("composite") || name.startsWith("composite");
	}

	private void deleteTargets() {
		if (framebuffer != 0) GL30.glDeleteFramebuffers(framebuffer);
		if (shadowFramebuffer != 0) GL30.glDeleteFramebuffers(shadowFramebuffer);
		if (colorTexture != 0) GL11.glDeleteTextures(colorTexture);
		if (pingTexture != 0) GL11.glDeleteTextures(pingTexture);
		if (depthTexture != 0) GL11.glDeleteTextures(depthTexture);
		if (shadowTexture != 0) GL11.glDeleteTextures(shadowTexture);
		framebuffer = 0;
		shadowFramebuffer = 0;
		colorTexture = 0;
		pingTexture = 0;
		depthTexture = 0;
		shadowTexture = 0;
	}

	@Override
	public synchronized void close() {
		enabled = false;
		for (CompiledPass pass : passes) GL20.glDeleteProgram(pass.program());
		passes.clear();
		deleteTargets();
		if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
		vertexArray = 0;
	}

	private record CompiledPass(int program) {
	}
}
