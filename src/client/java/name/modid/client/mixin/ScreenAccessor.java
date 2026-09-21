package name.modid.client.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
	@Invoker("addRenderableWidget")
	<T extends GuiEventListener & Renderable & NarratableEntry> T strontium$addRenderableWidget(T widget);

	@org.spongepowered.asm.mixin.gen.Accessor("width")
	int strontium$getWidth();

	@org.spongepowered.asm.mixin.gen.Accessor("height")
	int strontium$getHeight();
}
