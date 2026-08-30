package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;

import net.minecraft.world.item.Item;

public final class StructureMaterialItem extends Item {
    private final StructureMaterial material;
    private final MaterialPart part;

    public StructureMaterialItem(StructureMaterial material, MaterialPart part) {
        super(new Item.Properties());
        this.material = material;
        this.part = part;
    }

    public StructureMaterial material() {
        return material;
    }

    public MaterialPart part() {
        return part;
    }
}
