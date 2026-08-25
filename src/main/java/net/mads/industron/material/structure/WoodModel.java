package net.mads.industron.material.structure;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public enum WoodModel implements StructureModel {
    ACACIA("acacia", BlockSetType.ACACIA, WoodType.ACACIA),
    BAMBOO("bamboo", BlockSetType.BAMBOO, WoodType.BAMBOO),
    BIRCH("birch", BlockSetType.BIRCH, WoodType.BIRCH),
    CHERRY("cherry", BlockSetType.CHERRY, WoodType.CHERRY),
    CRIMSON("crimson", BlockSetType.CRIMSON, WoodType.CRIMSON),
    DARK_OAK("dark_oak", BlockSetType.DARK_OAK, WoodType.DARK_OAK),
    JUNGLE("jungle", BlockSetType.JUNGLE, WoodType.JUNGLE),
    MANGROVE("mangrove", BlockSetType.MANGROVE, WoodType.MANGROVE),
    OAK("oak", BlockSetType.OAK, WoodType.OAK),
    SPRUCE("spruce", BlockSetType.SPRUCE, WoodType.SPRUCE),
    WARPED("warped", BlockSetType.WARPED, WoodType.WARPED);

    private final String id;
    private final BlockSetType blockSetType;
    private final WoodType woodType;

    WoodModel(String id, BlockSetType blockSetType, WoodType woodType) {
        this.id = id;
        this.blockSetType = blockSetType;
        this.woodType = woodType;
    }

    @Override
    public String category() {
        return "wood";
    }

    @Override
    public String id() {
        return id;
    }

    public BlockSetType blockSetType() {
        return blockSetType;
    }

    public WoodType woodType() {
        return woodType;
    }
}
