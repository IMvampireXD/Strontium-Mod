package name.modid.client.mixin;

import name.modid.client.FontWidthCache;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontRendererMixin {
	@Inject(method = "width(Ljava/lang/String;)I", at = @At("RETURN"), cancellable = true)
	private void strontium$cacheStringWidth(String text, CallbackInfoReturnable<Integer> callback) {
		callback.setReturnValue(FontWidthCache.getOrCompute(text, ignored -> callback.getReturnValue()));
	}
}
