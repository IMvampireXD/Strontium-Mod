package name.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import name.modid.optimization.StrontiumOptimizations;
import name.modid.optimization.BufferedRawInput;
import name.modid.client.ModelBatcher;
import name.modid.client.shader.ShaderPackManager;
import name.modid.client.shader.ShaderPipeline;
import name.modid.client.shader.ShaderPack;
import name.modid.client.shader.ShaderBackend;
import net.minecraft.client.Minecraft;
import net.fabricmc.loader.api.FabricLoader;

public class StrontiumClient implements ClientModInitializer {
	public static StrontiumOptimizations optimizations;
	public static final BufferedRawInput<Runnable> inputEvents = new BufferedRawInput<>();
	public static final ModelBatcher<Integer, Object> modelBatches = new ModelBatcher<>();
	public static ShaderPackManager shaderPacks;
	public static ShaderPipeline shaderPipeline;
	public static ShaderBackend shaderBackend;

	@Override
	public void onInitializeClient() {
		optimizations = new StrontiumOptimizations();
		shaderPacks = new ShaderPackManager(FabricLoader.getInstance().getGameDir());
		shaderPipeline = new ShaderPipeline();
		shaderBackend = new ShaderBackend();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			inputEvents.drainTo(Runnable::run);
			modelBatches.clear();
			optimizations.clearFrameCaches();
		});
	}

	public static boolean activateShaderPack(ShaderPack pack) {
		shaderPipeline.activate(pack);
		Minecraft minecraft = Minecraft.getInstance();
		boolean loaded = shaderBackend.load(pack, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
		if (!loaded) {
			shaderBackend.enableBuiltinLighting(
					minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
		}
		return loaded;
	}

	public static void disableShaderPack() {
		if (shaderPacks != null) {
			shaderPacks.deactivate();
		}
		if (shaderBackend != null) {
			shaderBackend.close();
		}
	}

	public static boolean builtinLightingActive() {
		return shaderBackend != null && shaderBackend.isEnabled();
	}
}