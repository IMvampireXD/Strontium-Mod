package name.modid.client.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.GpuBackend;
import name.modid.Strontium;
import name.modid.client.OpenGL46;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class OpenGLWindowMixin {
	@Inject(
			method = "createGlfwWindow",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/systems/GpuBackend;setWindowHints()V",
					shift = At.Shift.AFTER
			)
	)
	private static void strontium$configureOpenGL46(int width, int height, String title, long monitor,
			GpuBackend backend, CallbackInfoReturnable<Long> callback) {
		OpenGL46.applyHints(Strontium.LOGGER);
	}
}
