package net.mads.industron.client;

import net.mads.industron.Industron;
import net.mads.industron.material.structure.StructureWoodChestBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Uses the generated per-WoodMaterial chest sprites while retaining vanilla chest geometry/animation. */
public final class StructureWoodChestRenderer extends ChestRenderer<StructureWoodChestBlockEntity> {
    public StructureWoodChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Material getMaterial(StructureWoodChestBlockEntity blockEntity, ChestType chestType) {
        String file = switch (chestType) {
            case LEFT -> "normal_left";
            case RIGHT -> "normal_right";
            case SINGLE -> "normal";
        };
        ResourceLocation sprite = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "entity/chest/structure_materials/" + blockEntity.material().id() + "/" + file
        );
        return new Material(Sheets.CHEST_SHEET, sprite);
    }
}
