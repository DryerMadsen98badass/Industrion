package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Chest-boat entity for generated WoodMaterial boat families. */
public final class StructureWoodChestBoat extends ChestBoat {
    private final WoodMaterial material;

    public StructureWoodChestBoat(EntityType<? extends Boat> type, Level level, WoodMaterial material) {
        super(type, level);
        this.material = material;
        setVariant(Boat.Type.OAK);
    }

    public WoodMaterial material() {
        return material;
    }

    @Override
    public Item getDropItem() {
        var holder = ItemRegistry.getStructureMaterialFormItem(material, MaterialPart.CHEST_BOAT);
        return holder == null ? super.getDropItem() : holder.get();
    }
}
