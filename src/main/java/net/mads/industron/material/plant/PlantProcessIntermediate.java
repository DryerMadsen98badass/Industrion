package net.mads.industron.material.plant;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.CompositionColor;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialFormulaFormatter;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * One route-owned plant-processing state.
 *
 * <p>Unlike {@link PlantPart}, an intermediate is not a permanent species form. It is created only
 * when an actually generated process route needs it. The state can be solid, liquid or gas and the
 * explicit component list is the material truth for every downstream recipe.</p>
 */
public record PlantProcessIntermediate(
        PlantMaterial parent,
        String suffix,
        String displaySuffix,
        ChemistryPhase phase,
        List<MaterialComponent> components,
        Optional<ResourceLocation> itemTexture
) implements IndustrialSubstance {
    public PlantProcessIntermediate {
        if (parent == null) throw new IllegalArgumentException("Plant intermediate requires a parent");
        if (suffix == null || suffix.isBlank()) throw new IllegalArgumentException("Plant intermediate suffix cannot be blank");
        if (displaySuffix == null || displaySuffix.isBlank()) throw new IllegalArgumentException("Plant intermediate display suffix cannot be blank");
        if (phase == null || phase == ChemistryPhase.UNKNOWN || phase == ChemistryPhase.MIXED || phase == ChemistryPhase.PLASMA) {
            throw new IllegalArgumentException("Unsupported plant intermediate phase: " + phase);
        }
        components = List.copyOf(components);
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Plant intermediate " + parent.id() + "_" + suffix + " has no .contains(...) composition");
        }
        itemTexture = itemTexture == null ? Optional.empty() : itemTexture;
        if (phase != ChemistryPhase.SOLID && itemTexture.isPresent()) {
            throw new IllegalArgumentException("Only solid plant intermediates can define item textures: " + idFor(parent, suffix));
        }
    }

    public static PlantProcessIntermediate solid(
            PlantMaterial parent,
            String suffix,
            String displaySuffix,
            List<MaterialComponent> components,
            String texturePath
    ) {
        ResourceLocation texture = texturePath == null || texturePath.isBlank()
                ? null
                : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, texturePath);
        return new PlantProcessIntermediate(
                parent,
                suffix,
                displaySuffix,
                ChemistryPhase.SOLID,
                components,
                Optional.ofNullable(texture)
        );
    }

    public static PlantProcessIntermediate fluid(
            PlantMaterial parent,
            String suffix,
            String displaySuffix,
            ChemistryPhase phase,
            List<MaterialComponent> components
    ) {
        if (phase != ChemistryPhase.LIQUID && phase != ChemistryPhase.GAS) {
            throw new IllegalArgumentException("Plant process fluid must be LIQUID or GAS: " + phase);
        }
        return new PlantProcessIntermediate(parent, suffix, displaySuffix, phase, components, Optional.empty());
    }

    public ResourceLocation registryId() {
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id());
    }

    public boolean isSolid() {
        return phase == ChemistryPhase.SOLID;
    }

    public boolean isLiquid() {
        return phase == ChemistryPhase.LIQUID;
    }

    public boolean isGas() {
        return phase == ChemistryPhase.GAS;
    }

    @Override
    public String id() {
        return idFor(parent, suffix);
    }

    @Override
    public String displayName() {
        return parent.displayName() + " " + displaySuffix;
    }

    @Override
    public int color() {
        return CompositionColor.blend(components);
    }

    @Override
    public String formula() {
        return formula(false);
    }

    @Override
    public String formula(boolean nested) {
        return MaterialFormulaFormatter.compound(components, nested);
    }

    @Override
    public int componentTemperature() {
        return 20;
    }

    private static String idFor(PlantMaterial parent, String suffix) {
        return parent.id() + "_" + suffix;
    }
}
