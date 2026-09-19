package name.modid.client.mixin;

import name.modid.client.AggressiveCulling;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SectionOcclusionGraph.class)
public abstract class SectionCullingMixin {
	@Inject(method = "addSectionsInFrustum", at = @At("TAIL"))
	private void strontium$limitDistantSections(Frustum frustum,
			List<SectionRenderDispatcher.RenderSection> visibleSections,
			List<SectionRenderDispatcher.RenderSection> nearbyVisibleSections,
			CallbackInfo callback) {
		visibleSections.removeIf(section -> !AggressiveCulling.keep(section, frustum.getCamX(), frustum.getCamY(),
				frustum.getCamZ()));
		nearbyVisibleSections.removeIf(section -> !AggressiveCulling.keep(section, frustum.getCamX(),
				frustum.getCamY(), frustum.getCamZ()));
	}
}
