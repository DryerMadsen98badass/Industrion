package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Boat entity for WoodMaterials that do not have a vanilla/Create .existing(BOAT) form. */
public final class StructureWoodBoat extends Boat {
    private final WoodMaterial material;

    public StructureWoodBoat(EntityType<? extends Boat> type, Level level, WoodMaterial material) {
        super(type, level);
        this.material = material;
        // Generated woods reuse vanilla boat geometry/physics; visual identity comes from the
        // material-specific generated entity texture, not from Boat.Type.
        setVariant(Boat.Type.OAK);
    }

    public WoodMaterial material() {
        return material;
    }

    @Override
    public Item getDropItem() {
        var holder = ItemRegistry.getStructureMaterialFormItem(material, MaterialPart.BOAT);
        return holder == null ? super.getDropItem() : holder.get();
    }
}
