package net.mads.industron.machine;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.recipes.CasingDefinition;

import java.util.Objects;

/** A generated machine casing bound to one IndustrialMaterial and that material's tier. */
public final class MaterialMachineCasingBlock extends MachineCasingBlock {
    private final IndustrialMaterial material;
    private final CasingDefinition definition;

    public MaterialMachineCasingBlock(
            IndustrialMaterial material,
            CasingDefinition definition,
            MachineTier tier
    ) {
        super(tier);
        this.material = Objects.requireNonNull(material, "material");
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public IndustrialMaterial material() {
        return material;
    }

    public CasingDefinition definition() {
        return definition;
    }
}
