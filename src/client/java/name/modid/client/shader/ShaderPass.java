package name.modid.client.shader;

import java.util.ArrayList;
import java.util.List;

public record ShaderPass(String name, String vertexSource, String fragmentSource, List<String> targets) {
	public ShaderPass {
		targets = List.copyOf(new ArrayList<>(targets));
	}
}
