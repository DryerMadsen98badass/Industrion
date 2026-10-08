package net.mads.industron.material.organism;

import net.minecraft.world.item.Item;

/** Carries shared composition identity independently of item naming. */
public final class BiologicalMaterialItem extends Item {
    private final BiologicalItemCatalog.Entry definition;
    public BiologicalMaterialItem(BiologicalItemCatalog.Entry definition) {
        super(new Item.Properties());
        this.definition=definition;
    }
    public BiologicalMaterial material() { return definition.material(); }
    public BiologicalItemCatalog.Entry definition() { return definition; }
}
