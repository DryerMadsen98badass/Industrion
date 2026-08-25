package net.mads.industron.menu;

import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
import net.mads.industron.recipe.ChemicalBalanceRange;
import net.minecraft.world.inventory.ContainerData;

final class MultiblockControllerMenuData implements ContainerData {
    static final int DURABILITY_LOW = 0;
    static final int DURABILITY_HIGH = 1;
    static final int MAX_DURABILITY = 2;
    static final int CORROSION_PER_TICK = 3;
    static final int MACHINE_CB = 4;
    static final int SAFE_CB_MIN = 5;
    static final int SAFE_CB_MAX = 6;
    static final int FLAGS = 7;
    static final int COUNT = 8;

    static final int FLAG_DURABILITY = 1;
    static final int FLAG_SAFE_CB = 1 << 1;
    static final int FLAG_CB_HATCH = 1 << 2;

    private final MultiblockControllerBlockEntity controller;

    MultiblockControllerMenuData(MultiblockControllerBlockEntity controller) {
        this.controller = controller;
    }

    @Override
    public int get(int index) {
        long durability = controller.machineDurabilityHundredths();
        return switch (index) {
            case DURABILITY_LOW -> (int) durability;
            case DURABILITY_HIGH -> (int) (durability >>> 32);
            case MAX_DURABILITY -> controller.maxMachineDurability();
            case CORROSION_PER_TICK -> controller.corrosionDamageHundredthsPerTick();
            case MACHINE_CB -> controller.machineChemicalBalanceHundredths();
            case SAFE_CB_MIN -> controller.safeChemicalBalanceRange().map(ChemicalBalanceRange::minHundredths).orElse(0);
            case SAFE_CB_MAX -> controller.safeChemicalBalanceRange().map(ChemicalBalanceRange::maxHundredths).orElse(0);
            case FLAGS -> flags();
            default -> 0;
        };
    }

    private int flags() {
        int flags = 0;
        if (controller.hasMachineDurability()) {
            flags |= FLAG_DURABILITY;
        }
        if (controller.safeChemicalBalanceRange().isPresent()) {
            flags |= FLAG_SAFE_CB;
        }
        if (controller.hasCbHatch()) {
            flags |= FLAG_CB_HATCH;
        }
        return flags;
    }

    @Override
    public void set(int index, int value) {
    }

    @Override
    public int getCount() {
        return COUNT;
    }
}
