package net.mads.industron.material;

import net.mads.industron.machine.MachineTier;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record IndustrialMaterial(
        String id,
        String displayName,
        int atomicNumber,
        MachineTier tier,
        MaterialContentProfile contentProfile,
        MaterialProperties properties,
        String itemMaterialSet,
        String blockMaterialSet,
        Set<MaterialPart> parts,
        Map<MaterialPart, ResourceLocation> existingParts,
        Set<MaterialPart> existingRecipeParts,
        Map<MaterialPart, ResourceLocation> customPartTextures,
        int radioactivity,
        Optional<String> elementSymbol,
        List<MaterialComponent> components,
        double furnaceFuelItems,
        boolean furnaceFuelSet,
        Map<MaterialPart, Double> furnaceFuelParts,
        List<MaterialStoneSource> stoneSources,
        Optional<MachineTier> centrifugeTier,
        int centrifugeInputCount,
        Optional<MachineTier> electrolyserTier,
        int electrolyserInputCount,
        Optional<IndustrialMaterial> smeltingResult,
        boolean smeltingSelf
) implements IndustrialSubstance {
    public IndustrialMaterial {
        parts = Set.copyOf(parts);
        existingParts = Map.copyOf(existingParts);
        existingRecipeParts = Set.copyOf(existingRecipeParts);
        customPartTextures = Map.copyOf(customPartTextures);
        furnaceFuelParts = Map.copyOf(furnaceFuelParts);
        elementSymbol = elementSymbol == null ? Optional.empty() : elementSymbol;
        components = List.copyOf(components);
        stoneSources = List.copyOf(stoneSources);
        centrifugeTier = centrifugeTier == null ? Optional.empty() : centrifugeTier;
        electrolyserTier = electrolyserTier == null ? Optional.empty() : electrolyserTier;
        smeltingResult = smeltingResult == null ? Optional.empty() : smeltingResult;
        if (tier == null || tier == MachineTier.NONE) {
            throw new IllegalArgumentException("Industrial material tier must be a real tier: " + id);
        }
        if (contentProfile == null) {
            throw new IllegalArgumentException("Industrial material content profile cannot be null: " + id);
        }
        if (properties == null) {
            throw new IllegalArgumentException("Industrial material properties cannot be null: " + id);
        }
    }

    public boolean has(MaterialPart part) { return parts.contains(part); }
    public boolean isMineralDust() { return contentProfile == MaterialContentProfile.MINERAL_DUST; }
    public boolean isOreMaterial() { return contentProfile == MaterialContentProfile.ORE; }
    public boolean hasExistingPart(MaterialPart part) { return existingParts.containsKey(part); }
    public ResourceLocation existingPart(MaterialPart part) { return existingParts.get(part); }
    public boolean hasExistingRecipe(MaterialPart part) { return existingRecipeParts.contains(part); }
    public boolean hasCustomPartTexture(MaterialPart part) { return customPartTextures.containsKey(part); }
    public ResourceLocation customPartTexture(MaterialPart part) { return customPartTextures.get(part); }

    public int color() { return properties.baseColor(); }
    public int strength() { return properties.structuralStrength(); }
    public int meltingPoint() { return properties.meltingPoint(); }
    public int temperature() { return properties.ambientTemperature(); }
    public boolean hasExplicitStrength() { return false; }
    public boolean hasExplicitMeltingPoint() { return false; }
    public int castTemperature() { return properties.castTemperature(); }

    public int temperatureFor(MaterialPart part) {
        return MaterialPropertyCalculator.temperatureFor(properties, part);
    }

    @Override
    public int componentTemperature() { return properties.meltingPoint(); }

    public boolean isFurnaceFuel() { return properties.furnaceFuel(); }

    public int furnaceBurnTimeTicks() { return properties.furnaceBurnTimeTicks(); }

    public boolean isFurnaceFuel(MaterialPart part) {
        return furnaceFuelParts.containsKey(part);
    }

    public int furnaceBurnTimeTicks(MaterialPart part) {
        Double fuelItems = furnaceFuelParts.get(part);
        if (fuelItems == null) {
            throw new IllegalArgumentException("Material part is not a furnace fuel: " + id + " " + part);
        }
        return MaterialPropertyCalculator.furnaceBurnTimeTicks(properties, fuelItems);
    }

    @Override
    public String formula() { return formula(false); }

    @Override
    public String formula(boolean nested) {
        if (elementSymbol.isPresent()) return elementSymbol.get();
        return MaterialFormulaFormatter.compound(components, nested);
    }

    public String compoundFormula(boolean nested) { return formula(nested); }
}
