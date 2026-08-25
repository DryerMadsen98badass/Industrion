package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.mads.industron.Industron;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockPattern;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockRegistry;
import net.mads.industron.machine.machines.electric.multiblock.PatternVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class MultiblockPreviewRenderer {
    private static final long PREVIEW_DURATION_MS = 10_000L;
    private static Preview preview;

    private MultiblockPreviewRenderer() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().isEmpty()
                || !event.getEntity().isShiftKeyDown()
                || !GogglesItem.isWearingGoggles(event.getEntity())) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof MultiblockControllerBlock controller)
                || state.getValue(MultiblockControllerBlock.FORMED)) {
            return;
        }

        MultiblockDefinition definition = MultiblockRegistry.byController(controller.controllerId()).orElse(null);
        if (definition == null) {
            return;
        }

        show(definition, event.getPos(), state.getValue(MultiblockControllerBlock.FACING));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || preview == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= preview.expiresAt()) {
            preview = null;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            preview = null;
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();

        for (PreviewBlock block : preview.blocks()) {
            poseStack.pushPose();
            poseStack.translate(block.pos().getX() - camera.x, block.pos().getY() - camera.y, block.pos().getZ() - camera.z);
            minecraft.getBlockRenderer().renderSingleBlock(block.state(), poseStack, buffer, LightTexture.FULL_BRIGHT, 0);
            poseStack.popPose();
        }
        buffer.endBatch();
    }

    private static void show(MultiblockDefinition definition, BlockPos controllerPos, Direction facing) {
        PatternVariant variant = definition.variants().stream()
                .filter(candidate -> "1".equals(candidate.id()))
                .findFirst()
                .orElse(definition.variants().get(0));
        LocalPos controllerLocal = controllerLocalPos(definition, variant);
        List<PreviewBlock> blocks = new ArrayList<>();

        for (int x = 0; x < variant.width(); x++) {
            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    char symbol = variant.symbolAt(x, y, z);
                    if (symbol == MultiblockPattern.air || symbol == MultiblockPattern.ignore) {
                        continue;
                    }

                    BlockState state = renderState(definition, symbol, facing);
                    if (state == null) {
                        continue;
                    }

                    BlockPos worldPos = rotate(controllerPos, facing, x - controllerLocal.x(), y - controllerLocal.y(), z - controllerLocal.z());
                    blocks.add(new PreviewBlock(worldPos, state));
                }
            }
        }

        preview = new Preview(blocks, System.currentTimeMillis() + PREVIEW_DURATION_MS);
    }

    private static LocalPos controllerLocalPos(MultiblockDefinition definition, PatternVariant variant) {
        for (int x = 0; x < variant.width(); x++) {
            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    if (variant.symbolAt(x, y, z) == definition.controllerSymbol()) {
                        return new LocalPos(x, y, z);
                    }
                }
            }
        }
        return new LocalPos(0, 0, 0);
    }

    private static BlockPos rotate(BlockPos controllerPos, Direction facing, int localX, int localY, int localZ) {
        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();
        return controllerPos
                .relative(right, localX)
                .above(localY)
                .relative(forward, localZ);
    }

    private static BlockState renderState(MultiblockDefinition definition, char symbol, Direction facing) {
        BlockState state;
        if (symbol == definition.controllerSymbol()) {
            state = BuiltInRegistries.BLOCK.get(definition.controllerId()).defaultBlockState();
        } else {
            List<ItemStack> stacks = definition.visualization().validStacks(symbol, MachineTier.ALL.get(0), definition.controller(), definition.controllerSymbol());
            if (stacks.isEmpty() || !(stacks.get(0).getItem() instanceof BlockItem blockItem)) {
                return null;
            }
            state = blockItem.getBlock().defaultBlockState();
        }

        if (state.hasProperty(MultiblockControllerBlock.FACING)) {
            return state.setValue(MultiblockControllerBlock.FACING, facing);
        }
        if (state.hasProperty(MachinePortBlock.FACING)) {
            return state.setValue(MachinePortBlock.FACING, facing);
        }
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        return state;
    }

    private record Preview(List<PreviewBlock> blocks, long expiresAt) {
    }

    private record PreviewBlock(BlockPos pos, BlockState state) {
    }

    private record LocalPos(int x, int y, int z) {
    }
}
