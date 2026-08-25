package net.mads.industron.material.structure;

import net.minecraft.world.item.Item;

public final class StructureMaterialItem extends Item {
    private final StructureMaterial material;
    private final StructureMaterialPart part;

    public StructureMaterialItem(StructureMaterial material, StructureMaterialPart part) {
        super(new Item.Properties());
        this.material = material;
        this.part = part;
    }

    public StructureMaterial material() {
        return material;
    }

    public StructureMaterialPart part() {
        return part;
    }
}
