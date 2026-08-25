package net.mads.industron.integration.jei.assembly;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.recipe.recipetypes.ComponentDefinition;

import java.util.Objects;

/** One component definition bound to one inherited material for synchronized JEI stacks. */
public record AssemblyComponentJeiRecipe(
        ComponentDefinition definition,
        IndustrialMaterial material
) {
    public AssemblyComponentJeiRecipe {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(material, "material");
    }
}
