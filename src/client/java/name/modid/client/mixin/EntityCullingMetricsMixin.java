package name.modid.client.mixin;

import name.modid.client.AggressiveCulling;
import name.modid.client.StrontiumClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityCullingMetricsMixin {
	@Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
	private <E extends Entity> void strontium$recordAndCull(E entity, Frustum frustum,
			double cameraX, double cameraY, double cameraZ, CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValue() || !AggressiveCulling.keepEntity(entity, cameraX, cameraY, cameraZ)) {
			callback.setReturnValue(false);
			if (StrontiumClient.optimizations != null) {
				StrontiumClient.optimizations.metrics().recordEntitySkipped();
			}
			return;
		}
		if (StrontiumClient.optimizations != null) {
			StrontiumClient.optimizations.metrics().recordEntityRendered();
		}
	}
}
