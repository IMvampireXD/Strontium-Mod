package name.modid.client.mixin;

import name.modid.client.StrontiumClient;
import name.modid.client.shader.ShaderPackScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
	@Inject(method = "init", at = @At("TAIL"))
	private void strontium$addShaderButton(CallbackInfo callback) {
		PauseScreen screen = (PauseScreen) (Object) this;
		((ScreenAccessor) (Object) screen).strontium$addRenderableWidget(Button.builder(
				Component.literal("Strontium Shaders"),
				button -> Minecraft.getInstance().setScreenAndShow(
						new ShaderPackScreen(screen, StrontiumClient.shaderPacks)))
				.bounds(screen.width / 2 - 100, screen.height - 52, 200, 20).build());
	}
}
