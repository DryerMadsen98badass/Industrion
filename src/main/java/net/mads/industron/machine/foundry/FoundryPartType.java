package net.mads.industron.machine.foundry;

import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The four special blocks that may replace Foundry bricks in legal structure positions. */
public enum FoundryPartType {
    CONTROLLER(
            "foundry_controller",
            "Foundry Controller",
            "block/machines/overlay/foundry/foundry_off",
            null
    ),
    ITEM_INPUT_BUS(
            "foundry_item_input_bus",
            "Foundry Item Input Bus",
            "block/machines/ino/input_bus",
            MultiblockAbility.ITEM_INPUT
    ),
    FLUID_INPUT_HATCH(
            "foundry_fluid_input_hatch",
            "Foundry Fluid Input Hatch",
            "block/machines/ino/input_hatch",
            MultiblockAbility.FLUID_INPUT
    ),
    FLUID_OUTPUT_HATCH(
            "foundry_fluid_output_hatch",
            "Foundry Fluid Output Hatch",
            "block/machines/ino/output_hatch",
            MultiblockAbility.FLUID_OUTPUT
    );

    public static final List<FoundryPartType> ALL = List.of(values());

    private final String id;
    private final String displayName;
    private final String overlayTexture;
    private final MultiblockAbility ability;

    FoundryPartType(
            String id,
            String displayName,
            String overlayTexture,
            @Nullable MultiblockAbility ability
    ) {
        this.id = id;
        this.displayName = displayName;
        this.overlayTexture = overlayTexture;
        this.ability = ability;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String overlayTexture() {
        return overlayTexture;
    }

    @Nullable
    public MultiblockAbility ability() {
        return ability;
    }

    public boolean isController() {
        return this == CONTROLLER;
    }

    public boolean allowedInBase() {
        return this == FLUID_OUTPUT_HATCH;
    }

    public boolean allowedInWall() {
        return this == ITEM_INPUT_BUS || this == FLUID_INPUT_HATCH;
    }
}
