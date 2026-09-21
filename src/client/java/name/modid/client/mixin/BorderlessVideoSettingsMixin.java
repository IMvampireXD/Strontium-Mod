package name.modid.client.mixin;

import name.modid.client.BorderlessFullscreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VideoSettingsScreen.class)
public abstract class BorderlessVideoSettingsMixin {
	@Inject(method = "addOptions", at = @At("TAIL"))
	private void strontium$addBorderlessOption(CallbackInfo callback) {
		VideoSettingsScreen screen = (VideoSettingsScreen) (Object) this;
		OptionsSubScreenAccessor options = (OptionsSubScreenAccessor) (Object) screen;
		options.strontium$getOptionsList().addBig(Button.builder(
				Component.literal(BorderlessFullscreen.isActive()
						? "Borderless Fullscreen: ON" : "Borderless Fullscreen: OFF"),
				button -> {
					BorderlessFullscreen.toggle(Minecraft.getInstance().getWindow());
					button.setMessage(Component.literal(BorderlessFullscreen.isActive()
							? "Borderless Fullscreen: ON" : "Borderless Fullscreen: OFF"));
				}).bounds(0, 0, 310, 20).build());
	}
}
