package name.modid.client.mixin;

import name.modid.client.StrontiumClient;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityMetricsMixin {
	@Inject(method = "tryExtractRenderState", at = @At("RETURN"))
	private <E extends BlockEntity, S extends BlockEntityRenderState> void strontium$recordBlockEntity(
			E blockEntity, float tickDelta, ModelFeatureRenderer.CrumblingOverlay overlay, boolean outline,
			CallbackInfoReturnable<S> callback) {
		if (StrontiumClient.optimizations == null) {
			return;
		}
		if (callback.getReturnValue() == null) {
			StrontiumClient.optimizations.metrics().recordBlockEntitySkipped();
		} else {
			StrontiumClient.optimizations.metrics().recordBlockEntityRendered();
		}
	}
}
