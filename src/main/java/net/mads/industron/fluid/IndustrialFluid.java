package net.mads.industron.fluid;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialFormulaFormatter;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.recipe.ChemicalBalanceRange;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record IndustrialFluid(
        String id,
        String displayName,
        int color,
        Kind kind,
        int temperature,
        int density,
        int viscosity,
        int lightLevel,
        Optional<Integer> chemicalBalanceHundredths,
        int cbDrainPerTickMb,
        List<MaterialComponent> components,
        Optional<ResourceLocation> existingFluid
) implements IndustrialSubstance {
    public IndustrialFluid {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Industrial fluid id cannot be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Industrial fluid display name cannot be blank");
        if (kind == null) throw new IllegalArgumentException("Industrial fluid kind cannot be null");
        if (lightLevel < 0 || lightLevel > 15) throw new IllegalArgumentException("Industrial fluid light level must be between 0 and 15");
        chemicalBalanceHundredths = chemicalBalanceHundredths == null ? Optional.empty() : chemicalBalanceHundredths;
        chemicalBalanceHundredths.ifPresent(value -> {
            if (value < ChemicalBalanceRange.MIN_HUNDREDTHS || value > ChemicalBalanceRange.MAX_HUNDREDTHS) {
                throw new IllegalArgumentException("Industrial fluid Chemical Balance must be between -100 and 100");
            }
            if (cbDrainPerTickMb <= 0) {
                throw new IllegalArgumentException("Industrial fluid CB drain rate must be greater than 0 mB/t");
            }
        });
        if (chemicalBalanceHundredths.isEmpty() && cbDrainPerTickMb != 0) {
            throw new IllegalArgumentException("Industrial fluid without Chemical Balance cannot define a CB drain rate");
        }
        components = List.copyOf(components);
        existingFluid = existingFluid == null ? Optional.empty() : existingFluid;
    }

    public boolean isGas() { return kind == Kind.GAS; }
    public boolean isLiquid() { return kind == Kind.LIQUID; }
    public boolean isMolten() { return kind == Kind.MOLTEN; }
    public boolean hasComponents() { return !components.isEmpty(); }
    public boolean hasExistingFluid() { return existingFluid.isPresent(); }
    public boolean hasChemicalBalance() { return chemicalBalanceHundredths.isPresent(); }
    public double chemicalBalance() {
        return chemicalBalanceHundredths.map(ChemicalBalanceRange::fromHundredths).orElseThrow(() ->
                new IllegalStateException("Industrial fluid '" + id + "' does not define a Chemical Balance value"));
    }

    public ResourceLocation existingFluidId() {
        return existingFluid.orElseThrow(() -> new IllegalStateException(
                "Industrial fluid '" + id + "' does not reference an existing fluid"));
    }

    @Override
    public int componentTemperature() { return temperature; }

    @Override
    public String formula() { return formula(false); }

    @Override
    public String formula(boolean nested) {
        return MaterialFormulaFormatter.compound(components, nested);
    }

    public String textureName() {
        return switch (kind) {
            case LIQUID -> "liquid";
            case GAS -> "gas";
            case MOLTEN -> "molten";
        };
    }

    public String registryName() { return kind == Kind.MOLTEN ? "molten_" + id : id; }
    public String bucketName() { return registryName() + "_bucket"; }
    public String localizedName() {
        return switch (kind) {
            case LIQUID -> displayName;
            case GAS -> displayName + " Gas";
            case MOLTEN -> "Molten " + displayName;
        };
    }
    public String bucketDisplayName() { return localizedName() + " Bucket"; }

    public enum Kind { LIQUID, GAS, MOLTEN }
}
