package net.mads.industron.tool;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;

import java.util.ArrayList;
import java.util.List;

/**
 * Material eligibility for composed tools.
 *
 * <p>The material definitions are the source of truth. A material can supply a tool part when
 * it actually exposes that MaterialPart and the part is used by a registered ToolDefinition.
 * There are deliberately no separate WOOD_PARTS / STONE_PARTS / METAL_PARTS lists to keep in
 * sync with material forms.</p>
 */
public final class ToolMaterialRules {
    public enum Kind { WOOD, STONE, METAL, NONE }

    private ToolMaterialRules() {
    }

    public static Kind kind(IndustrialSubstance material) {
        if (material instanceof WoodMaterial) return Kind.WOOD;
        if (material instanceof StoneMaterial) return Kind.STONE;
        if (material instanceof IndustrialMaterial industrial
                && (MaterialCategory.METAL.matches(industrial) || MaterialCategory.GEM.matches(industrial))) {
            return Kind.METAL;
        }
        return Kind.NONE;
    }

    public static MachineTier tier(IndustrialSubstance material) {
        return switch (kind(material)) {
            case WOOD -> MachineTier.ULV;
            case STONE -> MachineTier.LV;
            case METAL -> ((IndustrialMaterial) material).tier().recipeTier();
            case NONE -> MachineTier.NONE;
        };
    }

    /** All current cold material parts referenced by a real tool definition. */
    public static boolean isAssemblyToolPart(MaterialPart part) {
        if (part == null) return false;
        return ToolDefinitions.ALL.stream()
                .flatMap(definition -> definition.parts().stream())
                .anyMatch(slot -> slot.part() == part);
    }

    /** All currently defined tool materials that can form the requested cold part. */
    public static List<IndustrialSubstance> candidates(MaterialPart part) {
        if (part == null || isHotToolPart(part) || !isAssemblyToolPart(part)) return List.of();
        List<IndustrialSubstance> result = new ArrayList<>();
        WoodMaterials.ALL.stream().filter(material -> allows(material, part)).forEach(result::add);
        StoneMaterials.ALL.stream().filter(material -> allows(material, part)).forEach(result::add);
        IndustrialMaterials.ALL.stream().filter(material -> allows(material, part)).forEach(result::add);
        return List.copyOf(result);
    }

    /** True when at least one material of the requested family actually exposes this part. */
    public static boolean hasCandidate(MaterialPart part, Kind requestedKind) {
        if (requestedKind == null || requestedKind == Kind.NONE) return false;
        return candidates(part).stream().anyMatch(material -> kind(material) == requestedKind);
    }

    /**
     * A part is allowed solely because the material exposes it and a tool definition consumes it.
     * Adding/removing a form on the material therefore updates assembly eligibility automatically.
     */
    public static boolean allows(IndustrialSubstance material, MaterialPart part) {
        return material != null
                && part != null
                && kind(material) != Kind.NONE
                && !isHotToolPart(part)
                && isAssemblyToolPart(part)
                && hasForm(material, part);
    }

    private static boolean hasForm(IndustrialSubstance material, MaterialPart part) {
        if (material instanceof IndustrialMaterial industrial) {
            return industrial.has(part) || industrial.hasExistingPart(part);
        }
        if (material instanceof net.mads.industron.material.structure.StructureMaterial structure) {
            return structure.generatedForms().contains(part) || structure.hasExistingPart(part);
        }
        return false;
    }

    /** Includes every active tool part so all material tool components stack to one. */
    public static boolean isToolPartForm(MaterialPart part) {
        return isColdToolPart(part) || isHotToolPart(part);
    }

    public static boolean isHotToolPart(MaterialPart part) {
        if (part == null || !part.id().startsWith("hot_")) return false;
        String coldId = part.id().substring("hot_".length());
        for (MaterialPart candidate : MaterialPart.values()) {
            if (candidate.id().equals(coldId)) {
                return isColdToolPart(candidate);
            }
        }
        return false;
    }

    public static boolean isColdToolPart(MaterialPart part) {
        return part != null && isAssemblyToolPart(part);
    }

    /** One-part tool definitions own durability directly on their material-part item. */
    public static boolean isDirectFinishedToolPart(MaterialPart part) {
        if (part == null) return false;
        return ToolDefinitions.ALL.stream()
                .filter(definition -> definition.isDirectPartTool())
                .flatMap(definition -> definition.parts().stream())
                .anyMatch(slot -> slot.part() == part);
    }
}
