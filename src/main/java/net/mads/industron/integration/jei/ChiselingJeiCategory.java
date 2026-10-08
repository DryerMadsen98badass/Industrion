package net.mads.industron.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.mads.industron.Industron;
import net.mads.industron.recipe.chiseling.ChiselingGrid;
import net.mads.industron.recipe.chiseling.ChiselingRecipe;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.recipe.recipetypes.assembly.ToolVariantDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Compact JEI view for the world-interaction Chiseling recipe type. */
public final class ChiselingJeiCategory implements IRecipeCategory<RecipeHolder<ChiselingRecipe>> {
    public static final RecipeType<RecipeHolder<ChiselingRecipe>> TYPE = RecipeType.createRecipeHolderType(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "chiseling")
    );

    private static final int WIDTH = 176;
    private static final int HEIGHT = 138;
    private static final int GRID_X = 61;
    private static final int GRID_Y = 17;
    private static final int CELL = 18;
    private static final int GRID_SIZE = CELL * ChiselingGrid.SIZE;
    private final IDrawable icon;

    public ChiselingJeiCategory(IGuiHelper guiHelper) {
        ItemStack chisel = AssemblyTools.exampleStack(Tool.CHISEL.type());
        this.icon = guiHelper.createDrawableItemStack(chisel);
    }

    @Override
    public RecipeType<RecipeHolder<ChiselingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Chiseling");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ChiselingRecipe> holder, IFocusGroup focuses) {
        ChiselingRecipe recipe = holder.value();

        IRecipeSlotBuilder input = builder.addSlot(RecipeIngredientRole.INPUT, 8, 25)
                .addItemStacks(recipe.baseBlockInputStacks());
        input.addTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.literal("Base Block Input").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Required tier: " + recipe.tier().displayName()).withStyle(ChatFormatting.GOLD));
        });

        IRecipeSlotBuilder hammer = builder.addSlot(RecipeIngredientRole.CATALYST, 8, 85)
                .addItemStack(AssemblyTools.exampleStack(Tool.HAMMER.type()));
        addToolTooltip(hammer, Tool.HAMMER, true, recipe);

        IRecipeSlotBuilder chisel = builder.addSlot(RecipeIngredientRole.CATALYST, 30, 85)
                .addItemStack(AssemblyTools.exampleStack(Tool.CHISEL.type()));
        addToolTooltip(chisel, Tool.CHISEL, false, recipe);

        IRecipeSlotBuilder output = builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 25)
                .addItemStack(recipe.resultStack());
        output.addTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.literal(recipe.hasBlockOutput() ? "Base Block Output" : "Item Output")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(
                    recipe.hasBlockOutput()
                            ? "Placed when the full pattern is completed"
                            : "Dropped when the full pattern is completed"
            ).withStyle(ChatFormatting.DARK_GRAY));
        });

        ItemStack dust = recipe.dustOutputStack();
        if (!dust.isEmpty()) {
            IRecipeSlotBuilder dustSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 51)
                    .addItemStack(dust);
            dustSlot.addTooltipCallback((view, tooltip) -> {
                tooltip.add(Component.literal("Chiseling Byproduct").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Dropped on success").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Invalid pattern: block is lost, byproduct remains")
                        .withStyle(ChatFormatting.DARK_GRAY));
            });
        }
    }

    private static void addToolTooltip(
            IRecipeSlotBuilder slot,
            ToolDefinition tool,
            boolean mainHand,
            ChiselingRecipe recipe
    ) {
        slot.addTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.literal("Tool: any " + tool.type().displayName()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal(mainHand ? "Main hand" : "Off hand").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("Operations: " + recipe.pattern().size()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Required tier: " + recipe.tier().displayName()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Effective tier = lower of Hammer and Chisel")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Uses 1 durability per completed hit").withStyle(ChatFormatting.GRAY));

            ItemStack example = AssemblyTools.exampleStack(tool.type());
            ToolVariantDefinition variant = AssemblyTools.find(tool.type(), example);
            if (variant != null) {
                if (tool == Tool.CHISEL) {
                    double secondsPerHit = variant.useTimeTicks() / 20.0D;
                    double totalSeconds = secondsPerHit * recipe.pattern().size();
                    tooltip.add(Component.literal(
                            example.getHoverName().getString() + ": "
                                    + String.format(Locale.ROOT, "%.2f s/hit", secondsPerHit)
                    ).withStyle(ChatFormatting.DARK_GRAY));
                    tooltip.add(Component.literal(
                            "Example full pattern: " + String.format(Locale.ROOT, "%.2f s", totalSeconds)
                    ).withStyle(ChatFormatting.DARK_GRAY));
                    tooltip.add(Component.literal("Chisel controls hit duration").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    tooltip.add(Component.literal("Hammer does not control duration").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        });
    }

    @Override
    public void draw(
            RecipeHolder<ChiselingRecipe> holder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        ChiselingRecipe recipe = holder.value();
        var font = Minecraft.getInstance().font;

        graphics.drawString(font, "Base", 8, 13, 0xFF8A8A8A, false);
        graphics.drawString(font, "Pattern", GRID_X + 5, 5, 0xFF8A8A8A, false);
        graphics.drawString(font, "Result", 138, 13, 0xFF8A8A8A, false);
        if (!recipe.dustOutputStack().isEmpty()) {
            graphics.drawString(font, "Dust", 148, 72, 0xFF8A8A8A, false);
        }

        // 3x3 target: same numbering as the in-world overlay.
        graphics.fill(GRID_X - 2, GRID_Y - 2, GRID_X + GRID_SIZE + 2, GRID_Y + GRID_SIZE + 2, 0xFF777777);
        for (int row = 0; row < ChiselingGrid.SIZE; row++) {
            for (int column = 0; column < ChiselingGrid.SIZE; column++) {
                int x0 = GRID_X + column * CELL;
                int y0 = GRID_Y + row * CELL;
                graphics.fill(x0, y0, x0 + CELL - 1, y0 + CELL - 1, 0xFF202020);
                int cell = row * ChiselingGrid.SIZE + column + 1;
                String label = Integer.toString(cell);
                int tx = x0 + (CELL - font.width(label)) / 2;
                int ty = y0 + (CELL - font.lineHeight) / 2;
                graphics.drawString(font, label, tx, ty, 0xFFFFFFFF, false);
            }
        }

        graphics.drawString(font, "Tools", 8, 73, 0xFF8A8A8A, false);
        graphics.drawString(font, "Main", 5, 106, 0xFF8A8A8A, false);
        graphics.drawString(font, "Off", 31, 106, 0xFF8A8A8A, false);

        graphics.drawString(font, "Order", 61, 76, 0xFF8A8A8A, false);
        List<Integer> pattern = recipe.pattern();
        for (int row = 0; row < 3; row++) {
            int from = row * 3;
            if (from >= pattern.size()) break;
            int to = Math.min(from + 3, pattern.size());
            String line = joinPattern(pattern.subList(from, to));
            graphics.drawString(font, line, 61, 87 + row * 11, 0xFFE5E5E5, false);
        }

        graphics.drawString(font, "Tier: " + recipe.tier().displayName(), 8, 124,
                recipe.tier().color() | 0xFF000000, false);
        graphics.drawString(font, "Time: Chisel", 105, 124, 0xFFAAAAAA, false);
    }

    private static String joinPattern(List<Integer> cells) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < cells.size(); index++) {
            if (index > 0) result.append(" > ");
            result.append(cells.get(index));
        }
        return result.toString();
    }
}
