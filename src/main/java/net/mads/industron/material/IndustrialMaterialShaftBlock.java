package net.mads.industron.material;

import net.mads.industron.kinetics.shaft.AbstractMaterialShaftBlock;
import net.mads.industron.kinetics.shaft.ShaftLimits;

/** Generated shaft form for every industrial metal. */
public final class IndustrialMaterialShaftBlock extends AbstractMaterialShaftBlock implements MaterialPartBlock {
    private final IndustrialMaterial material;

    public IndustrialMaterialShaftBlock(IndustrialMaterial material) {
        super(MaterialBlock.propertiesFor(material, MaterialPart.SHAFT), ShaftLimits.METAL, true);
        this.material = material;
    }

    @Override
    public IndustrialMaterial material() {
        return material;
    }

    @Override
    public MaterialPart part() {
        return MaterialPart.SHAFT;
    }
}
