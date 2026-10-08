package net.mads.industron.mixin;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.mads.industron.client.model.SharedQuadVertices;
import net.mads.industron.client.model.VertexSharingCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
/** Share immutable data only; retain each quad's sprite, tint, lighting and other metadata. */
@Mixin(BakedQuad.class)
public abstract class BakedQuadVerticesMixin implements SharedQuadVertices {
    @Shadow @Final @Mutable protected int[] vertices;
    @Override public void industron$shareVertices(VertexSharingCache cache){vertices=cache.share(vertices);}
}
