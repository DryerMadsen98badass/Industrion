package net.mads.industron.machine;

import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum MachinePortType {
    KINETIC_INPUT_BOX("kinetic_input_box", "Kinetic Input Box", true, "kinetic_input_box", MultiblockAbility.KINETIC_INPUT),
    KINETIC_OUTPUT_BOX("kinetic_output_box", "Kinetic Output Box", true, "kinetic_output_box", MultiblockAbility.KINETIC_OUTPUT),
    INPUT_BUS("input_bus", "Input Bus", MultiblockAbility.ITEM_INPUT),
    OUTPUT_BUS("output_bus", "Output Bus", MultiblockAbility.ITEM_OUTPUT),
    INPUT_HATCH("input_hatch", "Input Hatch", MultiblockAbility.FLUID_INPUT),
    OUTPUT_HATCH("output_hatch", "Output Hatch", MultiblockAbility.FLUID_OUTPUT),
    MUFFLER_HATCH("muffler_hatch", "Muffler Hatch", "muffler", MultiblockAbility.MUFFLER),
    IO_INTERFACE("io_interface", "I/O Interface", "inobh", MultiblockAbility.IO_INTERFACE, MultiblockAbility.ITEM_INPUT, MultiblockAbility.ITEM_OUTPUT, MultiblockAbility.FLUID_INPUT, MultiblockAbility.FLUID_OUTPUT),
    ENERGY_INPUT_HATCH("energy_input_hatch", "Energy Input Hatch", MultiblockAbility.ENERGY_INPUT),
    ENERGY_OUTPUT_HATCH("energy_output_hatch", "Energy Output Hatch", MultiblockAbility.ENERGY_OUTPUT);

    public static final List<MachinePortType> ALL = List.of(values());

    private final String id;
    private final String displayName;
    private final boolean kinetic;
    private final String textureId;
    private final Set<MultiblockAbility> abilities;

    MachinePortType(String id, String displayName, MultiblockAbility... abilities) {
        this(id, displayName, false, id, abilities);
    }

    MachinePortType(String id, String displayName, boolean kinetic, String textureId, MultiblockAbility... abilities) {
        this.id = id;
        this.displayName = displayName;
        this.kinetic = kinetic;
        this.textureId = textureId;
        this.abilities = abilities.length == 0 ? EnumSet.noneOf(MultiblockAbility.class) : EnumSet.of(abilities[0], abilities);
    }

    MachinePortType(String id, String displayName, String textureId, MultiblockAbility... abilities) {
        this.id = id;
        this.displayName = displayName;
        this.kinetic = false;
        this.textureId = textureId;
        this.abilities = abilities.length == 0 ? EnumSet.noneOf(MultiblockAbility.class) : EnumSet.of(abilities[0], abilities);
    }

    public String id() {
        return id;
    }

    public String texturePath() {
        return "block/machines/ino/" + textureId;
    }

    public boolean isKinetic() {
        return kinetic;
    }

    public Set<MultiblockAbility> abilities() {
        return abilities;
    }

    public String registryName(MachineTier tier) {
        return tier.id() + "_" + id;
    }

    public String displayName(MachineTier tier) {
        return tier.displayName() + " " + displayName;
    }
}
