package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.WoodMaterial;
import net.minecraft.resources.ResourceLocation;

/** Shared id/form lookup for auto-generated wood Assembly and processing recipes. */
final class WoodRecipeIds {
    private WoodRecipeIds() {
    }

    static boolean has(WoodMaterial wood, MaterialPart part) {
        if (wood.hasExistingPart(part)) return true;
        if (part.isItem()) return wood.generatedForms().contains(part);
        if (part.isBlock()) {
            return StructureMaterialGenerator.blockDefinitions(wood).stream()
                    .anyMatch(definition -> definition.part().filter(candidate -> candidate == part).isPresent());
        }
        return false;
    }

    static ResourceLocation id(WoodMaterial wood, MaterialPart part) {
        if (!has(wood, part)) return null;
        if (wood.hasExistingPart(part)) return wood.existingPart(part);
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(wood));
    }

    static String stringId(WoodMaterial wood, MaterialPart part) {
        ResourceLocation id = id(wood, part);
        if (id == null) {
            throw new IllegalArgumentException("Wood " + wood.id() + " does not expose " + part);
        }
        return id.toString();
    }
}
