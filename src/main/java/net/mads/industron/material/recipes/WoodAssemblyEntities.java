package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StructureWoodBoat;
import net.mads.industron.material.structure.StructureWoodChestBoat;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyEntityDefinition;
import net.mads.industron.registry.EntityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Species-aware Boat/Chest-Boat entity forms used by code-first wood Assembly recipes. */
final class WoodAssemblyEntities {
    private static final Map<String, AssemblyEntityDefinition> BOATS = new LinkedHashMap<>();
    private static final Map<String, AssemblyEntityDefinition> CHEST_BOATS = new LinkedHashMap<>();

    private WoodAssemblyEntities() {
    }

    static AssemblyEntityDefinition boat(WoodMaterial wood) {
        return BOATS.computeIfAbsent(wood.id(), ignored -> AssemblyEntityDefinition.entity(
                key(wood, "boat"),
                () -> displayStack(wood, MaterialPart.BOAT),
                entity -> matchesBoat(entity, wood),
                level -> createBoat(wood, level)
        ));
    }

    static AssemblyEntityDefinition chestBoat(WoodMaterial wood) {
        return CHEST_BOATS.computeIfAbsent(wood.id(), ignored -> AssemblyEntityDefinition.entity(
                key(wood, "chest_boat"),
                () -> displayStack(wood, MaterialPart.CHEST_BOAT),
                entity -> matchesChestBoat(entity, wood),
                level -> createChestBoat(wood, level)
        ));
    }

    private static ResourceLocation key(WoodMaterial wood, String form) {
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "assembly_entity/wood/" + wood.id() + "/" + form
        );
    }

    private static ItemStack displayStack(WoodMaterial wood, MaterialPart part) {
        ResourceLocation id = WoodRecipeIds.id(wood, part);
        if (id == null) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private static boolean matchesBoat(Entity entity, WoodMaterial wood) {
        if (wood.hasExistingPart(MaterialPart.BOAT)) {
            return entity instanceof Boat boat
                    && !(entity instanceof ChestBoat)
                    && boat.getVariant() == vanillaType(wood);
        }
        return entity instanceof StructureWoodBoat boat && boat.material().id().equals(wood.id());
    }

    private static boolean matchesChestBoat(Entity entity, WoodMaterial wood) {
        if (wood.hasExistingPart(MaterialPart.CHEST_BOAT)) {
            return entity instanceof ChestBoat boat && boat.getVariant() == vanillaType(wood);
        }
        return entity instanceof StructureWoodChestBoat boat && boat.material().id().equals(wood.id());
    }

    private static Boat createBoat(WoodMaterial wood, net.minecraft.server.level.ServerLevel level) {
        if (!wood.hasExistingPart(MaterialPart.BOAT)) {
            var holder = EntityRegistry.boat(wood);
            if (holder == null) throw new IllegalStateException("Missing generated Boat entity for " + wood.id());
            StructureWoodBoat boat = holder.get().create(level);
            if (boat == null) throw new IllegalStateException("Could not create generated Boat entity for " + wood.id());
            return boat;
        }

        Boat boat = EntityType.BOAT.create(level);
        if (boat == null) throw new IllegalStateException("Could not create vanilla Boat entity for " + wood.id());
        boat.setVariant(vanillaType(wood));
        return boat;
    }

    private static ChestBoat createChestBoat(WoodMaterial wood, net.minecraft.server.level.ServerLevel level) {
        if (!wood.hasExistingPart(MaterialPart.CHEST_BOAT)) {
            var holder = EntityRegistry.chestBoat(wood);
            if (holder == null) throw new IllegalStateException("Missing generated Chest Boat entity for " + wood.id());
            StructureWoodChestBoat boat = holder.get().create(level);
            if (boat == null) throw new IllegalStateException("Could not create generated Chest Boat entity for " + wood.id());
            return boat;
        }

        ChestBoat boat = EntityType.CHEST_BOAT.create(level);
        if (boat == null) throw new IllegalStateException("Could not create vanilla Chest Boat entity for " + wood.id());
        boat.setVariant(vanillaType(wood));
        return boat;
    }

    private static Boat.Type vanillaType(WoodMaterial wood) {
        try {
            return Boat.Type.valueOf(wood.id().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Wood " + wood.id() + " has a vanilla boat item but no matching Boat.Type", exception);
        }
    }
}
