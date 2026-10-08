package net.mads.industron.client.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.tool.MaterialEquipment;
import net.mads.industron.tool.ToolMaterialLookup;
import net.mads.industron.tool.ToolMaterialResolver;
import net.mads.industron.tool.ToolStackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Renders a finished tool by stacking the already-generated models of its permanent parts. */
public final class ComposedToolRenderer extends BlockEntityWithoutLevelRenderer {
    private static final double LAYER_Z_STEP = 0.00075D;
    private static final ItemStack HANDHELD_TRANSFORM_SOURCE = new ItemStack(Items.IRON_PICKAXE);

    public ComposedToolRenderer() {
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
        if (!(stack.getItem() instanceof MaterialEquipment tool) || !tool.valid(stack)) return;
        ToolStackData data = tool.data(stack);
        if (data == null) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        poseStack.pushPose();
        // The outer builtin/entity model has no vanilla handheld display transforms. ItemRenderer
        // has already applied its normal -0.5 local-space shift before entering the BEWLR, so first
        // cancel that shift, then borrow the perspective transform from a normal vanilla pickaxe.
        // This makes the dynamic composed item behave exactly like a regular handheld tool in GUI,
        // first person, third person, item frames and on the ground without moving the source sprites.
        poseStack.translate(0.5D, 0.5D, 0.5D);
        applyHandheldTransform(itemRenderer, displayContext, poseStack, stack);
        poseStack.translate(
                tool.definition().renderOffsetXPixels() / 16.0D,
                tool.definition().renderOffsetYPixels() / 16.0D,
                0.0D
        );

        List<LayeredPart> renderedParts = new ArrayList<>();
        for (ToolDefinition.PartSlot slot : tool.definition().parts()) {
            IndustrialSubstance material = ToolMaterialResolver.resolve(data.materialKey(slot.role()));
            if (material == null) continue;

            ItemStack partStack = ToolMaterialLookup.stackFor(material, slot.part());
            if (!partStack.isEmpty()) renderedParts.add(new LayeredPart(slot.layer(), partStack));
        }
        for (ToolDefinition.FixedPartSlot slot : tool.definition().fixedParts()) {
            var fixedItem = BuiltInRegistries.ITEM.get(slot.itemId());
            if (fixedItem == Items.AIR) continue;
            renderedParts.add(new LayeredPart(slot.layer(), new ItemStack(fixedItem)));
        }
        renderedParts.sort(Comparator.comparingInt(LayeredPart::layer));

        for (LayeredPart part : renderedParts) {
            poseStack.pushPose();
            // The borrowed handheld/display transform above applies to the whole finished tool.
            // Material-derived and fixed components share the same 16x16 composition canvas;
            // only their depth changes so the ToolDefinition layer order remains deterministic.
            poseStack.translate(0.0D, 0.0D, part.layer() * LAYER_Z_STEP);
            itemRenderer.renderStatic(
                    part.stack(),
                    ItemDisplayContext.NONE,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    null,
                    0
            );
            poseStack.popPose();
        }
        poseStack.popPose();
    }


    private record LayeredPart(int layer, ItemStack stack) {
    }

    private static void applyHandheldTransform(
            ItemRenderer itemRenderer,
            ItemDisplayContext displayContext,
            PoseStack poseStack, ItemStack stack
    ) {
        ItemStack source = HANDHELD_TRANSFORM_SOURCE;
        if (stack.getItem() instanceof MaterialEquipment item) {
            source = switch (item.definition().id()) {
                case "bow" -> new ItemStack(Items.BOW);
                case "crossbow" -> new ItemStack(Items.CROSSBOW);
                case "fishing_rod" -> new ItemStack(Items.FISHING_ROD);
                default -> source;
            };
        }
        BakedModel handheldModel = itemRenderer.getModel(
                source,
                Minecraft.getInstance().level,
                null,
                0
        );
        boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        handheldModel.applyTransform(displayContext, poseStack, leftHand);
    }
}
