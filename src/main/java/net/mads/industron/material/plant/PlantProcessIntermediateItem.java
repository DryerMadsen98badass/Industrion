package net.mads.industron.material.plant;

import net.minecraft.world.item.Item;

/** Registered solid item owned by an on-demand plant-processing intermediate. */
public class PlantProcessIntermediateItem extends Item {
    private final PlantProcessIntermediate intermediate;

    public PlantProcessIntermediateItem(PlantProcessIntermediate intermediate) {
        super(new Item.Properties());
        if (intermediate == null || !intermediate.isSolid()) {
            throw new IllegalArgumentException("Plant process intermediate item requires a solid definition");
        }
        this.intermediate = intermediate;
    }

    public PlantProcessIntermediate intermediate() {
        return intermediate;
    }
}
