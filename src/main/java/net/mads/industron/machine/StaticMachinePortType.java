package net.mads.industron.machine;

import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum StaticMachinePortType {
    REDSTONE_PORT("redstone_port", "Redstone Port", "block/machines/ino/redstone_port", "block/machines/ino/casing", ModelKind.OVERLAY, MachineTier.ULV, true, MachineTier.ULV.color(), MultiblockAbility.REDSTONE),
    ANDESITE_KINETIC_INPUT_BOX("andesite_kinetic_input_box", "Andesite Kinetic Input Box", "block/machines/ino/andesite_gearbox", "create:block/andesite_casing", ModelKind.KINETIC, MachineTier.ULV, false, 0, MultiblockAbility.KINETIC_INPUT),
    ANDESITE_KINETIC_OUTPUT_BOX("andesite_kinetic_output_box", "Andesite Kinetic Output Box", "block/machines/ino/andesite_gearbox", "create:block/andesite_casing", ModelKind.KINETIC, MachineTier.ULV, false, 0, MultiblockAbility.KINETIC_OUTPUT),
    ANDESITE_INPUT_HATCH("andesite_input_hatch", "Andesite Input Hatch", "block/machines/ino/input_hatch", "create:block/andesite_casing", ModelKind.OVERLAY, MachineTier.ULV, false, 0, MultiblockAbility.FLUID_INPUT),
    ANDESITE_OUTPUT_HATCH("andesite_output_hatch", "Andesite Output Hatch", "block/machines/ino/output_hatch", "create:block/andesite_casing", ModelKind.OVERLAY, MachineTier.ULV, false, 0, MultiblockAbility.FLUID_OUTPUT),
    ANDESITE_MUFFLER("andesite_muffler", "Andesite Muffler", "block/machines/ino/muffler", "create:block/andesite_casing", ModelKind.OVERLAY, MachineTier.ULV, false, 0, MultiblockAbility.MUFFLER),
    ANDESITE_INPUT_BUS("andesite_input_bus", "Andesite Input Bus", "block/machines/ino/input_bus", "create:block/andesite_casing", ModelKind.OVERLAY, MachineTier.ULV, false, 0, MultiblockAbility.ITEM_INPUT),
    ANDESITE_OUTPUT_BUS("andesite_output_bus", "Andesite Output Bus", "block/machines/ino/output_bus", "create:block/andesite_casing", ModelKind.OVERLAY, MachineTier.ULV, false, 0, MultiblockAbility.ITEM_OUTPUT),
    BRASS_KINETIC_INPUT_BOX("brass_kinetic_input_box", "Brass Kinetic Input Box", "block/machines/ino/brass_gearbox", "create:block/brass_casing", ModelKind.KINETIC, MachineTier.LV, false, 0, MultiblockAbility.KINETIC_INPUT),
    BRASS_KINETIC_OUTPUT_BOX("brass_kinetic_output_box", "Brass Kinetic Output Box", "block/machines/ino/brass_gearbox", "create:block/brass_casing", ModelKind.KINETIC, MachineTier.LV, false, 0, MultiblockAbility.KINETIC_OUTPUT),
    BRASS_INPUT_HATCH("brass_input_hatch", "Brass Input Hatch", "block/machines/ino/input_hatch", "create:block/brass_casing", ModelKind.OVERLAY, MachineTier.LV, false, 0, MultiblockAbility.FLUID_INPUT),
    BRASS_CB_HATCH("brass_cb_hatch", "Brass CB Hatch", "block/machines/ino/cb_hatch", "create:block/brass_casing", ModelKind.OVERLAY, MachineTier.NONE, false, 0, MultiblockAbility.CB_INPUT),
    BRASS_OUTPUT_HATCH("brass_output_hatch", "Brass Output Hatch", "block/machines/ino/output_hatch", "create:block/brass_casing", ModelKind.OVERLAY, MachineTier.LV, false, 0, MultiblockAbility.FLUID_OUTPUT),
    BRASS_MUFFLER("brass_muffler", "Brass Muffler", "block/machines/ino/muffler", "create:block/brass_casing", ModelKind.OVERLAY, MachineTier.LV, false, 0, MultiblockAbility.MUFFLER),
    BRASS_INPUT_BUS("brass_input_bus", "Brass Input Bus", "block/machines/ino/input_bus", "create:block/brass_casing", ModelKind.OVERLAY, MachineTier.LV, false, 0, MultiblockAbility.ITEM_INPUT),
    BRASS_OUTPUT_BUS("brass_output_bus", "Brass Output Bus", "block/machines/ino/output_bus", "create:block/brass_casing", ModelKind.OVERLAY, MachineTier.LV, false, 0, MultiblockAbility.ITEM_OUTPUT);

    public static final List<StaticMachinePortType> ALL = List.of(values());

    public enum ModelKind {
        OVERLAY,
        KINETIC
    }

    private final String id;
    private final String displayName;
    private final String frontTexture;
    private final String casingTexture;
    private final ModelKind modelKind;
    private final MachineTier tier;
    private final boolean tinted;
    private final int tintColor;
    private final Set<MultiblockAbility> abilities;

    StaticMachinePortType(String id, String displayName, String frontTexture, String casingTexture, ModelKind modelKind, MachineTier tier, boolean tinted, int tintColor, MultiblockAbility... abilities) {
        this.id = id;
        this.displayName = displayName;
        this.frontTexture = frontTexture;
        this.casingTexture = casingTexture;
        this.modelKind = modelKind;
        this.tier = tier;
        this.tinted = tinted;
        this.tintColor = tintColor;
        this.abilities = abilities.length == 0 ? EnumSet.noneOf(MultiblockAbility.class) : EnumSet.of(abilities[0], abilities);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String frontTexture() {
        return frontTexture;
    }

    public String casingTexture() {
        return casingTexture;
    }

    public ModelKind modelKind() {
        return modelKind;
    }

    public MachineTier tier() {
        return tier;
    }

    public boolean tinted() {
        return tinted;
    }

    public int tintColor() {
        return tintColor;
    }

    public Set<MultiblockAbility> abilities() {
        return abilities;
    }
}
