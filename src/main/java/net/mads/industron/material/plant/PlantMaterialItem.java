package net.mads.industron.material.plant;

import net.minecraft.world.item.Item;

/** Registered item form generated from a {@link PlantMaterial}. */
public final class PlantMaterialItem extends Item {
    private final PlantMaterial material;
    private final PlantPart part;
    private final PlantDerivedSubstance substance;

    public PlantMaterialItem(PlantMaterial material, PlantPart part) {
        super(new Item.Properties());
        this.material = material;
        this.part = part;
        this.substance = PlantMaterialGenerator.substanceFor(material, part);
    }

    public PlantMaterial material() {
        return material;
    }

    public PlantPart part() {
        return part;
    }

    /** Part-specific material identity used by fuel/chemistry/process code. */
    public PlantDerivedSubstance substance() {
        return substance;
    }
}
