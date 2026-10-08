package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mads.industron.material.structure.StructureWoodChestBlock;
import net.mads.industron.material.structure.StructureWoodChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Item renderer for generated wood chests; vanilla's BEWLR only recognizes minecraft:chest. */
public final class StructureWoodChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    public StructureWoodChestItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof StructureWoodChestBlock chest)) {
            return;
        }

        StructureWoodChestBlockEntity blockEntity = new StructureWoodChestBlockEntity(
                BlockPos.ZERO,
                chest.defaultBlockState()
        );
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(
                blockEntity,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }
}
