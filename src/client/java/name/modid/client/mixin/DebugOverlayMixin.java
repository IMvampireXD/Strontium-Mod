package name.modid.client.mixin;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugOverlayMixin {
	private static final String STRONTIUM_LABEL = "Strontium renderer 1.0";

	@Inject(method = "extractLines", at = @At("HEAD"))
	private void strontium$appendRendererLabel(GuiGraphicsExtractor extractor, List<String> lines,
			boolean rightSide, CallbackInfo callback) {
		if (!rightSide) {
			lines.add(STRONTIUM_LABEL);
		}
	}
}
