package net.mads.industron.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.mads.industron.Industron;
import net.mads.industron.network.BindMultiblockSchedulePayload;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockPattern;
import net.mads.industron.machine.machines.electric.multiblock.PatternVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class MultiblockStructureCategory implements IRecipeCategory<MultiblockJeiRecipe> {
    public static final RecipeType<MultiblockJeiRecipe> TYPE = RecipeType.create(Industron.MOD_ID, "multiblock_structure", MultiblockJeiRecipe.class);
    private static final int WIDTH = 250;
    private static final int HEIGHT = 198;
    private static final int VIEW_X = 8;
    private static final int VIEW_Y = 38;
    private static final int VIEW_WIDTH = 134;
    private static final int VIEW_HEIGHT = 104;
    private static final int PANEL_X = 148;
    private static final int PANEL_Y = 38;
    private static final int PANEL_WIDTH = 94;
    private static final int PANEL_HEIGHT = 104;
    private static final int MATERIAL_X = 8;
    private static final int MATERIAL_Y = 158;
    private static final int MATERIAL_WIDTH = 234;
    private static final int MATERIAL_HEIGHT = 36;
    private static final int STACK_COLUMNS = 4;
    private static final int STACK_ROWS = 2;
    private static final int STACK_PAGE_SIZE = STACK_COLUMNS * STACK_ROWS;
    private static final int MATERIAL_COLUMNS = 12;
    private static final int MATERIAL_PAGE_SIZE = MATERIAL_COLUMNS;

    private final IDrawable icon;

    public MultiblockStructureCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<MultiblockJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Multiblock Structures");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Nullable
    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MultiblockJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.validStacks(recipe.definition().controllerSymbol()));
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.allValidStacks());

        List<MultiblockJeiRecipe.MaterialEntry> materials = recipe.materialEntries();
        int end = Math.min(materials.size(), MATERIAL_PAGE_SIZE);
        for (int index = 0; index < end; index++) {
            MultiblockJeiRecipe.MaterialEntry entry = materials.get(index);
            builder.addSlot(RecipeIngredientRole.INPUT, materialStackX(index), materialStackY())
                    .addIngredients(VanillaTypes.ITEM_STACK, entry.stacks())
                    .addTooltipCallback((slotView, tooltip) -> {
                        for (String line : entry.tooltip()) {
                            tooltip.add(Component.literal(line));
                        }
                    })
                    .setStandardSlotBackground();
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MultiblockJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputHandler(new StructureInputHandler(recipe));
    }

    @Override
    public void draw(MultiblockJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        button(guiGraphics, font, "Layer", 8, 16, 44, 14);
        button(guiGraphics, font, "Variant", 56, 16, 52, 14);
        button(guiGraphics, font, recipe.tier().displayName(), 112, 16, 40, 14);
        button(guiGraphics, font, "Save", 156, 16, 46, 14);
        button(guiGraphics, font, "Reset", 206, 16, 42, 14);

        guiGraphics.drawString(font, Component.literal(recipe.definition().displayName()), 8, 4, 0xFF303030, false);
        guiGraphics.drawString(font, Component.literal(recipe.layerDisplay() + "  Variant " + recipe.variant().id()), 8, 32, 0xFF555555, false);

        drawStructure3D(recipe, guiGraphics, font, mouseX, mouseY);
        drawSelection(recipe, guiGraphics, font);
        drawMaterialList(recipe, guiGraphics, font);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, MultiblockJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (inside(mouseX, mouseY, 8, 16, 44, 14)) {
            tooltip.add(Component.literal("Toggle all layers / next layer"));
            return;
        }
        if (inside(mouseX, mouseY, 56, 16, 52, 14)) {
            tooltip.add(Component.literal("Next structure variant"));
            return;
        }
        if (inside(mouseX, mouseY, 112, 16, 40, 14)) {
            tooltip.add(Component.literal("Next machine tier"));
            return;
        }
        if (inside(mouseX, mouseY, 156, 16, 46, 14)) {
            tooltip.add(Component.literal("Save this variant and tier to the held Machine Control Schedule"));
            return;
        }
        if (inside(mouseX, mouseY, 206, 16, 42, 14)) {
            tooltip.add(Component.literal("Reset 3D view"));
            return;
        }
        if (insideSelectedPageButton(mouseX, mouseY, recipe)) {
            tooltip.add(Component.literal("Next valid block page"));
            return;
        }
        if (insideMaterialPageButton(mouseX, mouseY, recipe)) {
            tooltip.add(Component.literal("Next material page"));
            return;
        }

        MultiblockJeiRecipe.ValidBlockEntry selectedEntry = selectedEntryAtMouse(recipe, mouseX, mouseY);
        if (selectedEntry != null) {
            tooltip.add(selectedEntry.stack().getHoverName());
            for (String line : selectedEntry.tooltip()) {
                tooltip.add(infoTooltip(line));
            }
            return;
        }

        MultiblockJeiRecipe.MaterialEntry materialEntry = materialEntryAtMouse(recipe, mouseX, mouseY);
        if (materialEntry != null) {
            tooltip.add(materialEntry.stack().getHoverName());
            for (String line : materialEntry.tooltip()) {
                tooltip.add(infoTooltip(line));
            }
            return;
        }

        PickedBlock block = blockAtMouse(recipe, mouseX, mouseY);
        if (block == null) {
            if (inside(mouseX, mouseY, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT)) {
                tooltip.add(Component.literal("Drag to rotate"));
                tooltip.add(Component.literal("Scroll to zoom"));
            }
            return;
        }

        tooltip.add(Component.literal("Position: " + block.x() + ", " + block.y() + ", " + block.z()).withStyle(ChatFormatting.DARK_GRAY));
        List<String> optionLines = recipe.optionSummaryLines(block.symbol());
        for (String line : optionLines) {
            tooltip.add(infoTooltip(line));
        }
        tooltip.add(Component.literal("Click to inspect").withStyle(ChatFormatting.YELLOW));
    }

    private static void drawStructure3D(MultiblockJeiRecipe recipe, GuiGraphics guiGraphics, Font font, double mouseX, double mouseY) {
        PatternVariant variant = recipe.variant();
        int width = variant.width();
        int rows = variant.height();
        int cols = variant.length();

        guiGraphics.fill(VIEW_X, VIEW_Y, VIEW_X + VIEW_WIDTH, VIEW_Y + VIEW_HEIGHT, 0xFFE0E4EA);
        guiGraphics.fill(VIEW_X + 1, VIEW_Y + 1, VIEW_X + VIEW_WIDTH - 1, VIEW_Y + VIEW_HEIGHT - 1, 0xFFF7F8FA);

        PickedBlock hovered = blockAtMouse(recipe, mouseX, mouseY);
        MultiblockJeiRecipe.SelectedBlock selected = recipe.selectedBlock();

        PoseStack pose = guiGraphics.pose();
        guiGraphics.flush();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();

        pose.pushPose();
        pose.translate(viewCenterX(), viewCenterY(), 260.0F);
        float scale = viewScale(recipe);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(recipe.viewPitch()));
        pose.mulPose(Axis.YP.rotationDegrees(recipe.viewYaw()));
        pose.translate(-width / 2.0F, -rows / 2.0F, -cols / 2.0F);

        for (int x = 0; x < width; x++) {
            if (!recipe.isLayerVisible(x)) {
                continue;
            }

            for (int y = 0; y < rows; y++) {
                for (int z = 0; z < cols; z++) {
                    char symbol = variant.symbolAt(x, y, z);
                    if (symbol == MultiblockPattern.air || symbol == MultiblockPattern.ignore) {
                        continue;
                    }

                    BlockState state = renderState(recipe, symbol, x, y, z);
                    if (state == null) {
                        continue;
                    }

                    pose.pushPose();
                    pose.translate(x, y, z);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                            state,
                            pose,
                            guiGraphics.bufferSource(),
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY
                    );
                    pose.popPose();
                }
            }
        }

        guiGraphics.flush();

        if (selected != null && recipe.isLayerVisible(selected.x())) {
            drawBlockOutline(pose, guiGraphics, selected.x(), selected.y(), selected.z(), 0xFFFFC857);
        }
        if (hovered != null) {
            drawBlockOutline(pose, guiGraphics, hovered.x(), hovered.y(), hovered.z(), 0xFF4D8DFF);
        }

        guiGraphics.flush();
        pose.popPose();
        Lighting.setupForFlatItems();
        RenderSystem.disableDepthTest();

        guiGraphics.drawString(font, Component.literal("Drag view  Scroll zoom"), VIEW_X + 4, VIEW_Y + VIEW_HEIGHT + 4, 0xFF555555, false);
    }

    @Nullable
    private static BlockState renderState(MultiblockJeiRecipe recipe, char symbol, int x, int y, int z) {
        ItemStack stack = recipe.previewStack(x, y, z, symbol);
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.getBlock() instanceof MachinePortBlock && state.hasProperty(MachinePortBlock.FACING)) {
            state = state.setValue(MachinePortBlock.FACING, abilityFacing(recipe, x, y, z));
        }
        return state;
    }

    private static Direction abilityFacing(MultiblockJeiRecipe recipe, int x, int y, int z) {
        PatternVariant variant = recipe.variant();
        double centerX = variant.width() / 2.0D;
        double centerY = variant.height() / 2.0D;
        double centerZ = variant.length() / 2.0D;
        double dx = x + 0.5D - centerX;
        double dy = y + 0.5D - centerY;
        double dz = z + 0.5D - centerZ;
        List<Direction> remaining = new java.util.ArrayList<>(List.of(Direction.values()));

        while (!remaining.isEmpty()) {
            double bestScore = remaining.stream()
                    .mapToDouble(direction -> dx * direction.getStepX() + dy * direction.getStepY() + dz * direction.getStepZ())
                    .max()
                    .orElse(-Double.MAX_VALUE);
            List<Direction> scoreGroup = remaining.stream()
                    .filter(direction -> Math.abs(dx * direction.getStepX() + dy * direction.getStepY() + dz * direction.getStepZ() - bestScore) <= 1.0E-7D)
                    .toList();

            int bestAir = 0;
            List<Direction> airWinners = new java.util.ArrayList<>();
            for (Direction direction : scoreGroup) {
                int air = consecutivePatternAirAhead(variant, x, y, z, direction, 64);
                if (air <= 0) {
                    continue;
                }
                if (air > bestAir) {
                    bestAir = air;
                    airWinners.clear();
                    airWinners.add(direction);
                } else if (air == bestAir) {
                    airWinners.add(direction);
                }
            }

            if (!airWinners.isEmpty()) {
                if (airWinners.size() == 1) {
                    return airWinners.getFirst();
                }
                int randomIndex = Math.floorMod(
                        java.util.Objects.hash(System.identityHashCode(recipe), variant.id(), x, y, z, Double.doubleToLongBits(bestScore)),
                        airWinners.size()
                );
                return airWinners.get(randomIndex);
            }

            remaining.removeAll(scoreGroup);
        }

        Direction[] directions = Direction.values();
        int randomIndex = Math.floorMod(
                java.util.Objects.hash(System.identityHashCode(recipe), variant.id(), x, y, z),
                directions.length
        );
        return directions[randomIndex];
    }

    private static int consecutivePatternAirAhead(PatternVariant variant, int x, int y, int z, Direction direction, int maximumDistance) {
        for (int distance = 1; distance <= maximumDistance; distance++) {
            int checkX = x + direction.getStepX() * distance;
            int checkY = y + direction.getStepY() * distance;
            int checkZ = z + direction.getStepZ() * distance;
            char symbol = variant.symbolAt(checkX, checkY, checkZ);
            if (symbol != MultiblockPattern.air && symbol != MultiblockPattern.ignore) {
                return distance - 1;
            }
        }
        return maximumDistance;
    }

    private static void drawSelection(MultiblockJeiRecipe recipe, GuiGraphics guiGraphics, Font font) {
        guiGraphics.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_WIDTH, PANEL_Y + PANEL_HEIGHT, 0xFFE5E8EC);
        guiGraphics.fill(PANEL_X + 1, PANEL_Y + 1, PANEL_X + PANEL_WIDTH - 1, PANEL_Y + PANEL_HEIGHT - 1, 0xFFF8F9FA);

        MultiblockJeiRecipe.SelectedBlock selected = recipe.selectedBlock();
        if (selected == null) {
            guiGraphics.drawString(font, Component.literal("Valid Blocks"), PANEL_X + 5, PANEL_Y + 5, 0xFF303030, false);
            guiGraphics.drawString(font, Component.literal("Click a block"), PANEL_X + 5, PANEL_Y + 23, 0xFF777777, false);
            return;
        }

        List<MultiblockJeiRecipe.ValidBlockEntry> entries = recipe.validBlockEntries(selected.symbol());
        guiGraphics.drawString(font, Component.literal("Valid Blocks"), PANEL_X + 5, PANEL_Y + 5, 0xFF303030, false);
        guiGraphics.drawString(font, Component.literal("Pos " + selected.x() + "," + selected.y() + "," + selected.z()), PANEL_X + 5, PANEL_Y + 18, 0xFF666666, false);

        int pages = Math.max(1, (entries.size() + STACK_PAGE_SIZE - 1) / STACK_PAGE_SIZE);
        int page = Math.min(recipe.selectedStackPage(), pages - 1);
        int start = page * STACK_PAGE_SIZE;
        int end = Math.min(entries.size(), start + STACK_PAGE_SIZE);

        for (int index = start; index < end; index++) {
            int local = index - start;
            MultiblockJeiRecipe.ValidBlockEntry entry = entries.get(index);
            int x = stackX(local);
            int y = stackY(local);
            ItemStack stack = entry.stack();
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(font, stack, x, y);
        }

        if (pages > 1) {
            guiGraphics.drawString(font, Component.literal((page + 1) + "/" + pages), PANEL_X + 5, PANEL_Y + PANEL_HEIGHT - 13, 0xFF555555, false);
            button(guiGraphics, font, "More", PANEL_X + PANEL_WIDTH - 35, PANEL_Y + PANEL_HEIGHT - 16, 30, 12);
        }
    }

    private static void drawMaterialList(MultiblockJeiRecipe recipe, GuiGraphics guiGraphics, Font font) {
        guiGraphics.fill(MATERIAL_X, MATERIAL_Y, MATERIAL_X + MATERIAL_WIDTH, MATERIAL_Y + MATERIAL_HEIGHT, 0xFFE5E8EC);
        guiGraphics.fill(MATERIAL_X + 1, MATERIAL_Y + 1, MATERIAL_X + MATERIAL_WIDTH - 1, MATERIAL_Y + MATERIAL_HEIGHT - 1, 0xFFF8F9FA);
        guiGraphics.drawString(font, Component.literal("Materials"), MATERIAL_X + 5, MATERIAL_Y + 5, 0xFF303030, false);
        List<MultiblockJeiRecipe.MaterialEntry> materials = recipe.materialEntries();
        if (materials.isEmpty()) {
            guiGraphics.drawString(font, Component.literal("No material entries"), MATERIAL_X + 5, MATERIAL_Y + 19, 0xFF777777, false);
            return;
        }

    }

    private static Component infoTooltip(String line) {
        ChatFormatting color;
        if (line.startsWith("Ability:")) {
            color = ChatFormatting.AQUA;
        } else if (line.startsWith("Minimum:")) {
            color = ChatFormatting.GREEN;
        } else if (line.startsWith("Maximum:")) {
            color = ChatFormatting.GOLD;
        } else if (line.startsWith("Tier:")) {
            color = ChatFormatting.LIGHT_PURPLE;
        } else if (line.startsWith("Sequential")) {
            color = ChatFormatting.YELLOW;
        } else if (line.startsWith("Total:")) {
            color = ChatFormatting.WHITE;
        } else {
            color = ChatFormatting.GRAY;
        }
        return Component.literal(line).withStyle(color);
    }

    private static void button(GuiGraphics guiGraphics, Font font, String label, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF606A78);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFECEFF3);
        int textX = x + Math.max(2, (width - font.width(label)) / 2);
        guiGraphics.drawString(font, label, textX, y + Math.max(2, (height - 8) / 2), 0xFF22262C, false);
    }

    @Nullable
    private static PickedBlock blockAtMouse(MultiblockJeiRecipe recipe, double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT)) {
            return null;
        }

        PatternVariant variant = recipe.variant();
        ViewRay ray = viewRay(recipe, mouseX, mouseY);
        PickedBlock best = null;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int x = 0; x < variant.width(); x++) {
            if (!recipe.isLayerVisible(x)) {
                continue;
            }

            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    char symbol = variant.symbolAt(x, y, z);
                    if (symbol == MultiblockPattern.air || symbol == MultiblockPattern.ignore) {
                        continue;
                    }
                    List<ItemStack> stacks = recipe.validStacks(symbol);
                    if (stacks.isEmpty() || !(stacks.getFirst().getItem() instanceof BlockItem)) {
                        continue;
                    }

                    double distance = rayBoxDistance(ray, x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
                    if (distance < 0.0D || distance >= bestDistance) {
                        continue;
                    }

                    bestDistance = distance;
                    best = new PickedBlock(x, y, z, symbol);
                }
            }
        }

        return best;
    }

    private static ViewRay viewRay(MultiblockJeiRecipe recipe, double mouseX, double mouseY) {
        PatternVariant variant = recipe.variant();
        double scale = viewScale(recipe);
        double viewX = (mouseX - viewCenterX()) / scale;
        double viewY = -(mouseY - viewCenterY()) / scale;
        double yaw = Math.toRadians(recipe.viewYaw());
        double pitch = Math.toRadians(recipe.viewPitch());
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double frontDepth = Math.max(variant.width(), Math.max(variant.height(), variant.length())) * 2.0D + 64.0D;

        double rotatedZ = -viewY * sinPitch + frontDepth * cosPitch;
        double originX = viewX * cosYaw - rotatedZ * sinYaw + variant.width() / 2.0D;
        double originY = viewY * cosPitch + frontDepth * sinPitch + variant.height() / 2.0D;
        double originZ = viewX * sinYaw + rotatedZ * cosYaw + variant.length() / 2.0D;

        double directionX = cosPitch * sinYaw;
        double directionY = -sinPitch;
        double directionZ = -cosPitch * cosYaw;
        return new ViewRay(originX, originY, originZ, directionX, directionY, directionZ);
    }

    private static double rayBoxDistance(
            ViewRay ray,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        double near = 0.0D;
        double far = Double.POSITIVE_INFINITY;

        double direction = ray.directionX();
        if (Math.abs(direction) < 1.0E-9D) {
            if (ray.originX() < minX || ray.originX() > maxX) {
                return -1.0D;
            }
        } else {
            double first = (minX - ray.originX()) / direction;
            double second = (maxX - ray.originX()) / direction;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            near = Math.max(near, first);
            far = Math.min(far, second);
            if (near > far) {
                return -1.0D;
            }
        }

        direction = ray.directionY();
        if (Math.abs(direction) < 1.0E-9D) {
            if (ray.originY() < minY || ray.originY() > maxY) {
                return -1.0D;
            }
        } else {
            double first = (minY - ray.originY()) / direction;
            double second = (maxY - ray.originY()) / direction;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            near = Math.max(near, first);
            far = Math.min(far, second);
            if (near > far) {
                return -1.0D;
            }
        }

        direction = ray.directionZ();
        if (Math.abs(direction) < 1.0E-9D) {
            if (ray.originZ() < minZ || ray.originZ() > maxZ) {
                return -1.0D;
            }
        } else {
            double first = (minZ - ray.originZ()) / direction;
            double second = (maxZ - ray.originZ()) / direction;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            near = Math.max(near, first);
            far = Math.min(far, second);
            if (near > far) {
                return -1.0D;
            }
        }

        return far < 0.0D ? -1.0D : near;
    }

    private static float viewScale(MultiblockJeiRecipe recipe) {
        PatternVariant variant = recipe.variant();
        double baseSize = Math.max(1.0D, Math.max(variant.width(), variant.length()) * 0.95D + variant.height() * 0.55D);
        float fitted = (float) Math.min(VIEW_WIDTH / (baseSize * 1.45D), VIEW_HEIGHT / (baseSize * 1.35D));
        return Math.max(7.0F, fitted) * recipe.zoom();
    }

    private static float viewCenterX() {
        return VIEW_X + VIEW_WIDTH / 2.0F;
    }

    private static float viewCenterY() {
        return VIEW_Y + VIEW_HEIGHT / 2.0F + 10.0F;
    }

    private static void drawBlockOutline(PoseStack pose, GuiGraphics guiGraphics, int x, int y, int z, int color) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        double expand = 0.003D;
        AABB box = new AABB(
                x - expand,
                y - expand,
                z - expand,
                x + 1.0D + expand,
                y + 1.0D + expand,
                z + 1.0D + expand
        );
        LevelRenderer.renderLineBox(
                pose,
                guiGraphics.bufferSource().getBuffer(RenderType.lines()),
                box,
                red,
                green,
                blue,
                alpha
        );
    }

    @Nullable
    private static MultiblockJeiRecipe.ValidBlockEntry selectedEntryAtMouse(MultiblockJeiRecipe recipe, double mouseX, double mouseY) {
        MultiblockJeiRecipe.SelectedBlock selected = recipe.selectedBlock();
        if (selected == null) {
            return null;
        }

        List<MultiblockJeiRecipe.ValidBlockEntry> entries = recipe.validBlockEntries(selected.symbol());
        int page = Math.min(recipe.selectedStackPage(), Math.max(0, (entries.size() + STACK_PAGE_SIZE - 1) / STACK_PAGE_SIZE - 1));
        int start = page * STACK_PAGE_SIZE;
        int end = Math.min(entries.size(), start + STACK_PAGE_SIZE);
        for (int index = start; index < end; index++) {
            int local = index - start;
            if (inside(mouseX, mouseY, stackX(local), stackY(local), 16, 16)) {
                return entries.get(index);
            }
        }
        return null;
    }

    @Nullable
    private static MultiblockJeiRecipe.MaterialEntry materialEntryAtMouse(MultiblockJeiRecipe recipe, double mouseX, double mouseY) {
        if (recipe.selectedBlock() != null) {
            return null;
        }

        List<MultiblockJeiRecipe.MaterialEntry> materials = recipe.materialEntries();
        int end = Math.min(materials.size(), MATERIAL_PAGE_SIZE);
        for (int index = 0; index < end; index++) {
            if (inside(mouseX, mouseY, materialStackX(index), materialStackY(), 16, 16)) {
                return materials.get(index);
            }
        }
        return null;
    }

    private static int stackX(int localIndex) {
        return PANEL_X + 5 + (localIndex % STACK_COLUMNS) * 21;
    }

    private static int stackY(int localIndex) {
        return PANEL_Y + 34 + (localIndex / STACK_COLUMNS) * 22;
    }

    private static int materialStackX(int localIndex) {
        return MATERIAL_X + 5 + localIndex * 18;
    }

    private static int materialStackY() {
        return MATERIAL_Y + 18;
    }

    private static boolean insideSelectedPageButton(double mouseX, double mouseY, MultiblockJeiRecipe recipe) {
        MultiblockJeiRecipe.SelectedBlock selected = recipe.selectedBlock();
        if (selected == null || recipe.validBlockEntries(selected.symbol()).size() <= STACK_PAGE_SIZE) {
            return false;
        }
        return inside(mouseX, mouseY, PANEL_X + PANEL_WIDTH - 35, PANEL_Y + PANEL_HEIGHT - 16, 30, 12);
    }

    private static boolean insideMaterialPageButton(double mouseX, double mouseY, MultiblockJeiRecipe recipe) {
        return false;
    }

    private static void saveToHeldSchedule(MultiblockJeiRecipe recipe) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        InteractionHand hand;
        if (minecraft.player.getMainHandItem().is(ItemRegistry.MACHINE_CONTROL_SCHEDULE.get())) {
            hand = InteractionHand.MAIN_HAND;
        } else if (minecraft.player.getOffhandItem().is(ItemRegistry.MACHINE_CONTROL_SCHEDULE.get())) {
            hand = InteractionHand.OFF_HAND;
        } else {
            minecraft.player.displayClientMessage(Component.literal("Hold a Machine Control Schedule first"), true);
            return;
        }

        PacketDistributor.sendToServer(new BindMultiblockSchedulePayload(
                recipe.definition().id(),
                recipe.variant().id(),
                recipe.tier().id(),
                hand.ordinal()
        ));
        minecraft.player.displayClientMessage(
                Component.literal("Saved " + recipe.definition().displayName() + " variant " + recipe.variant().id() + " (" + recipe.tier().displayName() + ")"),
                true
        );
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record PickedBlock(int x, int y, int z, char symbol) {
    }

    private record ViewRay(
            double originX,
            double originY,
            double originZ,
            double directionX,
            double directionY,
            double directionZ
    ) {
    }

    private static final class StructureInputHandler implements IJeiInputHandler {
        private final MultiblockJeiRecipe recipe;
        private boolean dragged;

        private StructureInputHandler(MultiblockJeiRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(0, 0, WIDTH, HEIGHT);
        }

        @Override
        public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
            InputConstants.Key key = input.getKey();
            if (key.getType() != InputConstants.Type.MOUSE || key.getValue() != 0) {
                return false;
            }

            boolean canHandle = clickable(mouseX, mouseY);
            if (input.isSimulate() || !canHandle) {
                return canHandle;
            }

            if (inside(mouseX, mouseY, 8, 16, 44, 14)) {
                recipe.nextLayer();
                dragged = false;
                return true;
            }
            if (inside(mouseX, mouseY, 56, 16, 52, 14)) {
                recipe.nextVariant();
                dragged = false;
                return true;
            }
            if (inside(mouseX, mouseY, 112, 16, 40, 14)) {
                recipe.nextTier();
                dragged = false;
                return true;
            }
            if (inside(mouseX, mouseY, 156, 16, 46, 14)) {
                saveToHeldSchedule(recipe);
                dragged = false;
                return true;
            }
            if (inside(mouseX, mouseY, 206, 16, 42, 14)) {
                recipe.resetView();
                dragged = false;
                return true;
            }
            if (insideSelectedPageButton(mouseX, mouseY, recipe)) {
                recipe.nextSelectedStackPage(STACK_PAGE_SIZE);
                dragged = false;
                return true;
            }
            if (insideMaterialPageButton(mouseX, mouseY, recipe)) {
                recipe.nextMaterialPage(MATERIAL_PAGE_SIZE);
                dragged = false;
                return true;
            }
            if (dragged) {
                dragged = false;
                return true;
            }

            PickedBlock block = blockAtMouse(recipe, mouseX, mouseY);
            if (block != null) {
                recipe.selectBlock(block.x(), block.y(), block.z(), block.symbol());
                return true;
            }
            return false;
        }

        @Override
        public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            if (!inside(mouseX, mouseY, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT)) {
                return false;
            }

            recipe.zoom(scrollDeltaY);
            return true;
        }

        @Override
        public boolean handleMouseDragged(double mouseX, double mouseY, InputConstants.Key mouseKey, double dragX, double dragY) {
            if (mouseKey.getType() != InputConstants.Type.MOUSE || mouseKey.getValue() != 0) {
                return false;
            }
            if (!inside(mouseX, mouseY, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT)) {
                return false;
            }

            recipe.dragView(dragX, dragY);
            dragged = true;
            return true;
        }

        private boolean clickable(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, 8, 16, 44, 14)
                    || inside(mouseX, mouseY, 56, 16, 52, 14)
                    || inside(mouseX, mouseY, 112, 16, 40, 14)
                    || inside(mouseX, mouseY, 156, 16, 46, 14)
                    || inside(mouseX, mouseY, 206, 16, 42, 14)
                    || insideSelectedPageButton(mouseX, mouseY, recipe)
                    || insideMaterialPageButton(mouseX, mouseY, recipe)
                    || inside(mouseX, mouseY, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT);
        }
    }
}