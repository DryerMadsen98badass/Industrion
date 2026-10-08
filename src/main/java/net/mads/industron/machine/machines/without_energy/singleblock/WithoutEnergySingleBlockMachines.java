package net.mads.industron.machine.machines.without_energy.singleblock;

import net.mads.industron.machine.machines.without_energy.singleblock.machines.RiverWasher;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockDefinition;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureSetResolver;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.CERecipeTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Material-derived primitive singleblock catalogue. No acquisition recipes are generated here. */
public final class WithoutEnergySingleBlockMachines {
    public static final List<SingleBlockDefinition> ALL = buildAll();

    private static List<SingleBlockDefinition> buildAll() {
        List<SingleBlockDefinition> definitions = new ArrayList<>();
        definitions.add(RiverWasher.DEFINITION);
        WoodMaterials.ALL.forEach(wood -> definitions.add(dryingRack(wood)));
        StoneMaterials.ALL.forEach(stone -> {
            definitions.add(basin(stone));
            definitions.add(brickMold(stone));
            definitions.add(kiln(stone));
        });
        return List.copyOf(definitions);
    }

    private static SingleBlockDefinition dryingRack(WoodMaterial wood) {
        String texture = woodTexture(wood);
        return SingleBlockDefinition.machine()
                .machineDefinition(SingleBlockDefinition.Option.id(wood.id() + "_drying_rack"))
                .machineDefinition(SingleBlockDefinition.Option.displayName(wood.displayName() + " Drying Rack"))
                .machineDefinition(SingleBlockDefinition.Option.onlyTier(MachineTier.NONE))
                .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.RACK_DRYING))
                .machineDefinition(SingleBlockDefinition.Option.slots(4, 4, 0, 0))
                .machineDefinition(SingleBlockDefinition.Option.parallel(4))
                .machineDefinition(SingleBlockDefinition.Option.model("block/drying_rack"))
                .machineDefinition(allFaces(texture))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Four independent 2 x 2 drying positions."))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Sun advances drying; rain reverses it and can destroy the input."))
                .machineDefinition(SingleBlockDefinition.Option.mineableWith(Tool.PICKAXE))
                .machineDefinition(SingleBlockDefinition.Option.breakingTier(MachineTier.LV))
                .build();
    }

    private static SingleBlockDefinition basin(StoneMaterial stone) {
        return SingleBlockDefinition.machine()
                .machineDefinition(SingleBlockDefinition.Option.id(stone.id() + "_basin"))
                .machineDefinition(SingleBlockDefinition.Option.displayName(stone.displayName() + " Basin"))
                .machineDefinition(SingleBlockDefinition.Option.onlyTier(MachineTier.NONE))
                .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.BASIN_MIXING))
                .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
                .machineDefinition(SingleBlockDefinition.Option.slots(9, 4, 6, 4))
                .machineDefinition(SingleBlockDefinition.Option.model("block/basin"))
                .machineDefinition(SingleBlockDefinition.Option.stoneTextureSource(
                        stone,
                        MaterialPart.COBBLED_STONE,
                        MaterialPart.STONE
                ))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Insert or throw in up to nine item stacks, then use the required tool to mix or grind."))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Six separate fluid inputs hold one bucket and one fluid type each."))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Accepts any item or liquid from interactions, hoppers, and pipes; gases are rejected."))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Empty-hand right-click removes finished outputs first, then unused inputs."))
                .machineDefinition(SingleBlockDefinition.Option.mineableWith(Tool.PICKAXE))
                .machineDefinition(SingleBlockDefinition.Option.breakingTier(MachineTier.LV))
                .build();
    }

    private static SingleBlockDefinition brickMold(StoneMaterial stone) {
        String texture = StructureSetResolver.generatedTexture(stone, stone.model().baseSideTexture()).toString();
        return SingleBlockDefinition.machine()
                .machineDefinition(SingleBlockDefinition.Option.id(stone.id() + "_brick_mold"))
                .machineDefinition(SingleBlockDefinition.Option.displayName(stone.displayName() + " Brick Mold"))
                .machineDefinition(SingleBlockDefinition.Option.onlyTier(MachineTier.NONE))
                .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.BRICK_MOLDING))
                .machineDefinition(SingleBlockDefinition.Option.slots(1, 1, 0, 0))
                .machineDefinition(SingleBlockDefinition.Option.model("block/brick_mold"))
                .machineDefinition(allFaces(texture))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Insert clay, work it with the required tool, then collect with an empty hand."))
                .machineDefinition(SingleBlockDefinition.Option.mineableWith(Tool.PICKAXE))
                .machineDefinition(SingleBlockDefinition.Option.breakingTier(MachineTier.LV))
                .build();
    }

    private static SingleBlockDefinition kiln(StoneMaterial stone) {
        return SingleBlockDefinition.machine()
                .machineDefinition(SingleBlockDefinition.Option.id(stone.id() + "_kiln"))
                .machineDefinition(SingleBlockDefinition.Option.displayName(stone.displayName() + " Kiln"))
                .machineDefinition(SingleBlockDefinition.Option.onlyTier(MachineTier.NONE))
                .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.KILN_FIRING))
                .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.HEATING))
                .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.FUEL))
                .machineDefinition(SingleBlockDefinition.Option.slots(7, 0, 0, 0))
                .machineDefinition(SingleBlockDefinition.Option.parallel(6))
                .machineDefinition(SingleBlockDefinition.Option.stoneTextureSource(
                        stone,
                        MaterialPart.COBBLED_STONE,
                        MaterialPart.STONE
                ))
                .machineDefinition(SingleBlockDefinition.Option.frontOverlay("block/machines/overlay/kiln/overlay_front"))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Six one-item chambers run in parallel from one continuous fuel timer."))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes Kiln Firing and Heating recipes up to 1000 C (primitive ULV/LV only)."))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Outputs remain in their chamber; continued fuel can process the next valid Kiln recipe."))
                .machineDefinition(SingleBlockDefinition.Option.mineableWith(Tool.PICKAXE))
                .machineDefinition(SingleBlockDefinition.Option.breakingTier(MachineTier.LV))
                .build();
    }

    private static SingleBlockDefinition.Option allFaces(String texture) {
        return builder -> {
            SingleBlockDefinition.Option.frontTexture(texture).apply(builder);
            SingleBlockDefinition.Option.backTexture(texture).apply(builder);
            SingleBlockDefinition.Option.leftTexture(texture).apply(builder);
            SingleBlockDefinition.Option.rightTexture(texture).apply(builder);
            SingleBlockDefinition.Option.topTexture(texture).apply(builder);
            SingleBlockDefinition.Option.bottomTexture(texture).apply(builder);
        };
    }

    private static String woodTexture(WoodMaterial wood) {
        if (wood.hasExistingPart(MaterialPart.PLANKS)) {
            ResourceLocation block = wood.existingPart(MaterialPart.PLANKS);
            return ResourceLocation.fromNamespaceAndPath(block.getNamespace(), "block/" + block.getPath()).toString();
        }
        return StructureSetResolver.generatedTexture(wood, wood.model().id() + "_planks.png").toString();
    }

    private WithoutEnergySingleBlockMachines() {}
}
