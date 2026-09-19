package name.modid.client.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
	@Invoker("addRenderableWidget")
	<T extends net.minecraft.client.gui.components.events.GuiEventListener
			& net.minecraft.client.gui.components.Renderable
			& net.minecraft.client.gui.narration.NarratableEntry> T strontium$addRenderableWidget(T widget);
}
