package net.mads.industron.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.recipe.stone_shaping.StoneShapingPattern;
import net.mads.industron.recipe.stone_shaping.StoneShapingRecipe;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

public final class StoneShapingJeiCategory implements IRecipeCategory<RecipeHolder<StoneShapingRecipe>> {
    public static final RecipeType<RecipeHolder<StoneShapingRecipe>> TYPE = RecipeType.createRecipeHolderType(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "stone_shaping")
    );

    private static final int WIDTH = 176;
    private static final int HEIGHT = 126;
    private static final int PATTERN_X = 40;
    private static final int PATTERN_Y = 22;
    private static final int CELL = 6;
    private static final int PATTERN_SIZE = CELL * StoneShapingPattern.SIZE;
    private final IDrawable icon;

    public StoneShapingJeiCategory(IGuiHelper guiHelper) {
        var pebble = ItemRegistry.getStructureMaterialFormItem(StoneMaterials.STONE, MaterialPart.PEBBLE);
        this.icon = guiHelper.createDrawableItemStack(pebble == null ? ItemStack.EMPTY : new ItemStack(pebble.get()));
    }

    @Override
    public RecipeType<RecipeHolder<StoneShapingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Stone Shaping");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<StoneShapingRecipe> holder, IFocusGroup focuses) {
        StoneShapingRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 26)
                .addItemStack(recipe.mainHand());
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 50)
                .addItemStack(recipe.offHand());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 151, 38)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(
            RecipeHolder<StoneShapingRecipe> holder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        StoneShapingRecipe recipe = holder.value();
        graphics.fill(PATTERN_X - 2, PATTERN_Y - 2, PATTERN_X + PATTERN_SIZE + 2, PATTERN_Y + PATTERN_SIZE + 2, 0xFF555555);
        graphics.fill(PATTERN_X, PATTERN_Y, PATTERN_X + PATTERN_SIZE, PATTERN_Y + PATTERN_SIZE, 0xFF202020);

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(recipe.texture());

        // Draw the real 16x16 stone texture once across the complete pattern area.
        // JEI translates recipe-category rendering on-screen, while GuiGraphics scissor rectangles
        // are not reliable with those local coordinates. The old per-cell scissor approach could
        // therefore clip the entire sprite and leave only the black background. Instead, render
        // the stone texture normally and cover every pixel that is not part of the recipe mask.
        graphics.blit(PATTERN_X, PATTERN_Y, 0, PATTERN_SIZE, PATTERN_SIZE, sprite, 1.0F, 1.0F, 1.0F, 1.0F);
        for (int y = 0; y < StoneShapingPattern.SIZE; y++) {
            for (int x = 0; x < StoneShapingPattern.SIZE; x++) {
                if (recipe.patternPixel(x, y)) continue;
                int cellX = PATTERN_X + x * CELL;
                int cellY = PATTERN_Y + y * CELL;
                graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, 0xFF202020);
            }
        }
    }
}
