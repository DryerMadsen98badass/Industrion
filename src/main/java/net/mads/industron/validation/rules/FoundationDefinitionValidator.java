package net.mads.industron.validation.rules;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Validates stable IDs owned by the base machine/recipe domain. */
public final class FoundationDefinitionValidator implements ValidationRule {
    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        validateMachineTiers(diagnostics);
        validateRecipeTypes(diagnostics);
    }

    private static void validateMachineTiers(ValidationCollector diagnostics) {
        List<MachineTier> tiers = new java.util.ArrayList<>(MachineTier.ALL);
        tiers.addAll(MachineTier.STEAM_SINGLEBLOCK_TIERS);

        Set<String> ids = new HashSet<>();
        for (MachineTier tier : tiers) {
            String subject = "machine_tier:" + tier.id();
            if (!validPath(tier.id())) {
                diagnostics.error(
                        ValidationSubsystem.FOUNDATION,
                        ValidationCode.INVALID_ID,
                        subject,
                        "Machine tier id is not a valid resource path"
                );
            }
            if (!ids.add(tier.id())) {
                diagnostics.error(
                        ValidationSubsystem.FOUNDATION,
                        ValidationCode.DUPLICATE_ID,
                        subject,
                        "Duplicate machine tier id"
                );
            }
        }
    }

    private static void validateRecipeTypes(ValidationCollector diagnostics) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (RecipeTypeDefinition type : CERecipeTypes.ALL) {
            String subject = "recipe_type:" + type.id();
            if (!ids.add(type.id())) {
                diagnostics.error(
                        ValidationSubsystem.RECIPE,
                        ValidationCode.DUPLICATE_ID,
                        subject,
                        "Duplicate CE recipe type id"
                );
            }
            if (type.displayName() == null || type.displayName().isBlank()) {
                diagnostics.error(
                        ValidationSubsystem.RECIPE,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Recipe type display name cannot be blank"
                );
            }
            if (type.maxItemInputs() < 0 || type.maxItemOutputs() < 0
                    || type.maxFluidInputs() < 0 || type.maxFluidOutputs() < 0) {
                diagnostics.error(
                        ValidationSubsystem.RECIPE,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Recipe type IO limits cannot be negative"
                );
            }

            Set<ResourceLocation> logic = new HashSet<>();
            for (ResourceLocation logicId : type.supportedLogic()) {
                if (!logic.add(logicId)) {
                    diagnostics.error(
                            ValidationSubsystem.RECIPE,
                            ValidationCode.DUPLICATE_ID,
                            subject,
                            "Recipe type contains duplicate logic id " + logicId
                    );
                }
            }
        }
    }

    private static boolean validPath(String path) {
        return path != null
                && !path.isBlank()
                && ResourceLocation.tryParse("industron:" + path) != null;
    }
}
