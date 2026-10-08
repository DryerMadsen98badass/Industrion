package net.mads.industron.client;

import com.mojang.datafixers.util.Pair;
import net.mads.industron.Industron;
import net.mads.industron.material.structure.WoodMaterial;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Reuses vanilla boat/chest-boat geometry and animation, but swaps the texture to the
 * generated WoodMaterial texture.  The generated entities keep Boat.Type.OAK only as
 * their geometry/physics carrier; their material identity lives in their registered type.
 */
public final class StructureWoodBoatRenderer extends BoatRenderer {
    private final ResourceLocation texture;

    public StructureWoodBoatRenderer(
            EntityRendererProvider.Context context,
            WoodMaterial material,
            boolean chestBoat
    ) {
        super(context, chestBoat);
        this.texture = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "textures/entity/" + (chestBoat ? "chest_boat" : "boat")
                        + "/structure_materials/" + material.id() + ".png"
        );
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        Pair<ResourceLocation, ListModel<Boat>> vanilla = super.getModelWithLocation(boat);
        return Pair.of(texture, vanilla.getSecond());
    }
}
