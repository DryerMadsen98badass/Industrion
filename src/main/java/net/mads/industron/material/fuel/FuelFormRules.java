package net.mads.industron.material.fuel;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantProcessIntermediate;

import java.util.Optional;

/**
 * Physical-form fuel multipliers.
 *
 * <p>These values never redefine material units: one solid unit is always one item, one liquid unit is 144 mB, and one gas unit is 576 mB. A form multiplier only describes how much burnable material that physical
 * form represents compared with its material's factor-1 reference form.</p>
 */
public final class FuelFormRules {
    private FuelFormRules() {
    }

    public static Optional<FuelFormFactor> wood(MaterialPart part) {
        return Optional.ofNullable(switch (part) {
            case LOG, STRIPPED_LOG, WOOD, STRIPPED_WOOD -> FuelFormFactor.of(4, 1);
            case PLANKS, MOSAIC -> FuelFormFactor.of(1, 1);
            case SLAB, MOSAIC_SLAB -> FuelFormFactor.of(1, 2);
            case STAIRS, MOSAIC_STAIRS -> FuelFormFactor.of(3, 4);
            case FENCE -> FuelFormFactor.of(3, 2);
            case FENCE_GATE -> FuelFormFactor.of(2, 1);
            case BUTTON -> FuelFormFactor.of(1, 8);
            case PRESSURE_PLATE, SIGN -> FuelFormFactor.of(1, 1);
            case DOOR, HANGING_SIGN -> FuelFormFactor.of(2, 1);
            case TRAPDOOR -> FuelFormFactor.of(3, 2);
            case LEAVES -> FuelFormFactor.of(1, 8);
            case SAPLING -> FuelFormFactor.of(1, 4);
            case STICK, TOOL_HANDLE, SAW_HANDLE -> FuelFormFactor.of(1, 8);
            case BARK -> FuelFormFactor.of(1, 4);
            case WOOD_PULP -> FuelFormFactor.of(1, 10);
            case SMALL_WOOD_PULP -> FuelFormFactor.of(1, 40);
            case TINY_WOOD_PULP -> FuelFormFactor.of(1, 90);
            case TOOL_HEAD_PICKAXE, TOOL_HEAD_AXE, TOOL_HEAD_SHOVEL, TOOL_HEAD_HOE,
                 TOOL_HEAD_HAMMER, TOOL_HEAD_MALLET -> FuelFormFactor.of(1, 2);
            case SIFTER_FRAME -> FuelFormFactor.of(2, 1);
            default -> null;
        });
    }

    /**
     * Plant-item fuel value relative to one normal harvested plant item. These are fuel/value
     * relations only; they never change the universal material-unit rule (1 solid item = 1 unit).
     */
    public static Optional<FuelFormFactor> plant(PlantPart part) {
        return Optional.ofNullable(switch (part) {
            case STORAGE_BLOCK -> null; // World-only quantity container, never a fuel item.
            case SEEDS, CARPET, SAPLING -> FuelFormFactor.of(1, 4);
            case LEAVES -> FuelFormFactor.of(1, 8);
            case BALE, COMPRESSED_BLOCK -> FuelFormFactor.of(9, 1);
            case PLANT, CROP, ROOT, STEM, FRUIT, FRUIT_BLOCK, BLOCK, FLOWER, VINE,
                 AQUATIC, FUNGUS, DRIED, FIBER, STRING -> FuelFormFactor.ONE;
        });
    }

    /** Route intermediates are one item, 144 mB liquid, or 576 mB gas per material unit. */
    public static FuelFormFactor plantIntermediate(PlantProcessIntermediate intermediate) {
        if (intermediate == null) throw new IllegalArgumentException("Plant intermediate cannot be null");
        return FuelFormFactor.of(1, 1);
    }

    /** Generic material forms that can sensibly act as a direct fuel input. */
    public static Optional<FuelFormFactor> material(MaterialPart part) {
        return Optional.ofNullable(switch (part) {
            case TINY_DUST -> FuelFormFactor.of(1, 9);
            case SMALL_DUST -> FuelFormFactor.of(1, 4);
            case DUST, IMPURE_DUST, PURIFIED_DUST,
                 RAW_ORE, CRUSHED_ORE, WASHED_CRUSHED_ORE, REFINED_ORE,
                 INGOT, PLATE -> FuelFormFactor.of(1, 1);
            case NUGGET -> FuelFormFactor.of(1, 9);
            case DOUBLE_INGOT, DOUBLE_PLATE -> FuelFormFactor.of(2, 1);
            case BLOCK, RAW_BLOCK -> FuelFormFactor.of(9, 1);
            case LIQUID, GAS -> FuelFormFactor.of(1, 1);
            default -> null;
        });
    }
}
