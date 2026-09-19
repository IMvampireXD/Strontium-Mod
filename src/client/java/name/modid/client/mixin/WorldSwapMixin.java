package name.modid.client.mixin;

import name.modid.client.WorldSwapCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class WorldSwapMixin {
	@Inject(method = "setLevel", at = @At("HEAD"))
	private void strontium$prepareWorldSwap(ClientLevel level, CallbackInfo callback) {
		WorldSwapCache.begin(level);
	}
}
