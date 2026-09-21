package name.modid.client.mixin;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import name.modid.client.StrontiumClient;
import name.modid.optimization.RenderMetrics;
import name.modid.optimization.StringDeduplicator;
import name.modid.optimization.PacketTrafficMetrics;
import name.modid.Strontium;
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
			if (StrontiumClient.optimizations != null) {
				RenderMetrics metrics = StrontiumClient.optimizations.metrics();
				StringDeduplicator strings = Strontium.STRINGS;
				lines.add("[Culling] Last pass: " + metrics.cullingTime() + "ms");
				lines.add("[Culling] Passes: " + metrics.cullingPasses()
						+ " Last: " + metrics.cullingAgeMillis() + "ms ago");
				lines.add("[VRAM] " + StrontiumClient.optimizations.vram().summary());
				lines.add("[Culling] Rendered Block Entities: " + metrics.renderedBlockEntities()
						+ " Skipped: " + metrics.skippedBlockEntities());
				lines.add("[Culling] Rendered Entities: " + metrics.renderedEntities()
						+ " Skipped: " + metrics.skippedEntities());
				lines.add(strings.processed() + " Lowercase strings processed. "
						+ strings.unique() + " Unique. " + strings.deduplicated() + " Deduplicated");
				PacketTrafficMetrics traffic = StrontiumClient.optimizations.traffic();
				long inboundBytes = Math.max(traffic.inboundRaw(), traffic.inboundPackets());
				long outboundBytes = Math.max(traffic.outboundRaw(), traffic.outboundPackets());
				long inboundRate = Math.max(traffic.inboundRawPerSecond(), traffic.inboundPerSecond());
				long outboundRate = Math.max(traffic.outboundRawPerSecond(), traffic.outboundPerSecond());
				lines.add("Actual Transmission Inbound " + kib(inboundRate)
						+ " KiB/s, Total " + mib(inboundBytes) + " MiB");
				lines.add("Actual Transmission Outbound " + kib(outboundRate)
						+ " KiB/s, Total " + kib(outboundBytes) + " KiB");
				lines.add("Raw Payload Inbound " + kib(traffic.inboundRawPerSecond())
						+ " KiB/s, Total " + mib(traffic.inboundRaw()) + " MiB");
				lines.add("Raw Payload Outbound " + traffic.outboundRawPerSecond()
						+ " bytes/s, Total " + kib(traffic.outboundRaw()) + " KiB");
			}
		}
	}

	private static long kib(long bytes) {
		return bytes / 1024L;
	}

	private static long mib(long bytes) {
		return bytes / (1024L * 1024L);
	}
}
