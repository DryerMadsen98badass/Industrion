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
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Displays exactly one ComponentDefinition level so JEI navigation preserves quantities. */
public final class AssemblyComponentJeiCategory implements IRecipeCategory<AssemblyComponentJeiRecipe> {
    public static final RecipeType<AssemblyComponentJeiRecipe> TYPE =
            RecipeType.create(Industron.MOD_ID, "assembly_components", AssemblyComponentJeiRecipe.class);

    private static final int WIDTH = 176;
    private static final int HEIGHT = 92;
    private static final int SLOT_STEP = 20;
    private static final int MAX_COLUMNS = 7;
    private final IDrawable icon;

    public AssemblyComponentJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Blocks.SMITHING_TABLE));
    }

    @Override public RecipeType<AssemblyComponentJeiRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Assembly Components"); }
    @Override public @Nullable IDrawable getBackground() { return null; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            AssemblyComponentJeiRecipe recipe,
            IFocusGroup focuses
    ) {
        ComponentDefinition definition = recipe.definition();
        int representativeIndex = AssemblyJeiStacks.representativeIndex(definition);
        if (representativeIndex < 0) return;

        IRecipeSlotBuilder output = builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 12);
        output.addItemStacks(AssemblyJeiStacks.component(
                definition.component(),
                null,
                recipe.material(),
                1
        ));
        AssemblyJeiCategory.addComponentTooltip(output, definition.component().displayName());

        int displayedInput = 0;
        for (int stepIndex = 0; stepIndex < definition.steps().size(); stepIndex++) {
            if (stepIndex == representativeIndex) continue;
            ComponentDefinition.Step step = definition.steps().get(stepIndex);
            if (step.kind() == ComponentDefinition.StepKind.WAIT) continue;

            int x = 8 + (displayedInput % MAX_COLUMNS) * SLOT_STEP;
            int y = 44 + (displayedInput / MAX_COLUMNS) * SLOT_STEP;
            RecipeIngredientRole role = step.kind() == ComponentDefinition.StepKind.TOOL
                    ? RecipeIngredientRole.CATALYST
                    : RecipeIngredientRole.INPUT;
            IRecipeSlotBuilder slot = builder.addSlot(role, x, y);

            switch (step.kind()) {
                case MATERIAL -> {
                    slot.addItemStacks(AssemblyJeiStacks.directMaterial(
                            step.material(),
                            step.metalOverride(),
                            recipe.material(),
                            step.relativeRequirements(),
                            step.count()
                    ));
                    addRelativeRequirementTooltip(slot, step.relativeRequirements());
                }
                case COMPONENT -> {
                    slot.addItemStacks(AssemblyJeiStacks.component(
                            step.component(),
                            step.metalOverride(),
                            recipe.material(),
                            step.relativeRequirements(),
                            step.count()
                    ));
                    AssemblyJeiCategory.addComponentTooltip(slot, step.component().displayName());
                    addRelativeRequirementTooltip(slot, step.relativeRequirements());
                }
                case ITEM -> slot.addItemStacks(AssemblyJeiStacks.exactItem(step.itemId(), step.count()));
                case TOOL -> {
                    slot.addItemStacks(AssemblyJeiStacks.tools(step.tool()));
                    AssemblyJeiCategory.addToolTooltip(slot, step.tool(), step.count());
                }
                case WAIT -> { }
            }
            displayedInput++;
        }
    }


    private static void addRelativeRequirementTooltip(
            IRecipeSlotBuilder slot,
            List<net.mads.industron.recipe.recipetypes.assembly.AssemblyRelativeRequirement> requirements
    ) {
        if (requirements.isEmpty()) return;
        slot.addTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.literal("Relative requirements (all descendant material parts):")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            requirements.stream().map(net.mads.industron.recipe.recipetypes.assembly.AssemblyRelativeRequirement::tooltip)
                    .forEach(tooltip::add);
        });
    }

    @Override
    public void draw(
            AssemblyComponentJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(
                font,
                recipe.material().displayName() + " " + recipe.definition().component().displayName(),
                8,
                15,
                0xFFFFFFFF,
                false
        );
        graphics.drawString(font, "Component", 126, 31, 0xFF777777, false);
        graphics.drawString(font, "Direct requirements", 8, 70, 0xFF777777, false);

        long waitTicks = recipe.definition().steps().stream()
                .filter(step -> step.kind() == ComponentDefinition.StepKind.WAIT)
                .mapToLong(step -> (long) step.waitTicks() * step.count())
                .sum();
        if (waitTicks > 0) {
            graphics.drawString(font, "Wait: " + (waitTicks / 20.0F) + " s", 8, 80, 0xFFAAAAAA, false);
        }
    }

    public static List<AssemblyComponentJeiRecipe> allRecipes() {
        List<AssemblyComponentJeiRecipe> result = new ArrayList<>();
        Set<String> registeredOutputs = new LinkedHashSet<>();
        for (ComponentDefinition definition : ComponentDefinitions.ALL) {
            if (AssemblyJeiStacks.representativeIndex(definition) < 0) continue;
            for (var material : IndustrialMaterials.ALL) {
                if (!AssemblyJeiStacks.componentAvailable(definition.component(), material)) continue;
                List<ItemStack> outputs = AssemblyJeiStacks.component(definition.component(), null, material, 1);
                if (outputs.isEmpty()) continue;
                String outputKey = definition.component().id() + "|"
                        + BuiltInRegistries.ITEM.getKey(outputs.get(0).getItem());
                if (!registeredOutputs.add(outputKey)) continue;
                result.add(new AssemblyComponentJeiRecipe(definition, material));
            }
        }
        return List.copyOf(result);
    }
}
