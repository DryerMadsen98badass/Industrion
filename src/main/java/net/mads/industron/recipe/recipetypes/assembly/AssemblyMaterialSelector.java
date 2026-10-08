package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.tool.ToolMaterialRules;

import java.util.List;
import java.util.Objects;

/**
 * Material selection policy for Assembly inputs.
 *
 * <p>Selectors reference the central material catalog rather than duplicating every material
 * as Java constants. Category selectors therefore discover newly registered materials/defined alloys
 * automatically.</p>
 */
public record AssemblyMaterialSelector(Mode mode, MaterialCategory category, String fixedMaterialId) {
    public enum Mode { ANY, CATEGORY, FIXED, WOOD, STONE, TOOL_MATERIAL, TOOL_WOOD, TOOL_STONE }

    public static final AssemblyMaterialSelector ANY = new AssemblyMaterialSelector(Mode.ANY, null, null);
    public static final AssemblyMaterialSelector METAL = category(MaterialCategory.METAL);
    public static final AssemblyMaterialSelector GEM = category(MaterialCategory.GEM);
    public static final AssemblyMaterialSelector FLUID = category(MaterialCategory.FLUID);
    public static final AssemblyMaterialSelector GAS = category(MaterialCategory.GAS);
    /** Tool-only selector; unlike ANY this may bind wood, stone, or metal tool materials. */
    public static final AssemblyMaterialSelector WOOD = new AssemblyMaterialSelector(Mode.WOOD, null, null);
    public static final AssemblyMaterialSelector STONE = new AssemblyMaterialSelector(Mode.STONE, null, null);
    public static final AssemblyMaterialSelector TOOL_MATERIAL = new AssemblyMaterialSelector(Mode.TOOL_MATERIAL, null, null);
    public static final AssemblyMaterialSelector TOOL_WOOD = new AssemblyMaterialSelector(Mode.TOOL_WOOD, null, null);
    public static final AssemblyMaterialSelector TOOL_STONE = new AssemblyMaterialSelector(Mode.TOOL_STONE, null, null);

    public AssemblyMaterialSelector(String fixedMaterialId) {
        this(Mode.FIXED, null, fixedMaterialId);
    }

    public AssemblyMaterialSelector {
        Objects.requireNonNull(mode, "mode");
        switch (mode) {
            case ANY -> {
                if (category != null || fixedMaterialId != null) {
                    throw new IllegalArgumentException("ANY material selector cannot define category/fixed id");
                }
            }
            case CATEGORY -> {
                Objects.requireNonNull(category, "category");
                if (fixedMaterialId != null) {
                    throw new IllegalArgumentException("CATEGORY material selector cannot define fixed id");
                }
            }
            case FIXED -> {
                if (category != null) {
                    throw new IllegalArgumentException("FIXED material selector cannot define category");
                }
                if (fixedMaterialId == null || fixedMaterialId.isBlank()) {
                    throw new IllegalArgumentException("Fixed material id cannot be blank");
                }
            }
            case WOOD, STONE, TOOL_MATERIAL, TOOL_WOOD, TOOL_STONE -> {
                if (category != null || fixedMaterialId != null) {
                    throw new IllegalArgumentException(mode + " tool selector cannot define category/fixed id");
                }
            }
        }
    }

    public static AssemblyMaterialSelector category(MaterialCategory category) {
        return new AssemblyMaterialSelector(Mode.CATEGORY, Objects.requireNonNull(category), null);
    }

    /** Fixes the selector to one canonical material identity from IndustrialMaterials.
     *  Accepts both ElementDefinition and IndustrialMaterial because both are IndustrialSubstance.
     */
    public static AssemblyMaterialSelector fixed(IndustrialSubstance substance) {
        return new AssemblyMaterialSelector(Mode.FIXED, null, Objects.requireNonNull(substance).id());
    }

    public boolean isAny() { return mode == Mode.ANY; }
    public boolean isCategory() { return mode == Mode.CATEGORY; }
    public boolean isFixed() { return mode == Mode.FIXED; }
    public boolean isWood() { return mode == Mode.WOOD; }
    public boolean isStone() { return mode == Mode.STONE; }
    public boolean isToolSelector() { return mode == Mode.TOOL_MATERIAL || mode == Mode.TOOL_WOOD || mode == Mode.TOOL_STONE; }
    public boolean isFree() { return mode != Mode.FIXED; }

    public IndustrialSubstance resolveSubstance() {
        if (!isFixed()) {
            throw new IllegalStateException("Only a fixed AssemblyMaterialSelector resolves to one material");
        }

        IndustrialMaterial industrial = MaterialCatalog.find(fixedMaterialId);
        if (industrial != null) return industrial;

        var wood = WoodMaterials.ALL.stream()
                .filter(material -> material.id().equals(fixedMaterialId))
                .findFirst()
                .orElse(null);
        if (wood != null) return wood;

        var stone = StoneMaterials.ALL.stream()
                .filter(material -> material.id().equals(fixedMaterialId))
                .findFirst()
                .orElse(null);
        if (stone != null) return stone;

        throw new IllegalStateException("Unknown material/substance: " + fixedMaterialId);
    }

    public IndustrialMaterial resolve() {
        IndustrialSubstance substance = resolveSubstance();
        if (substance instanceof IndustrialMaterial industrial) return industrial;
        throw new IllegalStateException(
                "Fixed Assembly material " + fixedMaterialId + " is not an IndustrialMaterial"
        );
    }

    public boolean matches(IndustrialMaterial material) {
        if (material == null) return false;
        return switch (mode) {
            case ANY -> true;
            case CATEGORY -> category.matches(material);
            case FIXED -> fixedMaterialId.equals(material.id());
            case WOOD, STONE, TOOL_MATERIAL, TOOL_WOOD, TOOL_STONE -> false;
        };
    }

    public List<IndustrialMaterial> candidates() {
        return switch (mode) {
            case ANY -> MaterialCatalog.all();
            case CATEGORY -> MaterialCatalog.all(category);
            case FIXED -> List.of(resolve());
            case WOOD, STONE, TOOL_MATERIAL, TOOL_WOOD, TOOL_STONE -> List.of();
        };
    }

    /** Matches a material identity across every Assembly-visible definition family. */
    public boolean matchesSubstance(IndustrialSubstance material) {
        if (material == null) return false;
        return switch (mode) {
            case ANY -> true;
            case CATEGORY -> material instanceof IndustrialMaterial industrial && category.matches(industrial);
            case FIXED -> resolveSubstance().getClass().equals(material.getClass())
                    && fixedMaterialId.equals(material.id());
            case WOOD -> material instanceof net.mads.industron.material.structure.WoodMaterial;
            case STONE -> material instanceof net.mads.industron.material.structure.StoneMaterial;
            case TOOL_MATERIAL, TOOL_WOOD, TOOL_STONE -> false;
        };
    }

    public boolean matchesTool(IndustrialSubstance material, MaterialPart part) {
        if (material == null || part == null || !ToolMaterialRules.allows(material, part)) return false;
        return switch (mode) {
            case TOOL_MATERIAL -> true;
            case TOOL_WOOD -> ToolMaterialRules.kind(material) == ToolMaterialRules.Kind.WOOD;
            case TOOL_STONE -> ToolMaterialRules.kind(material) == ToolMaterialRules.Kind.STONE;
            case ANY, CATEGORY, FIXED -> material instanceof IndustrialMaterial industrial && matches(industrial);
            case WOOD -> material instanceof net.mads.industron.material.structure.WoodMaterial;
            case STONE -> material instanceof net.mads.industron.material.structure.StoneMaterial;
        };
    }

    public String displayName() {
        return switch (mode) {
            case ANY -> "Any Material";
            case CATEGORY -> switch (category) {
                case METAL -> "Metal";
                case GEM -> "Gem";
                case FLUID -> "Fluid";
                case GAS -> "Gas";
                case OTHER_SOLID -> "Solid Material";
            };
            case FIXED -> resolveSubstance().displayName();
            case WOOD -> "Wood";
            case STONE -> "Stone";
            case TOOL_MATERIAL -> "Tool Material";
            case TOOL_WOOD -> "Wood Tool Material";
            case TOOL_STONE -> "Stone Tool Material";
        };
    }
}
