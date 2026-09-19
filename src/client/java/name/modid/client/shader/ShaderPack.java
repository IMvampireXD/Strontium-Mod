package name.modid.client.shader;

import java.nio.file.Path;
import java.util.List;

public record ShaderPack(String name, Path file, List<ShaderPass> passes) {
	public ShaderPack {
		passes = List.copyOf(passes);
	}
}
