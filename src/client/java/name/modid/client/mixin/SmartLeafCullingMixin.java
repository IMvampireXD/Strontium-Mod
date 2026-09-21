package name.modid.client.mixin;

import name.modid.client.SmartLeafCulling;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeavesBlock.class)
public abstract class SmartLeafCullingMixin {
	@Inject(method = "skipRendering", at = @At("RETURN"), cancellable = true)
	private void strontium$hideDenseCanopyFace(BlockState state, BlockState adjacent,
			Direction direction, CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValue()
				&& SmartLeafCulling.shouldHideFace(state, adjacent, direction)) {
			callback.setReturnValue(true);
		}
	}
}
