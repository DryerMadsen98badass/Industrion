package net.mads.industron.material.recipes;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.recipetypes.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipetypes.AssemblyRequirement;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Converts generated material casings into ordinary AssemblyRecipeDefinition recipes. */
public final class MaterialCasingAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private MaterialCasingAssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();
        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            result.add(build(generated));
        }
        return List.copyOf(result);
    }

    private static AssemblyRecipeDefinition build(MaterialCasingGenerator.GeneratedCasing generated) {
        CasingDefinition definition = generated.definition();
        IndustrialMaterial material = generated.material();

        AssemblyRecipeDefinition.Builder builder = AssemblyRecipeDefinition
                .recipe("material/recipes/" + generated.registryName())
                .baseBlockInput(resolveMaterialBlock(material, definition.baseBlockInput()));

        for (CasingDefinition.Input input : definition.inputs()) {
            switch (input.kind()) {
                case MATERIAL -> {
                    if (input.metalOverride() == null) builder.input(input.material(), material, input.count());
                    else builder.input(input.material(), input.metalOverride(), input.count());
                }
                case COMPONENT -> {
                    if (input.metalOverride() == null) builder.input(input.component(), material, input.count());
                    else builder.input(input.component(), input.metalOverride(), input.count());
                }
                case ITEM -> builder.input(input.itemId().toString(), input.count());
                case TOOL -> builder.input(input.tool());
                case WAIT -> builder.waitTicks(input.waitTicks());
            }

            if (input.sound() != null) builder.sound(input.sound());

            for (AssemblyRequirement requirement : MaterialCasingGenerator.effectiveInputRequirements(generated, input)) {
                applyRequirement(builder, requirement);
            }
        }

        // Casing output is always the exact generated casing block for this material.
        return builder
                .baseBlockOutput(BlockRegistry.getMaterialMachineCasing(generated.registryName()).get())
                .build();
    }

    private static Block resolveMaterialBlock(IndustrialMaterial material, MaterialPart part) {
        if (material.hasExistingPart(part)) {
            return BuiltInRegistries.BLOCK.get(material.existingPart(part));
        }
        var holder = BlockRegistry.getMaterialBlock(material, part);
        if (holder == null) {
            throw new IllegalStateException("Missing generated material block for casing base: " + material.id() + " " + part);
        }
        return holder.get();
    }

    private static void applyRequirement(AssemblyRecipeDefinition.Builder builder, AssemblyRequirement requirement) {
        if (requirement.kind() == AssemblyRequirement.Kind.IS) {
            applyPropertyRequirement(builder, requirement);
            return;
        }
        if (requirement.capability() == null) {
            throw new IllegalStateException("Numeric casing requirement has no capability: " + requirement);
        }
        switch (requirement.kind()) {
            case AT_LEAST -> builder.stat(requirement.capability()).atLeast(requirement.first());
            case AT_MOST -> builder.stat(requirement.capability()).atMost(requirement.first());
            case EXACT -> builder.stat(requirement.capability()).exactly(requirement.first());
            case RANGE -> builder.stat(requirement.capability()).range(requirement.first(), requirement.second());
            case IS -> throw new IllegalStateException("Handled above");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyPropertyRequirement(AssemblyRecipeDefinition.Builder builder, AssemblyRequirement requirement) {
        if (requirement.property() == null) {
            throw new IllegalStateException("Typed casing requirement has no property: " + requirement);
        }
        builder.stat((net.mads.industron.recipe.recipetypes.AssemblyProperty) requirement.property())
                .is(requirement.expected());
    }
}
