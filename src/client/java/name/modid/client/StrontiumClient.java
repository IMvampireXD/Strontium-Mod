package name.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import name.modid.optimization.StrontiumOptimizations;
import name.modid.optimization.BufferedRawInput;
import name.modid.optimization.AsyncRawMouseInput;
import name.modid.client.ModelBatcher;

public class StrontiumClient implements ClientModInitializer {
	private static final Identifier PERFORMANCE_HUD_ID =
			Identifier.fromNamespaceAndPath("strontium", "performance_hud");
	public static StrontiumOptimizations optimizations;
	public static final BufferedRawInput<Runnable> inputEvents = new BufferedRawInput<>();
	public static AsyncRawMouseInput rawMouseInput;
	public static final ModelBatcher<Integer, Object> modelBatches = new ModelBatcher<>();
	private static volatile int serverTicksPerSecond = 20;
	private static long tickWindowStart;
	private static int tickWindow;

	public static int serverTicksPerSecond() {
		return serverTicksPerSecond;
	}

	@Override
	public void onInitializeClient() {
		optimizations = new StrontiumOptimizations();
		rawMouseInput = new AsyncRawMouseInput();
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, PERFORMANCE_HUD_ID,
				StrontiumClient::renderPerformanceHud);
		tickWindowStart = System.nanoTime();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			inputEvents.drainTo(Runnable::run);
			rawMouseInput.drain((deltaX, deltaY, timestamp) -> inputEvents.offer(() -> {
				// Platform callbacks can enqueue here without running game logic.
			}));
			modelBatches.clear();
			optimizations.metrics().beginRenderPass();
			optimizations.clearFrameCaches();
			optimizations.particles().beginFrame(
					client.getFrameTimeNs() / 1_000_000.0);
			tickWindow++;
			long now = System.nanoTime();
			if (now - tickWindowStart >= 1_000_000_000L) {
				serverTicksPerSecond = (int) (tickWindow * 1_000_000_000L / (now - tickWindowStart));
				tickWindow = 0;
				tickWindowStart = now;
			}
		});
	}

	private static void renderPerformanceHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		int pingValue = 0;
		ClientPacketListener connection = minecraft.getConnection();
		if (connection != null) {
			PlayerInfo playerInfo = connection.getPlayerInfo(minecraft.player.getUUID());
			if (playerInfo != null) {
				pingValue = playerInfo.getLatency();
			}
		}

		Font font = minecraft.font;
		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = minecraft.getWindow().getGuiScaledHeight();
		int hotbarRight = (screenWidth >> 1) + 91;
		int textX = hotbarRight + 6;
		int fpsY = screenHeight - 32;
		int pingY = screenHeight - 21;
		graphics.text(font, "FPS: " + minecraft.getFps(), textX, fpsY, -1, true);
		graphics.text(font, "Ping: " + pingValue + "ms", textX, pingY, -1, true);
	}
}