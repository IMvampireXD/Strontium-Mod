package name.modid.client.shader;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ShaderStatusScreen extends Screen {
	private final Screen parent;
	private final String message;

	public ShaderStatusScreen(Screen parent, String message) {
		super(Component.literal("Strontium Shaders"));
		this.parent = parent;
		this.message = message;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal("OK"),
				button -> minecraft.setScreenAndShow(parent))
				.bounds(width / 2 - 75, height / 2 + 20, 150, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, height / 2 - 20, 0xFFFFFF);
		graphics.centeredText(font, Component.literal(message), width / 2, height / 2, 0xCCCCCC);
	}
}
