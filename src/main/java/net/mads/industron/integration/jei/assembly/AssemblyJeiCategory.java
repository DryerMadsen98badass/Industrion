package net.mads.industron.integration.jei.assembly;

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
import net.mads.industron.recipe.recipetypes.assembly.AssemblyPlan;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyToolType;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipes.assembly.AssemblyRecipes;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Final products only. Recursive component details live in Assembly Components. */
public final class AssemblyJeiCategory implements IRecipeCategory<AssemblyJeiRecipe> {
    public static final RecipeType<AssemblyJeiRecipe> TYPE =
            RecipeType.create(Industron.MOD_ID, "assembly_products", AssemblyJeiRecipe.class);

    private static final int WIDTH = 176;
    private static final int HEIGHT = 92;
    private static final int SLOT_STEP = 20;
    private static final int MAX_COLUMNS = 7;
    private final IDrawable icon;

    public AssemblyJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ItemRegistry.ASSEMBLY_WORKBENCH.get()));
    }

    @Override public RecipeType<AssemblyJeiRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Assembly Products"); }
    @Override public @Nullable IDrawable getBackground() { return null; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblyJeiRecipe jeiRecipe, IFocusGroup focuses) {
        AssemblyRecipeDefinition recipe = jeiRecipe.recipe();
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 12).addItemStack(baseStack(recipe.baseInput()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 12).addItemStack(baseStack(recipe.baseOutput()));
        addDirectInputs(builder, recipe);
    }

    private static void addDirectInputs(IRecipeLayoutBuilder builder, AssemblyRecipeDefinition recipe) {
        int index = 0;
        for (AssemblyRecipeDefinition.RootInput input : recipe.inputs()) {
            if (input.kind() == AssemblyRecipeDefinition.InputKind.WAIT) continue;

            int x = 8 + (index % MAX_COLUMNS) * SLOT_STEP;
            int y = 44 + (index / MAX_COLUMNS) * SLOT_STEP;
            RecipeIngredientRole role = input.kind() == AssemblyRecipeDefinition.InputKind.TOOL
                    ? RecipeIngredientRole.CATALYST
                    : RecipeIngredientRole.INPUT;
            IRecipeSlotBuilder slot = builder.addSlot(role, x, y);

            switch (input.kind()) {
                case MATERIAL -> addMaterialInput(slot, input, false);
                case COMPONENT -> addMaterialInput(slot, input, true);
                case ITEM -> slot.addItemStacks(AssemblyJeiStacks.exactItem(input.itemId(), input.count()));
                case TOOL -> {
                    slot.addItemStacks(AssemblyJeiStacks.tools(input.tool()));
                    addToolTooltip(slot, input.tool(), 1);
                }
                case WAIT -> { }
            }
            index++;
        }
    }

    private static void addMaterialInput(
            IRecipeSlotBuilder slot,
            AssemblyRecipeDefinition.RootInput input,
            boolean componentInput
    ) {
        List<AssemblyPlan.Step> expandedInput = AssemblyPlan.compileInput(input);
        AssemblyPlan.Step representative = expandedInput.stream()
                .filter(step -> step.kind() == AssemblyPlan.Kind.MATERIAL
                        || step.kind() == AssemblyPlan.Kind.ITEM)
                .findFirst()
                .orElse(null);

        if (representative != null && representative.kind() == AssemblyPlan.Kind.MATERIAL) {
            slot.addItemStacks(AssemblyJeiStacks.material(representative, expandedInput, input.count()));
            addRequirementTooltip(slot, input.requirements());
        } else if (representative != null) {
            slot.addItemStacks(AssemblyJeiStacks.exactItem(representative.itemId(), input.count()));
        }

        if (componentInput) {
            addComponentTooltip(slot, input.component().displayName());
        }
    }

    private static void addRequirementTooltip(IRecipeSlotBuilder slot, List<AssemblyRequirement> requirements) {
        if (requirements.isEmpty()) return;
        slot.addTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.literal("Requirements (all must match):").withStyle(ChatFormatting.GOLD));
            requirements.stream().map(AssemblyRequirement::tooltip).forEach(tooltip::add);
        });
    }

    static void addComponentTooltip(IRecipeSlotBuilder slot, String componentName) {
        slot.addTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.literal("Component: " + componentName).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Open recipes to view its direct requirements")
                    .withStyle(ChatFormatting.GRAY));
        });
    }

    static void addToolTooltip(IRecipeSlotBuilder slot, AssemblyToolType toolType, int operations) {
        slot.addTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.literal("Tool: any " + toolType.displayName()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Operations: " + operations).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Uses 1 durability per operation").withStyle(ChatFormatting.GRAY));
            AssemblyTools.all().stream()
                    .filter(tool -> tool.type().equals(toolType))
                    .forEach(tool -> tooltip.add(Component.literal(
                            tool.item().getDescription().getString() + ": "
                                    + String.format(java.util.Locale.ROOT, "%.1f s", tool.useTimeTicks() / 20.0F)
                    ).withStyle(ChatFormatting.DARK_GRAY)));
        });
    }

    @Override
    public void draw(
            AssemblyJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, "Base", 8, 31, 0xFF777777, false);
        graphics.drawString(font, "Result", 138, 31, 0xFF777777, false);
        graphics.drawString(font, "Direct inputs", 8, 70, 0xFF777777, false);
    }

    public static List<AssemblyJeiRecipe> allRecipes() {
        List<AssemblyJeiRecipe> result = new ArrayList<>();
        AssemblyRecipes.ALL.forEach(recipe -> result.add(AssemblyJeiRecipe.recipe(recipe)));
        return List.copyOf(result);
    }

    private static ItemStack baseStack(AssemblyRecipeDefinition.BaseValue value) {
        return switch (value.kind()) {
            case BLOCK -> BuiltInRegistries.BLOCK.getOptional(value.id())
                    .map(block -> new ItemStack(block))
                    .orElse(ItemStack.EMPTY);
            case ITEM -> BuiltInRegistries.ITEM.getOptional(value.id())
                    .map(item -> new ItemStack(item))
                    .orElse(ItemStack.EMPTY);
        };
    }
}
