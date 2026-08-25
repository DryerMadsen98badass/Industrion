package net.mads.industron.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.RaycastHelper.PredicateTraceResult;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.mads.industron.Industron;
import net.mads.industron.item.MultiblockDevToolItem;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.network.MultiblockDevToolActionPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class MultiblockDevToolSelectionHandler {
    private static final Object SELECTION_OUTLINE = new Object();
    private static final Object CONTROLLER_OUTLINE = new Object();
    private static final int YELLOW = 0xF5D142;
    private static final int CONTROLLER_YELLOW = 0xFFF176;

    private static BlockPos selectedPos;
    private static BlockPos selectedBlock;
    private static Direction selectedFace;
    private static int range = 10;
    private static ToolMode mode = ToolMode.SELECT_AREA;
    private static boolean menuFocused;
    private static float menuOffset;

    private MultiblockDevToolSelectionHandler() {
    }

    public enum ToolMode {
        SELECT_AREA("Select Area", AllIcons.I_TARGET, List.of(
                "Right-click to select corners",
                "Hold Ctrl to target in air",
                "Ctrl + Scroll changes range / selected face"
        )),
        SET_CONTROLLER("Set Controller", AllIcons.I_TOOL_DEPLOY, List.of(
                "Right-click the controller block",
                "The controller must be inside the selection"
        )),
        SAVE("Save", AllIcons.I_CONFIRM, List.of(
                "Right-click to capture the multiblock",
                "Provides Copy Pattern and Copy Full Variant"
        ));

        private final String displayName;
        private final AllIcons icon;
        private final List<String> description;

        ToolMode(String displayName, AllIcons icon, List<String> description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tick();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || !isActive()) {
            return;
        }
        if (!AllKeys.TOOL_MENU.doesModifierAndCodeMatch(event.getKey())) {
            return;
        }

        boolean pressed = event.getAction() != 0;
        menuFocused = pressed;
    }

    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null || !isActive()) {
            return;
        }

        double delta = event.getScrollDeltaY();
        if (menuFocused) {
            cycleMode(delta);
            event.setCanceled(true);
            return;
        }

        if (AllKeys.ctrlDown() && mode == ToolMode.SELECT_AREA && handleSelectionScroll(delta)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (Minecraft.getInstance().screen != null || !isActive()) {
            return;
        }
        if (event.getAction() == 0 || event.getButton() != 1) {
            return;
        }

        if (handleRightClick()) {
            event.setCanceled(true);
        }
    }

    public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!isActive() || Minecraft.getInstance().options.hideGui) {
            return;
        }

        menuOffset += ((menuFocused ? 10F : 0F) - menuOffset) * 0.1F;
        drawToolMenu(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isActive()) {
            selectedPos = null;
            selectedBlock = null;
            selectedFace = null;
            menuFocused = false;
            menuOffset *= 0.9F;
            return;
        }

        LocalPlayer player = minecraft.player;
        ItemStack stack = player.getMainHandItem();
        MultiblockDevToolItem.Selection selection = MultiblockDevToolItem.selection(stack);

        BlockHitResult trace = RaycastHelper.rayTraceRange(player.level(), player, 75);
        selectedBlock = trace != null && trace.getType() == Type.BLOCK ? trace.getBlockPos().immutable() : null;

        if (mode == ToolMode.SELECT_AREA && AllKeys.ACTIVATE_TOOL.isPressed()) {
            float partialTicks = AnimationTickHolder.getPartialTicks();
            Vec3 target = player.getEyePosition(partialTicks).add(player.getLookAngle().scale(range));
            selectedPos = BlockPos.containing(target);
        } else if (trace != null && trace.getType() == Type.BLOCK) {
            BlockPos hit = trace.getBlockPos();
            if (mode == ToolMode.SELECT_AREA) {
                boolean replaceable = player.level().getBlockState(hit)
                        .canBeReplaced(new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, trace)));
                if (trace.getDirection().getAxis().isVertical() && !replaceable) {
                    hit = hit.relative(trace.getDirection());
                }
            }
            selectedPos = hit.immutable();
        } else {
            selectedPos = null;
        }

        selectedFace = null;
        if (mode == ToolMode.SELECT_AREA && selection.pos1() != null && selection.pos2() != null) {
            AABB bb = inclusiveBox(selection.pos1(), selection.pos2()).inflate(0.45F);
            Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
            boolean inside = bb.contains(camera);
            PredicateTraceResult result = RaycastHelper.rayTraceUntil(player, 70, pos -> inside ^ bb.contains(VecHelper.getCenterOf(pos)));
            selectedFace = result.missed() ? null : inside ? result.getFacing().getOpposite() : result.getFacing();
        }

        AABB selectionBox = currentSelectionBox(selection);
        if (selectionBox != null) {
            Outliner.getInstance().chaseAABB(SELECTION_OUTLINE, selectionBox)
                    .colored(YELLOW)
                    .withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                    .lineWidth(1 / 16F)
                    .highlightFace(mode == ToolMode.SELECT_AREA ? selectedFace : null);
        }

        if (selection.controller() != null) {
            Outliner.getInstance().chaseAABB(CONTROLLER_OUTLINE, new AABB(selection.controller()))
                    .colored(CONTROLLER_YELLOW)
                    .withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                    .lineWidth(2 / 16F);
        }
    }

    private static boolean handleRightClick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ItemStack stack = player.getMainHandItem();
        MultiblockDevToolItem.Selection selection = MultiblockDevToolItem.selection(stack);

        if (mode == ToolMode.SELECT_AREA) {
            if (player.isShiftKeyDown()) {
                MultiblockDevToolItem.clearSelection(stack);
                PacketDistributor.sendToServer(new MultiblockDevToolActionPayload(
                        MultiblockDevToolActionPayload.CLEAR, BlockPos.ZERO, BlockPos.ZERO
                ));
                return true;
            }
            if (selection.pos2() != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Selection complete - use Alt + Scroll and choose Save or Set Controller"), true);
                return true;
            }
            if (selectedPos == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("No target"), true);
                return true;
            }
            if (selection.pos1() == null) {
                MultiblockDevToolItem.setPos1(stack, selectedPos);
                PacketDistributor.sendToServer(new MultiblockDevToolActionPayload(
                        MultiblockDevToolActionPayload.SET_POS_1, selectedPos, selectedPos
                ));
            } else {
                MultiblockDevToolItem.setPos2(stack, selectedPos);
                PacketDistributor.sendToServer(new MultiblockDevToolActionPayload(
                        MultiblockDevToolActionPayload.SET_POS_2, selectedPos, selectedPos
                ));
            }
            return true;
        }

        if (mode == ToolMode.SET_CONTROLLER) {
            if (selectedBlock == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Look at the controller block"), true);
                return true;
            }
            BlockState state = player.level().getBlockState(selectedBlock);
            Direction facing = state.getBlock() instanceof MultiblockControllerBlock
                    ? state.getValue(MultiblockControllerBlock.FACING)
                    : player.getDirection().getOpposite();
            MultiblockDevToolItem.setController(stack, selectedBlock, facing);
            PacketDistributor.sendToServer(new MultiblockDevToolActionPayload(
                    MultiblockDevToolActionPayload.SET_CONTROLLER, selectedBlock, selectedBlock
            ));
            return true;
        }

        if (!selection.complete()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Select both corners and a controller before saving"
            ), true);
            return true;
        }

        PacketDistributor.sendToServer(new MultiblockDevToolActionPayload(
                MultiblockDevToolActionPayload.EXPORT, BlockPos.ZERO, BlockPos.ZERO
        ));
        mode = ToolMode.SELECT_AREA;
        range = 10;
        selectedPos = null;
        selectedBlock = null;
        selectedFace = null;
        menuFocused = false;
        return true;
    }

    private static boolean handleSelectionScroll(double delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ItemStack stack = player.getMainHandItem();
        MultiblockDevToolItem.Selection selection = MultiblockDevToolItem.selection(stack);

        if (selection.pos2() == null) {
            range = (int) Mth.clamp(range + delta, 1, 100);
            return true;
        }
        if (selection.pos1() == null || selectedFace == null) {
            return true;
        }

        AABB bb = new AABB(Vec3.atLowerCornerOf(selection.pos1()), Vec3.atLowerCornerOf(selection.pos2()));
        Vec3i vec = selectedFace.getNormal();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        if (inclusiveBox(selection.pos1(), selection.pos2()).contains(camera)) {
            delta *= -1;
        }

        int intDelta = (int) (delta > 0 ? Math.ceil(delta) : Math.floor(delta));
        int x = vec.getX() * intDelta;
        int y = vec.getY() * intDelta;
        int z = vec.getZ() * intDelta;

        AxisDirection axisDirection = selectedFace.getAxisDirection();
        if (axisDirection == AxisDirection.NEGATIVE) {
            bb = bb.move(-x, -y, -z);
        }

        double maxX = Math.max(bb.maxX - x * axisDirection.getStep(), bb.minX);
        double maxY = Math.max(bb.maxY - y * axisDirection.getStep(), bb.minY);
        double maxZ = Math.max(bb.maxZ - z * axisDirection.getStep(), bb.minZ);
        bb = new AABB(bb.minX, bb.minY, bb.minZ, maxX, maxY, maxZ);

        BlockPos first = BlockPos.containing(bb.minX, bb.minY, bb.minZ);
        BlockPos second = BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ);
        MultiblockDevToolItem.setPos1(stack, first);
        MultiblockDevToolItem.setPos2(stack, second);
        PacketDistributor.sendToServer(new MultiblockDevToolActionPayload(
                MultiblockDevToolActionPayload.SET_BOUNDS, first, second
        ));

        int sizeX = Math.abs(second.getX() - first.getX()) + 1;
        int sizeY = Math.abs(second.getY() - first.getY()) + 1;
        int sizeZ = Math.abs(second.getZ() - first.getZ()) + 1;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(sizeX + " x " + sizeY + " x " + sizeZ), true);
        return true;
    }

    private static AABB currentSelectionBox(MultiblockDevToolItem.Selection selection) {
        if (selection.pos2() == null) {
            if (selection.pos1() == null) {
                return selectedPos == null ? null : new AABB(selectedPos);
            }
            return selectedPos == null ? new AABB(selection.pos1()) : inclusiveBox(selection.pos1(), selectedPos);
        }
        return inclusiveBox(selection.pos1(), selection.pos2());
    }

    private static AABB inclusiveBox(BlockPos first, BlockPos second) {
        return new AABB(Vec3.atLowerCornerOf(first), Vec3.atLowerCornerOf(second)).expandTowards(1, 1, 1);
    }

    private static void cycleMode(double delta) {
        ToolMode[] modes = ToolMode.values();
        int index = mode.ordinal() + (delta < 0 ? 1 : -1);
        index = (index + modes.length) % modes.length;
        mode = modes[index];
    }

    private static void drawToolMenu(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        ToolMode[] modes = ToolMode.values();
        int width = Math.max(modes.length * 50 + 30, 220);
        int height = 30;
        int x = (window.getGuiScaledWidth() - width) / 2 + 15;
        int y = window.getGuiScaledHeight() - height - 75;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, -menuOffset, menuFocused ? 100 : 0);

        AllGuiTextures background = AllGuiTextures.HUD_BACKGROUND;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, menuFocused ? 7 / 8F : 1 / 2F);
        graphics.blit(background.location, x - 15, y, background.getStartX(), background.getStartY(), width, height, background.getWidth(), background.getHeight());

        float descriptionAlpha = menuOffset / 10F;
        int alpha = ((int) (descriptionAlpha * 0xFF)) << 24;
        if (descriptionAlpha > 0.25F) {
            RenderSystem.setShaderColor(0.95F, 0.82F, 0.25F, descriptionAlpha);
            graphics.blit(background.location, x - 15, y + 33, background.getStartX(), background.getStartY(), width, height + 22, background.getWidth(), background.getHeight());
            RenderSystem.setShaderColor(1, 1, 1, 1);
            List<String> description = mode.description;
            for (int i = 0; i < description.size() && i < 4; i++) {
                graphics.drawString(minecraft.font, description.get(i), x - 10, y + 38 + i * 11, YELLOW + alpha, false);
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        String keyName = AllKeys.TOOL_MENU.getBoundKey();
        String hint = menuFocused ? "Scroll to Cycle" : "Hold " + keyName + " to Focus";
        graphics.drawCenteredString(minecraft.font, hint, window.getGuiScaledWidth() / 2, y - 10, 0xF5D142);

        for (int i = 0; i < modes.length; i++) {
            ToolMode tool = modes[i];
            pose.pushPose();
            float iconAlpha = menuFocused ? 1F : 0.2F;
            if (tool == mode) {
                pose.translate(0, -10, 0);
                graphics.drawCenteredString(minecraft.font, tool.displayName, x + i * 50 + 24, y + 28, 0xF5D142);
                iconAlpha = 1F;
            }
            RenderSystem.setShaderColor(0.85F, 0.65F, 0.05F, iconAlpha);
            tool.icon.render(graphics, x + i * 50 + 16, y + 12);
            RenderSystem.setShaderColor(1F, 0.9F, 0.3F, iconAlpha);
            tool.icon.render(graphics, x + i * 50 + 16, y + 11);
            pose.popPose();
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static boolean isActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && minecraft.player.getMainHandItem().getItem() instanceof MultiblockDevToolItem;
    }
}
