package net.mads.industron.registry;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.StructureWoodBoat;
import net.mads.industron.material.structure.StructureWoodChestBoat;
import net.mads.industron.material.structure.WoodMaterial;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** Entity types needed only for wood forms not replaced by .existing(...). */
public final class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Industron.MOD_ID);

    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<StructureWoodBoat>>> WOOD_BOATS =
            new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<StructureWoodChestBoat>>> WOOD_CHEST_BOATS =
            new LinkedHashMap<>();

    static {
        for (WoodMaterial wood : WoodMaterials.ALL) {
            if (!wood.hasExistingPart(MaterialPart.BOAT)) {
                String id = MaterialPart.BOAT.registryName(wood);
                WOOD_BOATS.put(wood.id(), ENTITIES.register(id, () ->
                        EntityType.Builder.<StructureWoodBoat>of(
                                        (type, level) -> new StructureWoodBoat(type, level, wood),
                                        MobCategory.MISC
                                )
                                .sized(1.375F, 0.5625F)
                                .clientTrackingRange(10)
                                .build(id)
                ));
            }
            if (!wood.hasExistingPart(MaterialPart.CHEST_BOAT)) {
                String id = MaterialPart.CHEST_BOAT.registryName(wood);
                WOOD_CHEST_BOATS.put(wood.id(), ENTITIES.register(id, () ->
                        EntityType.Builder.<StructureWoodChestBoat>of(
                                        (type, level) -> new StructureWoodChestBoat(type, level, wood),
                                        MobCategory.MISC
                                )
                                .sized(1.375F, 0.5625F)
                                .clientTrackingRange(10)
                                .build(id)
                ));
            }
        }
    }

    private EntityRegistry() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    public static DeferredHolder<EntityType<?>, EntityType<StructureWoodBoat>> boat(WoodMaterial wood) {
        return WOOD_BOATS.get(wood.id());
    }

    public static DeferredHolder<EntityType<?>, EntityType<StructureWoodChestBoat>> chestBoat(WoodMaterial wood) {
        return WOOD_CHEST_BOATS.get(wood.id());
    }
}
