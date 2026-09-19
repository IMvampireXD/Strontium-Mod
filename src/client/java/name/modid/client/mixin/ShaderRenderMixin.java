package name.modid.client.mixin;

import name.modid.client.StrontiumClient;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class ShaderRenderMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void strontium$beginGBuffer(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker,
			boolean renderBlockOutline, CameraRenderState camera, Matrix4fc modelView, GpuBufferSlice projection,
			Vector4f fogColor, boolean shouldRenderBlockOutline, CallbackInfo callback) {
		if (StrontiumClient.shaderBackend != null) {
			StrontiumClient.shaderBackend.beginVanillaFrame(
					net.minecraft.client.Minecraft.getInstance().getWindow().getWidth(),
					net.minecraft.client.Minecraft.getInstance().getWindow().getHeight());
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void strontium$applyPostProcess(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker,
			boolean renderBlockOutline, CameraRenderState camera, Matrix4fc modelView, GpuBufferSlice projection,
			Vector4f fogColor, boolean shouldRenderBlockOutline,
			CallbackInfo callback) {
		if (StrontiumClient.shaderBackend != null) {
			StrontiumClient.shaderBackend.applyAfterVanilla(
					net.minecraft.client.Minecraft.getInstance().getWindow().getWidth(),
					net.minecraft.client.Minecraft.getInstance().getWindow().getHeight());
		}
	}
}
