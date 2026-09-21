package name.modid.mixin;

import io.netty.channel.ChannelPipeline;
import name.modid.optimization.EncodedPacketChannelHandler;
import name.modid.optimization.EncodedPacketTransport;
import name.modid.optimization.PacketTrafficHandler;
import name.modid.optimization.PacketTrafficMetrics;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.protocol.Packet;

@Mixin(Connection.class)
public abstract class ConnectionTrafficMixin {
	@Inject(method = "configurePacketHandler", at = @At("TAIL"))
	private void strontium$installTrafficMetrics(ChannelPipeline pipeline, CallbackInfo callback) {
		/*
		 * The transport is deliberately opt-in. Negotiation is completed by a
		 * compatible peer through EncodedPacketChannelHandler.negotiate; without
		 * it this handler is a no-op and vanilla packet framing is untouched.
		 */
		if (Boolean.getBoolean("strontium.encodedTransport")
				&& pipeline.get(EncodedPacketChannelHandler.NAME) == null) {
			pipeline.addFirst(EncodedPacketChannelHandler.NAME,
					new EncodedPacketChannelHandler(new EncodedPacketTransport()));
		}
		if (pipeline.get("strontium_traffic") == null) {
			PacketTrafficHandler handler = new PacketTrafficHandler(PacketTrafficMetrics.GLOBAL);
			if (pipeline.get("splitter") != null) {
				pipeline.addBefore("splitter", "strontium_traffic", handler);
			} else {
				pipeline.addFirst("strontium_traffic", handler);
			}
		}
	}

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
	private void strontium$countOutbound(Packet<?> packet, CallbackInfo callback) {
		PacketTrafficMetrics.GLOBAL.recordOutboundPacketOnly(packet);
	}

	@Inject(method = "channelRead0", at = @At("HEAD"))
	private void strontium$countInbound(io.netty.channel.ChannelHandlerContext context,
			Packet<?> packet, CallbackInfo callback) {
		PacketTrafficMetrics.GLOBAL.recordInboundPacketOnly(packet);
	}
}
